package com.example.studybuddy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class PomodoroViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val pomodoroCollection = db.collection("pomodoro_sessions")
    
    private var sessionsListener: ListenerRegistration? = null

    private val _pomodoroSessions = MutableLiveData<List<PomodoroSession>>()
    val pomodoroSessions: LiveData<List<PomodoroSession>> get() = _pomodoroSessions
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error
    
    private val _currentSession = MutableLiveData<PomodoroSession?>(null)
    val currentSession: LiveData<PomodoroSession?> get() = _currentSession
    
    // Timer states
    private val _timerState = MutableLiveData<TimerState>(TimerState.STOPPED)
    val timerState: LiveData<TimerState> get() = _timerState
    
    private val _timeRemaining = MutableLiveData<Long>(0L)
    val timeRemaining: LiveData<Long> get() = _timeRemaining
    
    private val _currentPhase = MutableLiveData<PomodoroPhase>(PomodoroPhase.WORK)
    val currentPhase: LiveData<PomodoroPhase> get() = _currentPhase
    
    private val _completedCycles = MutableLiveData<Int>(0)
    val completedCycles: LiveData<Int> get() = _completedCycles

    fun savePomodoroSession(session: PomodoroSession) {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val sessionWithUserId = session.copy(userId = currentUserId)
                
                pomodoroCollection.add(sessionWithUserId)
                    .addOnSuccessListener { documentReference ->
                        _isLoading.value = false
                        Log.d("PomodoroViewModel", "Session saved with ID: ${documentReference.id}")
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        Log.w("PomodoroViewModel", "Error saving session", e)
                        _error.value = "Failed to save session: ${e.message}"
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("PomodoroViewModel", "Error saving pomodoro session", e)
                _error.value = "Error saving session: ${e.message}"
            }
        }
    }

    fun fetchPomodoroSessions(userId: String? = null) {
        val currentUserId = userId ?: auth.currentUser?.uid
        
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        // Remove any existing listener
        sessionsListener?.remove()
        
        // Set up real-time listener for user's pomodoro sessions
        sessionsListener = pomodoroCollection
            .whereEqualTo("userId", currentUserId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                
                if (e != null) {
                    Log.w("PomodoroViewModel", "Listen failed.", e)
                    _error.value = "Failed to fetch sessions: ${e.message}"
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(PomodoroSession::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("PomodoroViewModel", "Error parsing session document", e)
                            null
                        }
                    }
                    
                    Log.d("PomodoroViewModel", "Fetched ${sessions.size} sessions for user $currentUserId")
                    _pomodoroSessions.value = sessions
                } else {
                    Log.d("PomodoroViewModel", "Current data: null")
                }
            }
    }
    
    fun getSessionsForPeriod(startTime: Long, endTime: Long): List<PomodoroSession> {
        return _pomodoroSessions.value?.filter { session ->
            session.startTime >= startTime && session.startTime <= endTime
        } ?: emptyList()
    }
    
    fun getTodaysSessions(): List<PomodoroSession> {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
        return getSessionsForPeriod(startOfDay, endOfDay)
    }
    
    fun getThisWeeksSessions(): List<PomodoroSession> {
        val now = System.currentTimeMillis()
        val week = 7 * 24 * 60 * 60 * 1000L
        val startOfWeek = now - week
        return getSessionsForPeriod(startOfWeek, now)
    }
    
    fun getTotalStudyTime(): Long {
        return _pomodoroSessions.value?.sumOf { it.duration } ?: 0L
    }
    
    fun getTotalStudyTimeToday(): Long {
        return getTodaysSessions().sumOf { it.duration }
    }
    
    fun getTotalStudyTimeThisWeek(): Long {
        return getThisWeeksSessions().sumOf { it.duration }
    }
    
    fun getAverageSessionDuration(): Long {
        val sessions = _pomodoroSessions.value ?: return 0L
        return if (sessions.isNotEmpty()) {
            sessions.sumOf { it.duration } / sessions.size
        } else 0L
    }
    
    // Timer Control Functions
    fun startTimer(workDuration: Long, breakDuration: Long) {
        _timerState.value = TimerState.RUNNING
        _timeRemaining.value = workDuration
        _currentPhase.value = PomodoroPhase.WORK
        
        // Create new session
val newSession = PomodoroSession(
            userId = auth.currentUser?.uid ?: "",
            startTime = System.currentTimeMillis(),
            endTime = 0L,
            duration = 0L,
            type = PomodoroPhase.WORK.name
        )
        _currentSession.value = newSession
    }
    
    fun pauseTimer() {
        _timerState.value = TimerState.PAUSED
    }
    
    fun resumeTimer() {
        _timerState.value = TimerState.RUNNING
    }
    
    fun stopTimer() {
        _timerState.value = TimerState.STOPPED
        _timeRemaining.value = 0L
        
        // Save current session if it exists
        _currentSession.value?.let { session ->
val completedSession = session.copy(
                endTime = System.currentTimeMillis(),
                duration = System.currentTimeMillis() - session.startTime
            )
            savePomodoroSession(completedSession)
        }
        
        _currentSession.value = null
    }
    
    fun completePhase() {
        when (_currentPhase.value) {
            PomodoroPhase.WORK -> {
                _currentPhase.value = PomodoroPhase.BREAK
                _completedCycles.value = (_completedCycles.value ?: 0) + 1
            }
            PomodoroPhase.BREAK -> {
                _currentPhase.value = PomodoroPhase.WORK
            }
            null -> {}
        }
    }
    
    fun updateTimeRemaining(time: Long) {
        _timeRemaining.value = time
    }
    
    fun resetTimer() {
        _timerState.value = TimerState.STOPPED
        _timeRemaining.value = 0L
        _currentPhase.value = PomodoroPhase.WORK
        _completedCycles.value = 0
        _currentSession.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        sessionsListener?.remove()
    }
}

// Enums for timer states
enum class TimerState {
    STOPPED, RUNNING, PAUSED
}

enum class PomodoroPhase {
    WORK, BREAK
}
