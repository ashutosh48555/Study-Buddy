package com.example.studybuddy

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.studybuddy.databinding.DialogTaskBinding
import com.example.studybuddy.databinding.FragmentToDoBinding
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.TextView
import com.example.studybuddy.DialogUtils.showConfirmationDialog
import com.example.studybuddy.DialogUtils.showSuccessDialog
import com.example.studybuddy.DialogUtils.showInfoDialog
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.util.TimeZone

class ToDoFragment : Fragment(), TaskAdapter.TaskActionListener {
    private var _binding: FragmentToDoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private val allCategoriesOption = "All Categories"
    private lateinit var noTasksPlaceholder: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToDoBinding.inflate(inflater, container, false)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
        val repository = TaskRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(repository, userId) as T
            }
        }).get(TaskViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView with adapter
        taskAdapter = TaskAdapter(emptyList())
        taskAdapter.setTaskActionListener(this)
        binding.todoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.todoRecyclerView.adapter = taskAdapter

        // Setup Category Spinner
        setupCategorySpinner()

        // Setup Floating Action Button
        val addTaskFab = view.findViewById<View>(R.id.addTaskFab)
        addTaskFab.setOnClickListener {
            showTaskDialog()
        }

        // Observe tasks LiveData - this will auto-refresh when tasks change
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            val today = System.currentTimeMillis()
            val tz = TimeZone.getDefault()
            val offset = tz.getOffset(today)
            val startOfDay = (today + offset) / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000) - offset
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
            val todayTasks = tasks.filter { it.dueDate in startOfDay..endOfDay }
            taskAdapter.updateTasks(todayTasks)
            if (todayTasks.isEmpty()) {
                noTasksPlaceholder.visibility = View.VISIBLE
                binding.todoRecyclerView.visibility = View.GONE
            } else {
                noTasksPlaceholder.visibility = View.GONE
                binding.todoRecyclerView.visibility = View.VISIBLE
            }
        }

        // Observe categories LiveData
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            updateCategorySpinner(categories)
        }

        // Observe error state for user feedback
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showInfoDialog("Error", it)
                viewModel.clearError()
            }
        }

        noTasksPlaceholder = view.findViewById(R.id.noTasksPlaceholder)
    }

    private fun setupCategorySpinner() {
        // Initialize with just "All Categories"
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf(allCategoriesOption))
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        // Set listener for category selection
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                if (selectedCategory == allCategoriesOption) {
                    viewModel.setFilter(null) // No filter
                } else {
                    viewModel.setFilter(selectedCategory)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.setFilter(null) // No filter
            }
        }
    }

    private fun updateCategorySpinner(categories: Set<String>) {
        val spinnerItems = mutableListOf(allCategoriesOption)
        spinnerItems.addAll(categories)

        categoryAdapter.clear()
        categoryAdapter.addAll(spinnerItems)
        categoryAdapter.notifyDataSetChanged()
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
            startActivityForResult(intent, 101)
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
                    showInfoDialog("Error", "Please fill in all fields.")
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
        // Handle ringtone picker result
        requireActivity().supportFragmentManager.setFragmentResultListener("RINGTONE_PICKED", this) { _, bundle ->
            selectedRingtoneUri = bundle.getString("ringtoneUri")
            updateDateTimeButtons()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && data != null) {
            val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val bundle = Bundle()
            bundle.putString("ringtoneUri", uri?.toString())
            requireActivity().supportFragmentManager.setFragmentResult("RINGTONE_PICKED", bundle)
        }
    }

    private fun scheduleTaskAlarm(task: Task) {
        if (task.dueDate <= 0L) return
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Prompt user to allow exact alarms
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
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            task.dueDate,
            pendingIntent
        )
    }

    // TaskActionListener implementation
    override fun onTaskCompleted(task: Task) {
        lifecycleScope.launch {
            viewModel.toggleTaskCompletion(task)
        }
    }

    override fun onTaskEdit(task: Task) {
        showTaskDialog(task)
    }

    override fun onTaskDelete(task: Task) {
        showConfirmationDialog("Delete Task", "Are you sure you want to delete this task?") {
            viewModel.deleteTask(task)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}