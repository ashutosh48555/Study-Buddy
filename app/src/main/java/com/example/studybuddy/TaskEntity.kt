package com.example.studybuddy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val category: String = "",
    val userId: String = "",
    val isCompleted: Boolean = false,
    val completedDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: String = "Medium",
    val dueDate: Long = 0L,
    val ringtoneUri: String? = null
) 