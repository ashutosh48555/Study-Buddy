package com.example.studybuddy

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studybuddy.databinding.ItemTaskBinding

class TaskAdapter(private var tasks: List<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    
    interface TaskActionListener {
        fun onTaskCompleted(task: Task)
        fun onTaskEdit(task: Task)
        fun onTaskDelete(task: Task)
    }
    
    private var listener: TaskActionListener? = null
    
    fun setTaskActionListener(listener: TaskActionListener) {
        this.listener = listener
    }
    
    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        
        // Set task title and apply strikethrough if completed
        holder.binding.taskTitle.text = task.title
        if (task.isCompleted) {
            holder.binding.taskTitle.paintFlags = holder.binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.binding.taskTitle.paintFlags = holder.binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        
        // Set category
        holder.binding.taskCategory.text = task.category
        
        // Set checkbox state
        holder.binding.taskCheckbox.isChecked = task.isCompleted
        
        // Set click listeners
        holder.binding.taskCheckbox.setOnClickListener {
            listener?.onTaskCompleted(task)
        }
        
        holder.binding.editButton.setOnClickListener {
            listener?.onTaskEdit(task)
        }
        
        holder.binding.deleteButton.setOnClickListener {
            listener?.onTaskDelete(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}