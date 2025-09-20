package com.example.studybuddy

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ScrollView
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
// import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AppCompatDelegate
import android.widget.LinearLayout
import android.widget.FrameLayout

class AnalyticsFragment : Fragment() {

    private lateinit var taskViewModel: TaskViewModel
    // private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val localUserId = "local_user_id" // Placeholder for local storage

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var pomodoroStatsTextView: TextView
    private lateinit var barChartPlaceholder: LinearLayout
    private lateinit var pieChartPlaceholder: LinearLayout
    private lateinit var lineChartPlaceholder: LinearLayout
    private lateinit var analyticsHeadline: TextView
    private lateinit var totalStudyTimeTextView: TextView
    private lateinit var completedTasksBigTextView: TextView
    private lateinit var pomodoroSessionsTextView: TextView

    private val pomodoroViewModel: PomodoroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize TaskViewModel with factory
        val factory = TaskViewModelFactory(TaskRepository.getInstance(requireContext()), localUserId)
        taskViewModel = ViewModelProvider(this, factory).get(TaskViewModel::class.java)

        barChart = view.findViewById(R.id.barChart)
        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)
        pomodoroStatsTextView = view.findViewById(R.id.pomodoroStatsTextView)
        barChartPlaceholder = view.findViewById(R.id.barChartPlaceholder)
        pieChartPlaceholder = view.findViewById(R.id.pieChartPlaceholder)
        lineChartPlaceholder = view.findViewById(R.id.lineChartPlaceholder)
        analyticsHeadline = view.findViewById(R.id.analyticsHeadline)
        totalStudyTimeTextView = view.findViewById(R.id.totalStudyTimeTextView)
        completedTasksBigTextView = view.findViewById(R.id.completedTasksBigTextView)
        pomodoroSessionsTextView = view.findViewById(R.id.pomodoroSessionsTextView)

        // Set text color for visibility (fallback)
        pomodoroStatsTextView.setTextColor(getThemeTextColor())

        // Tasks are already loaded via the repository in the ViewModel constructor
        taskViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            val completedTasks = tasks.filter { it.isCompleted }
            completedTasksBigTextView.text = completedTasks.size.toString()
            updateBarChart(completedTasks)
            updatePieChart(completedTasks)
            updateLineChart(completedTasks)
        }

        Log.d("AnalyticsFragment", "Fetching Pomodoro sessions for userId: $localUserId")
        pomodoroViewModel.fetchPomodoroSessions(localUserId)
        pomodoroViewModel.pomodoroSessions.observe(viewLifecycleOwner) { sessions ->
            // Total study time (sum of WORK session durations in minutes)
            val totalWorkMillis = sessions.filter { it.type == "WORK" }.sumOf { it.duration }
            val totalWorkMinutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(totalWorkMillis)
            totalStudyTimeTextView.text = "$totalWorkMinutes min"
            // Pomodoro sessions completed (WORK sessions count)
            val pomodoroCount = sessions.count { it.type == "WORK" }
            pomodoroSessionsTextView.text = pomodoroCount.toString()
            updatePomodoroStats(sessions)
        }
    }

    private fun updatePomodoroStats(sessions: List<PomodoroSession>) {
        Log.d("AnalyticsFragment", "updatePomodoroStats called with ${sessions.size} sessions")
        val totalWorkTimeMillis = sessions.filter { it.type == "WORK" }.sumOf { it.duration }
        val totalBreakTimeMillis = sessions.filter { it.type == "SHORT_BREAK" || it.type == "LONG_BREAK" }.sumOf { it.duration }
        val totalSessionsCount = sessions.filter { it.type == "WORK" }.size

        if (sessions.isEmpty()) {
            pomodoroStatsTextView.text = "No Pomodoro sessions found."
            return
        }

        val totalWorkTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(totalWorkTimeMillis)
        val totalBreakTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(totalBreakTimeMillis)

        val statsText = "Pomodoro Stats:\n" +
                "Total Work Sessions: $totalSessionsCount\n" +
                "Total Work Time: ${totalWorkTimeMinutes} minutes\n" +
                "Total Break Time: ${totalBreakTimeMinutes} minutes"
        pomodoroStatsTextView.text = statsText
    }

    private fun updateBarChart(completedTasks: List<Task>) {
        val calendar = Calendar.getInstance()
        val dailyCompletionCounts = mutableMapOf<String, Int>()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())

        // Initialize counts for the last 7 days
        for (i in 0 until 7) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            dailyCompletionCounts[dateFormat.format(calendar.time)] = 0
        }

        // Populate counts based on completed tasks
        completedTasks.forEach { task ->
            if (task.completedDate > 0) { // Only count if completedDate is set
                calendar.timeInMillis = task.completedDate
                val day = dateFormat.format(calendar.time)
                dailyCompletionCounts[day] = (dailyCompletionCounts[day] ?: 0) + 1
            }
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        // Sort days to ensure correct order on the chart
        val sortedDays = dailyCompletionCounts.keys.sortedWith(Comparator { day1, day2 ->
            val cal1 = Calendar.getInstance()
            val cal2 = Calendar.getInstance()
            cal1.time = dateFormat.parse(day1)!!
            cal2.time = dateFormat.parse(day2)!!
            cal1.get(Calendar.DAY_OF_YEAR).compareTo(cal2.get(Calendar.DAY_OF_YEAR))
        })

        sortedDays.forEachIndexed { index, day ->
            entries.add(BarEntry(index.toFloat(), dailyCompletionCounts[day]!!.toFloat()))
            labels.add(day)
        }

        if (entries.all { it.y == 0f }) {
            barChart.clear()
            barChart.visibility = View.GONE
            barChartPlaceholder.visibility = View.VISIBLE
            return
        } else {
            barChart.visibility = View.VISIBLE
            barChartPlaceholder.visibility = View.GONE
        }

        val dataSet = BarDataSet(entries, "Tasks Completed").apply {
            color = getThemeAccentColor()
            valueTextColor = getThemeTextColor()
            valueTextSize = 12f
            highLightColor = getThemeHighlightColor()
            setDrawValues(true)
        }
        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            description.isEnabled = false
            setDrawGridBackground(false)
            setFitBars(true)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = getThemeGridColor()
                granularity = 1f
                labelCount = labels.size
                textColor = getThemeTextColor()
                textSize = 12f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = getThemeGridColor()
                axisMinimum = 0f
                textColor = getThemeTextColor()
                textSize = 12f
            }
            axisRight.isEnabled = false
            legend.textColor = getThemeTextColor()
            animateY(1000)
            invalidate()
        }
    }

    private fun updatePieChart(completedTasks: List<Task>) {
        val categoryCounts = completedTasks.groupingBy { it.category }.eachCount()
        val entries = ArrayList<PieEntry>()

        categoryCounts.forEach { (category, count) ->
            entries.add(PieEntry(count.toFloat(), category))
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.visibility = View.GONE
            pieChartPlaceholder.visibility = View.VISIBLE
            return
        } else {
            pieChart.visibility = View.VISIBLE
            pieChartPlaceholder.visibility = View.GONE
        }

        val dataSet = PieDataSet(entries, "Task Categories").apply {
            colors = getThemePieColors()
            valueTextColor = getThemeTextColor()
            valueTextSize = 12f
        }
        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(getThemeTextColor())
            setEntryLabelTextSize(14f)
            centerText = "Task Categories"
            setCenterTextColor(getThemeTextColor())
            legend.textColor = getThemeTextColor()
            animateY(1000)
            invalidate()
        }
    }

    private fun updateLineChart(completedTasks: List<Task>) {
        val calendar = Calendar.getInstance()
        val dailyCompletionCounts = mutableMapOf<Long, Int>() // Use timestamp for sorting

        // Initialize counts for the last 30 days
        for (i in 0 until 30) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            // Normalize to start of day for consistent grouping
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            dailyCompletionCounts[calendar.timeInMillis] = 0
        }

        // Populate counts based on completed tasks
        completedTasks.forEach { task ->
            if (task.completedDate > 0) {
                calendar.timeInMillis = task.completedDate
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val dayTimestamp = calendar.timeInMillis
                dailyCompletionCounts[dayTimestamp] = (dailyCompletionCounts[dayTimestamp] ?: 0) + 1
            }
        }

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Sort by timestamp to ensure correct order on the chart
        val sortedDays = dailyCompletionCounts.keys.sorted()

        sortedDays.forEachIndexed { index, timestamp ->
            entries.add(Entry(index.toFloat(), dailyCompletionCounts[timestamp]!!.toFloat()))
            labels.add(dateFormat.format(Date(timestamp)))
        }

        if (entries.all { it.y == 0f }) {
            lineChart.clear()
            lineChart.visibility = View.GONE
            lineChartPlaceholder.visibility = View.VISIBLE
            return
        } else {
            lineChart.visibility = View.VISIBLE
            lineChartPlaceholder.visibility = View.GONE
        }

        val dataSet = LineDataSet(entries, "Daily Completed Tasks (Last 30 Days)").apply {
            color = getThemeAccentColor()
            valueTextColor = getThemeTextColor()
            valueTextSize = 12f
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(true)
            fillColor = getThemeAccentColor()
            fillAlpha = 80
            setCircleColor(getThemeHighlightColor())
            circleRadius = 4f
        }
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            description.isEnabled = false
            setDrawGridBackground(false)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = getThemeGridColor()
                granularity = 1f
                labelCount = labels.size
                setLabelRotationAngle(45f)
                textColor = getThemeTextColor()
                textSize = 12f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = getThemeGridColor()
                axisMinimum = 0f
                textColor = getThemeTextColor()
                textSize = 12f
            }
            axisRight.isEnabled = false
            legend.textColor = getThemeTextColor()
            animateX(1000)
            invalidate()
        }
    }

    private fun getThemeTextColor(): Int {
        val typedValue = android.util.TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        return typedValue.data
    }

    private fun getThemeAccentColor(): Int {
        val typedValue = android.util.TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    private fun getThemeHighlightColor(): Int {
        // Use secondary color for highlight
        val typedValue = android.util.TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(android.R.attr.colorSecondary, typedValue, true)
        return typedValue.data
    }

    private fun getThemePieColors(): List<Int> {
        // Use a set of vibrant colors for pie chart, adapting to theme
        val isDark = getThemeTextColor() == android.graphics.Color.WHITE
        return if (isDark) {
            listOf(
                android.graphics.Color.parseColor("#BB86FC"),
                android.graphics.Color.parseColor("#03DAC5"),
                android.graphics.Color.parseColor("#FFD600"),
                android.graphics.Color.parseColor("#FF0266"),
                android.graphics.Color.parseColor("#00C853"),
                android.graphics.Color.parseColor("#00B8D4")
            )
        } else {
            listOf(
                android.graphics.Color.parseColor("#6200EE"),
                android.graphics.Color.parseColor("#03DAC5"),
                android.graphics.Color.parseColor("#FFD600"),
                android.graphics.Color.parseColor("#FF0266"),
                android.graphics.Color.parseColor("#00C853"),
                android.graphics.Color.parseColor("#00B8D4")
            )
        }
    }

    private fun getThemeGridColor(): Int {
        // Use a subtle gray for grid lines in light mode, lighter in dark mode
        val isDark = getThemeTextColor() == android.graphics.Color.WHITE
        return if (isDark) android.graphics.Color.parseColor("#444444") else android.graphics.Color.parseColor("#CCCCCC")
    }
}
