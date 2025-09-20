package com.example.studybuddy

data class PomodoroSession(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // "WORK", "SHORT_BREAK", "LONG_BREAK"
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val duration: Long = 0L // Duration in milliseconds
)