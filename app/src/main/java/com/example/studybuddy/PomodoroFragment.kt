package com.example.studybuddy

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.studybuddy.databinding.FragmentPomodoroBinding
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.Date
import com.airbnb.lottie.value.LottieValueCallback
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.LottieProperty

class PomodoroFragment : Fragment() {
    private var _binding: FragmentPomodoroBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var timeLeft = 0L
    private var currentMode = TimerMode.WORK
    private var streakCount = 0
    private var sessionCount = 1
    private var totalSessions = 4
    private var soundEnabled = true
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences
    private var sessionStartTime: Long = 0L
    private var totalDurationMillis: Long = 0L
    private val localUserId = "local_user_id" // For local storage

    private val pomodoroViewModel: PomodoroViewModel by viewModels()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Default timer durations
    private var workDuration = 25
    private var shortBreakDuration = 5
    private var longBreakDuration = 15
    
    companion object {
        private const val PREFS_NAME = "PomodoroPrefs"
        private const val PREF_WORK_DURATION = "workDuration"
        private const val PREF_SHORT_BREAK = "shortBreakDuration"
        private const val PREF_LONG_BREAK = "longBreakDuration"
        private const val PREF_SESSIONS = "totalSessions"
        private const val PREF_SOUND = "soundEnabled"
        private const val COUNTDOWN_INTERVAL = 1000L // 1 second
    }
    
    enum class TimerMode {
        WORK, SHORT_BREAK, LONG_BREAK
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPomodoroBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load saved settings
        loadSettings()

        // Initialize timer with loaded settings
        timeLeft = workDuration * 60 * 1000L
        totalDurationMillis = timeLeft

        updateTimerText()
        updateModeText()
        updateSessionCountText()

        // Set info text
        binding.infoText.text = getString(R.string.pomodoro_info, workDuration, shortBreakDuration)

        // Initialize streak count text
        binding.streakCountText.text = getString(R.string.pomodoro_streak, streakCount)

        binding.startPauseButton.setOnClickListener {
            if (isRunning) {
                pauseTimer()
            } else {
                if (timeLeft == totalDurationMillis) {
                    // Starting a new timer
                    sessionStartTime = System.currentTimeMillis() // Record start time
                    startTimer()
                } else {
                    // Resuming a paused timer
                    resumeTimer()
                }
            }
        }

        binding.resetButton.setOnClickListener {
            resetTimer()
        }

        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        loadStreakCount()

        // Log all Lottie keypaths to help identify minute and second hand layers
        binding.timerAnimation.addLottieOnCompositionLoadedListener { composition ->
            val keypaths = binding.timerAnimation.resolveKeyPath(KeyPath("**"))
            for (kp in keypaths) {
                android.util.Log.d("LottieKeyPath", kp.keysToString())
            }
        }
    }
    
    private fun loadSettings() {
        workDuration = prefs.getInt(PREF_WORK_DURATION, 25)
        shortBreakDuration = prefs.getInt(PREF_SHORT_BREAK, 5)
        longBreakDuration = prefs.getInt(PREF_LONG_BREAK, 15)
        totalSessions = prefs.getInt(PREF_SESSIONS, 4)
        soundEnabled = prefs.getBoolean(PREF_SOUND, true)
    }
    
    private fun saveSettings() {
        prefs.edit().apply {
            putInt(PREF_WORK_DURATION, workDuration)
            putInt(PREF_SHORT_BREAK, shortBreakDuration)
            putInt(PREF_LONG_BREAK, longBreakDuration)
            putInt(PREF_SESSIONS, totalSessions)
            putBoolean(PREF_SOUND, soundEnabled)
            apply()
        }
    }
    
