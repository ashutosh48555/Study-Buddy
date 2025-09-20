package com.example.studybuddy

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository private constructor(context: Context) {
    private val taskDao = TaskDatabase.getDatabase(context).taskDao()

    fun getTasksForUser(userId: String): LiveData<List<Task>> {
        return taskDao.getTasksForUser(userId).map { entities ->
            entities.map { it.toTask() }
        }
    }

    suspend fun addTask(task: Task) {
        withContext(Dispatchers.IO) {
            taskDao.insertTask(task.toEntity())
        }
    }

    suspend fun updateTask(task: Task) {
        withContext(Dispatchers.IO) {
            taskDao.updateTask(task.toEntity())
        }
    }

    suspend fun deleteTask(task: Task) {
        withContext(Dispatchers.IO) {
            taskDao.deleteTask(task.toEntity())
        }
    }

    suspend fun toggleTaskCompletion(task: Task) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        updateTask(updated)
    }

    companion object {
        @Volatile
        private var INSTANCE: TaskRepository? = null
        fun getInstance(context: Context): TaskRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TaskRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

// Mapping functions
fun TaskEntity.toTask(): Task = Task(
    id = id.toString(),
    title = title,
    category = category,
    userId = userId,
    isCompleted = isCompleted,
    completedDate = completedDate,
    createdAt = createdAt,
    priority = priority,
    dueDate = dueDate,
    ringtoneUri = ringtoneUri
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id.toIntOrNull() ?: 0,
    title = title,
    category = category,
    userId = userId,
    isCompleted = isCompleted,
    completedDate = completedDate,
    createdAt = createdAt,
    priority = priority,
    dueDate = dueDate,
    ringtoneUri = ringtoneUri
)
