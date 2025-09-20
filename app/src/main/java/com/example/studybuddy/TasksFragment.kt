package com.example.studybuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.example.studybuddy.databinding.DialogTaskBinding
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.util.Calendar
import android.content.Intent
import android.content.Context

class TasksFragment : Fragment(), TaskAdapter.TaskActionListener {
    private lateinit var viewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var noTasksPlaceholder: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
        val repository = TaskRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(repository, userId) as T
            }
        }).get(TaskViewModel::class.java)
        taskAdapter = TaskAdapter(emptyList())
        taskAdapter.setTaskActionListener(this)
        val recyclerView = view.findViewById<RecyclerView>(R.id.tasksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = taskAdapter
        noTasksPlaceholder = view.findViewById(R.id.noTasksPlaceholder)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.updateTasks(tasks)
            if (tasks.isEmpty()) {
                noTasksPlaceholder.visibility = View.VISIBLE
            } else {
                noTasksPlaceholder.visibility = View.GONE
            }
        }
    }

    override fun onTaskCompleted(task: Task) {
        viewModel.toggleTaskCompletion(task)
    }

    override fun onTaskEdit(task: Task) {
        showTaskDialog(task)
        Toast.makeText(requireContext(), "Task updated.", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskDelete(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask(task)
                Toast.makeText(requireContext(), "Task deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTaskDialog(task: Task? = null) {
        val dialogBinding = DialogTaskBinding.inflate(layoutInflater)
        val isEdit = task != null
        dialogBinding.dialogTitle.text = if (isEdit) "Edit Task" else "Add New Task"
        if (isEdit) {
            dialogBinding.taskTitleInput.setText(task?.title)
            dialogBinding.taskCategoryInput.setText(task?.category)
        }
        // Date and time selection variables
        var selectedDueDate: Long = task?.dueDate ?: 0L
        var selectedRingtoneUri: String? = task?.ringtoneUri
        val calendar = Calendar.getInstance()
        if (selectedDueDate > 0L) calendar.timeInMillis = selectedDueDate
        // Show picked date/time/ringtone on button
        fun updateDateTimeButtons() {
            val dateText = android.text.format.DateFormat.format("yyyy-MM-dd", calendar)
            val timeText = android.text.format.DateFormat.format("HH:mm", calendar)
            dialogBinding.btnPickDate.text = "Pick Date: $dateText"
            dialogBinding.btnPickTime.text = "Pick Time: $timeText"
            val ringtoneTitle = selectedRingtoneUri?.let {
                val uri = Uri.parse(it)
                val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
                ringtone?.getTitle(requireContext()) ?: "Default"
            } ?: "Default"
            dialogBinding.btnPickRingtone.text = "Pick Ringtone: $ringtoneTitle"
        }
        updateDateTimeButtons()
        dialogBinding.btnPickDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeButtons()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        dialogBinding.btnPickTime.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                updateDateTimeButtons()
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }
        dialogBinding.btnPickRingtone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
            val currentUri = selectedRingtoneUri?.let { Uri.parse(it) }
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            startActivityForResult(intent, 102)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add") { _, _ ->
                val title = dialogBinding.taskTitleInput.text.toString().trim()
                val category = dialogBinding.taskCategoryInput.text.toString().trim()
                val dueDate = calendar.timeInMillis
                val ringtoneUri = selectedRingtoneUri
                if (title.isNotEmpty() && category.isNotEmpty()) {
                    if (isEdit) {
                        val updatedTask = task!!.copy(title = title, category = category, dueDate = dueDate, ringtoneUri = ringtoneUri)
                        viewModel.updateTask(updatedTask)
                        scheduleTaskAlarm(updatedTask)
                    } else {
                        val newTask = Task(id = "", title = title, category = category, userId = viewModel.tasks.value?.firstOrNull()?.userId ?: "local_user", isCompleted = false, dueDate = dueDate, ringtoneUri = ringtoneUri)
                        viewModel.addTask(newTask)
                        scheduleTaskAlarm(newTask)
                    }
                } else {
                    Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
        // Handle ringtone picker result
        requireActivity().supportFragmentManager.setFragmentResultListener("RINGTONE_PICKED_TASKS", this) { _, bundle ->
            selectedRingtoneUri = bundle.getString("ringtoneUri")
            updateDateTimeButtons()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102 && data != null) {
            val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val bundle = android.os.Bundle()
            bundle.putString("ringtoneUri", uri?.toString())
            requireActivity().supportFragmentManager.setFragmentResult("RINGTONE_PICKED_TASKS", bundle)
        }
    }

    private fun scheduleTaskAlarm(task: Task) {
        if (task.dueDate <= 0L) return
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                Toast.makeText(requireContext(), "Please allow exact alarms for reminders to work.", Toast.LENGTH_LONG).show()
                requireContext().startActivity(intent)
                return
            }
        }
        val intent = Intent(requireContext(), TaskAlarmReceiver::class.java).apply {
            putExtra("taskTitle", task.title)
            putExtra("taskId", task.id)
            putExtra("ringtoneUri", task.ringtoneUri)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            requireContext(),
            task.id.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            task.dueDate,
            pendingIntent
        )
    }
}