    private fun startTimer() {
        timer = object : CountDownTimer(timeLeft, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                updateTimerText()
                
                // Play tick sound every second if enabled
                if (soundEnabled && millisUntilFinished <= 5000) {
                    // You can add a tick sound here if you have the resource
                    // playSound(R.raw.timer_tick)
                }
            }
            
            override fun onFinish() {
                val sessionEndTime = System.currentTimeMillis()
                val duration = sessionEndTime - sessionStartTime

                // Use local user ID for local storage
                val userId = localUserId
                val sessionType = currentMode.name
                val pomodoroSession = PomodoroSession(
                    userId = userId,
                    type = sessionType,
                    startTime = sessionStartTime,
                    endTime = sessionEndTime,
                    duration = duration
                )
                pomodoroViewModel.savePomodoroSession(pomodoroSession)

                if (currentMode == TimerMode.WORK) {
                    // Work session completed
                    streakCount++
                    saveStreakCount()
                    binding.streakCountText.text = getString(R.string.pomodoro_streak, streakCount)
                    // Stop the animation when timer completes
                    binding.timerAnimation.pauseAnimation()

                    // Play completion sound
                    if (soundEnabled) {
                        // You can add a completion sound here if you have the resource
                        // playSound(R.raw.timer_complete)
                    }

                    // Increment session count
                    sessionCount++

                    // Check if it's time for a long break
                    if (sessionCount > totalSessions) {
                        sessionCount = 1
                        currentMode = TimerMode.LONG_BREAK
                        timeLeft = longBreakDuration * 60 * 1000L
                        totalDurationMillis = timeLeft
                    } else {
                        // Switch to short break mode
                        currentMode = TimerMode.SHORT_BREAK
                        timeLeft = shortBreakDuration * 60 * 1000L
                        totalDurationMillis = timeLeft
                    }
                } else {
                    // Break completed, switch back to work mode
                    currentMode = TimerMode.WORK
                    timeLeft = workDuration * 60 * 1000L
                    totalDurationMillis = timeLeft

                    // Play completion sound
                    if (soundEnabled) {
                        // You can add a completion sound here if you have the resource
                        // playSound(R.raw.timer_complete)
                    }
                }

                updateModeText()
                updateTimerText()
                updateSessionCountText()
                isRunning = false
                binding.startPauseButton.text = getString(R.string.pomodoro_start)
                // Stop the animation completely when timer finishes
                binding.timerAnimation.pauseAnimation()
            }
        }.start()
        
