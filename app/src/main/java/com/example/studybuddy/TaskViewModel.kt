package com.example.studybuddy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Use the same Task data class as before

data class Task(
    val id: String = "", // Local DB id as string
    val title: String = "",
    val category: String = "",
    val userId: String = "",
    val isCompleted: Boolean = false,
    val completedDate: Long = 0L, // Timestamp in milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val priority: String = "Medium", // Low, Medium, High
    val dueDate: Long = 0L, // Optional due date
    val ringtoneUri: String? = null // Custom alarm sound
)

class TaskViewModel(private val repository: TaskRepository, private val userId: String) : ViewModel() {
    val tasks: LiveData<List<Task>> = repository.getTasksForUser(userId)

    private val _categories = MutableLiveData<Set<String>>()
    val categories: LiveData<Set<String>> get() = _categories

    private val _currentFilter = MutableLiveData<String?>(null)
    val currentFilter: LiveData<String?> get() = _currentFilter

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error

    init {
        // Observe tasks and update categories
        tasks.observeForever { taskList ->
            updateCategories(taskList)
        }
    }

    private fun updateCategories(taskList: List<Task>) {
        val uniqueCategories = taskList.map { it.category }.filter { it.isNotEmpty() }.toSet()
        _categories.value = uniqueCategories
    }

    fun setFilter(category: String?) {
        _currentFilter.value = category
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.addTask(task.copy(userId = userId))
            } catch (e: Exception) {
                _error.value = "Failed to add task: ${e.message}"
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
            } catch (e: Exception) {
                _error.value = "Failed to update task: ${e.message}"
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
            } catch (e: Exception) {
                _error.value = "Failed to delete task: ${e.message}"
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                repository.toggleTaskCompletion(task)
            } catch (e: Exception) {
                _error.value = "Failed to toggle completion: ${e.message}"
            }
        }
    }

    fun getTasksByCategory(category: String): List<Task> {
        return tasks.value?.filter { it.category == category } ?: emptyList()
    }

    fun getCompletedTasks(): List<Task> {
        return tasks.value?.filter { it.isCompleted } ?: emptyList()
    }

    fun getPendingTasks(): List<Task> {
        return tasks.value?.filter { !it.isCompleted } ?: emptyList()
    }

    fun getTasksCompletedToday(): List<Task> {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        return tasks.value?.filter { it.isCompleted && it.completedDate >= startOfDay } ?: emptyList()
    }

    fun clearError() {
        _error.value = null
    }
}
