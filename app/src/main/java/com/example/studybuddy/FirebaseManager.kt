package com.example.studybuddy

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    // Collections
    private const val USERS_COLLECTION = "users"
    private const val TASKS_COLLECTION = "tasks"
    private const val POMODORO_SESSIONS_COLLECTION = "pomodoro_sessions"
    private const val STUDY_GOALS_COLLECTION = "study_goals"
    
    // Authentication
    fun getCurrentUser() = auth.currentUser
    
    fun isUserAuthenticated() = auth.currentUser != null
    
    suspend fun signIn(email: String, password: String) = auth.signInWithEmailAndPassword(email, password).await()
    
    suspend fun signUp(email: String, password: String) = auth.createUserWithEmailAndPassword(email, password).await()
    
    fun signOut() = auth.signOut()
    
    // User Profile Operations
    suspend fun createUserProfile(userProfile: UserProfile) {
        firestore.collection(USERS_COLLECTION)
            .document(userProfile.uid)
            .set(userProfile)
            .await()
    }
    
    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject(UserProfile::class.java)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            null
        }
    }
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .update(updates)
            .await()
    }
    
    // Task Operations
    suspend fun addTask(task: Task): String {
        val docRef = firestore.collection(TASKS_COLLECTION).add(task).await()
        return docRef.id
    }
    
    suspend fun updateTask(taskId: String, task: Task) {
        firestore.collection(TASKS_COLLECTION)
            .document(taskId)
            .set(task)
            .await()
    }
    
    suspend fun deleteTask(taskId: String) {
        firestore.collection(TASKS_COLLECTION)
            .document(taskId)
            .delete()
            .await()
    }
    
    suspend fun getUserTasks(userId: String): List<Task> {
        return try {
            val snapshot = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Task::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user tasks", e)
            emptyList()
        }
    }
    
    // Pomodoro Session Operations
    suspend fun savePomodoroSession(session: PomodoroSession) {
        firestore.collection(POMODORO_SESSIONS_COLLECTION)
            .add(session)
            .await()
    }
    
    suspend fun getUserPomodoroSessions(userId: String): List<PomodoroSession> {
        return try {
            val snapshot = firestore.collection(POMODORO_SESSIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(PomodoroSession::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pomodoro sessions", e)
            emptyList()
        }
    }
    
    suspend fun getPomodoroSessionsForPeriod(userId: String, startTime: Long, endTime: Long): List<PomodoroSession> {
        return try {
            val snapshot = firestore.collection(POMODORO_SESSIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("startTime", startTime)
                .whereLessThanOrEqualTo("startTime", endTime)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(PomodoroSession::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pomodoro sessions for period", e)
            emptyList()
        }
    }
    
    // Study Goals Operations
    suspend fun saveStudyGoal(goal: StudyGoal) {
        firestore.collection(STUDY_GOALS_COLLECTION)
            .add(goal)
            .await()
    }
    
    suspend fun getUserStudyGoals(userId: String): List<StudyGoal> {
        return try {
            val snapshot = firestore.collection(STUDY_GOALS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(StudyGoal::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting study goals", e)
            emptyList()
        }
    }
    
    suspend fun updateStudyGoal(goalId: String, updates: Map<String, Any>) {
        firestore.collection(STUDY_GOALS_COLLECTION)
            .document(goalId)
            .update(updates)
            .await()
    }
    
    // Analytics Operations
    suspend fun getTaskCompletionStats(userId: String, startTime: Long, endTime: Long): TaskCompletionStats {
        return try {
            val snapshot = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", true)
                .whereGreaterThanOrEqualTo("completedDate", startTime)
                .whereLessThanOrEqualTo("completedDate", endTime)
                .get()
                .await()
            
            val completedTasks = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Task::class.java)?.copy(id = doc.id)
            }
            
            val totalTasks = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .size()
            
            TaskCompletionStats(
                totalTasks = totalTasks,
                completedTasks = completedTasks.size,
                completionRate = if (totalTasks > 0) (completedTasks.size.toFloat() / totalTasks.toFloat()) * 100 else 0f,
                tasksByCategory = completedTasks.groupBy { it.category }.mapValues { it.value.size }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting task completion stats", e)
            TaskCompletionStats(0, 0, 0f, emptyMap())
        }
    }
    
    suspend fun getStudyTimeStats(userId: String, startTime: Long, endTime: Long): StudyTimeStats {
        return try {
            val sessions = getPomodoroSessionsForPeriod(userId, startTime, endTime)
            val totalStudyTime = sessions.sumOf { it.duration }
            val averageSessionTime = if (sessions.isNotEmpty()) totalStudyTime / sessions.size else 0L
            
            StudyTimeStats(
                totalStudyTime = totalStudyTime,
                totalSessions = sessions.size,
                averageSessionTime = averageSessionTime,
                dailyStudyTime = sessions.groupBy { 
                    // Group by day
                    val dayInMillis = 24 * 60 * 60 * 1000
                    it.startTime / dayInMillis
                }.mapValues { entry ->
                    entry.value.sumOf { it.duration }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting study time stats", e)
            StudyTimeStats(0L, 0, 0L, emptyMap())
        }
    }
    
    // Storage Operations
    fun getStorageReference() = storage.reference
    
    suspend fun uploadFile(path: String, data: ByteArray): String {
        val ref = storage.reference.child(path)
        ref.putBytes(data).await()
        return ref.downloadUrl.await().toString()
    }
    
    suspend fun deleteFile(downloadUrl: String) {
        val ref = storage.getReferenceFromUrl(downloadUrl)
        ref.delete().await()
    }
}

// Data classes for analytics
data class TaskCompletionStats(
    val totalTasks: Int,
    val completedTasks: Int,
    val completionRate: Float,
    val tasksByCategory: Map<String, Int>
)

data class StudyTimeStats(
    val totalStudyTime: Long,
    val totalSessions: Int,
    val averageSessionTime: Long,
    val dailyStudyTime: Map<Long, Long>
)

data class StudyGoal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val targetValue: Int = 0,
    val currentValue: Int = 0,
    val unit: String = "", // "tasks", "minutes", "hours", etc.
    val type: String = "", // "daily", "weekly", "monthly"
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long = 0L,
    val isCompleted: Boolean = false
)