        isRunning = true
        binding.startPauseButton.text = getString(R.string.pomodoro_pause)
        // Start continuous looping animation
        binding.timerAnimation.progress = 0f
        binding.timerAnimation.speed = 1f // Normal speed for continuous looping
        binding.timerAnimation.repeatCount = -1 // Loop infinitely
        binding.timerAnimation.playAnimation()
    }
    
    private fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        binding.startPauseButton.text = getString(R.string.pomodoro_resume)
        binding.timerAnimation.pauseAnimation()
    }
    
    private fun resumeTimer() {
        timer = object : CountDownTimer(timeLeft, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                updateTimerText()
                
                // Play tick sound every second if enabled
                if (soundEnabled && millisUntilFinished <= 5000) {
                    // You can add a tick sound here if you have the resource
                    // playSound(R.raw.timer_tick)
                }
            }
            
            override fun onFinish() {
                val sessionEndTime = System.currentTimeMillis()
                val duration = sessionEndTime - sessionStartTime

                // Use local user ID for local storage
                val userId = localUserId
                val sessionType = currentMode.name
                val pomodoroSession = PomodoroSession(
                    userId = userId,
                    type = sessionType,
                    startTime = sessionStartTime,
                    endTime = sessionEndTime,
                    duration = duration
                )
                pomodoroViewModel.savePomodoroSession(pomodoroSession)

                if (currentMode == TimerMode.WORK) {
                    // Work session completed
                    streakCount++
                    saveStreakCount()
                    binding.streakCountText.text = getString(R.string.pomodoro_streak, streakCount)
                    // Stop the animation when timer completes
                    binding.timerAnimation.pauseAnimation()

                    // Play completion sound
                    if (soundEnabled) {
                        // You can add a completion sound here if you have the resource
                        // playSound(R.raw.timer_complete)
                    }

                    // Increment session count
                    sessionCount++

                    // Check if it's time for a long break
                    if (sessionCount > totalSessions) {
                        sessionCount = 1
                        currentMode = TimerMode.LONG_BREAK
                        timeLeft = longBreakDuration * 60 * 1000L
                        totalDurationMillis = timeLeft
                    } else {
                        // Switch to short break mode
                        currentMode = TimerMode.SHORT_BREAK
                        timeLeft = shortBreakDuration * 60 * 1000L
                        totalDurationMillis = timeLeft
                    }
                } else {
                    // Break completed, switch back to work mode
                    currentMode = TimerMode.WORK
                    timeLeft = workDuration * 60 * 1000L
                    totalDurationMillis = timeLeft

                    // Play completion sound
                    if (soundEnabled) {
                        // You can add a completion sound here if you have the resource
                        // playSound(R.raw.timer_complete)
                    }
                }

                updateModeText()
                updateTimerText()
                updateSessionCountText()
                isRunning = false
                binding.startPauseButton.text = getString(R.string.pomodoro_start)
                // Stop the animation completely when timer finishes
                binding.timerAnimation.pauseAnimation()
            }
        }.start()
        
        isRunning = true
        binding.startPauseButton.text = getString(R.string.pomodoro_pause)
        // Resume animation with continuous looping
        binding.timerAnimation.speed = 1f
        binding.timerAnimation.repeatCount = -1 // Loop infinitely
        binding.timerAnimation.resumeAnimation()
    }
    
    private fun resetTimer() {
        timer?.cancel()
        currentMode = TimerMode.WORK
        timeLeft = workDuration * 60 * 1000L
        totalDurationMillis = timeLeft
        sessionCount = 1
        isRunning = false
        updateTimerText()
        updateModeText()
        updateSessionCountText()
        binding.startPauseButton.text = getString(R.string.pomodoro_start)
        // Stop and reset animation
        binding.timerAnimation.pauseAnimation()
        binding.timerAnimation.progress = 0f
    }
    
    private fun updateTimerText() {
        val minutes = (timeLeft / 1000) / 60
        val seconds = (timeLeft / 1000) % 60
        binding.timerText.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
    
    private fun updateModeText() {
        binding.modeText.text = when (currentMode) {
            TimerMode.WORK -> getString(R.string.pomodoro_work_mode)
            TimerMode.SHORT_BREAK -> getString(R.string.pomodoro_break_mode)
            TimerMode.LONG_BREAK -> getString(R.string.pomodoro_long_break_mode)
        }
    }
    
    private fun updateSessionCountText() {
        binding.sessionCountText.text = getString(R.string.pomodoro_sessions_count, sessionCount, totalSessions)
    }
    
    private fun playSound(soundResId: Int) {
        try {
            // Release any previous MediaPlayer
            mediaPlayer?.release()
            
            // Create and start a new MediaPlayer
            mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer?.start()
            
            // Set up a listener to release resources when playback completes
            mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadStreakCount() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        streakCount = document.getLong("pomodoroStreak")?.toInt() ?: 0
                        _binding?.let { binding ->
                            binding.streakCountText.text = getString(R.string.pomodoro_streak, streakCount)
                        }
                    }
                }
        }
    }
    
    private fun saveStreakCount() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .update("pomodoroStreak", streakCount)
        }
    }
    
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_timer_settings, null)
        
        // Set current values
        val workDurationInput = dialogView.findViewById<EditText>(R.id.workDurationInput)
        val shortBreakInput = dialogView.findViewById<EditText>(R.id.shortBreakInput)
        val longBreakInput = dialogView.findViewById<EditText>(R.id.longBreakInput)
        val sessionsInput = dialogView.findViewById<EditText>(R.id.sessionsInput)
        val soundSwitch = dialogView.findViewById<SwitchCompat>(R.id.soundSwitch)
        
        workDurationInput.setText(workDuration.toString())
        shortBreakInput.setText(shortBreakDuration.toString())
        longBreakInput.setText(longBreakDuration.toString())
        sessionsInput.setText(totalSessions.toString())
        soundSwitch.isChecked = soundEnabled
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.timer_settings)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                try {
                    val newWorkDuration = workDurationInput.text.toString().toInt()
                    val newShortBreak = shortBreakInput.text.toString().toInt()
                    val newLongBreak = longBreakInput.text.toString().toInt()
                    val newSessions = sessionsInput.text.toString().toInt()
                    
                    if (newWorkDuration > 0 && newShortBreak > 0 && newLongBreak > 0 && newSessions > 0) {
                        workDuration = newWorkDuration
                        shortBreakDuration = newShortBreak
                        longBreakDuration = newLongBreak
                        totalSessions = newSessions
                        soundEnabled = soundSwitch.isChecked
                        
                        saveSettings()
                        resetTimer()
                        binding.infoText.text = getString(R.string.pomodoro_info, workDuration, shortBreakDuration)
                    }
                } catch (e: NumberFormatException) {
                    // Handle invalid input
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        mediaPlayer?.release()
        _binding = null
    }
}
