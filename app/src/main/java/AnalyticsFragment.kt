import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
// Remove or comment out the incorrect TextView import if not used
// import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart // Corrected import
import com.github.mikephil.charting.data.BarData // Corrected import
import com.github.mikephil.charting.data.BarDataSet // Corrected import
import com.github.mikephil.charting.data.BarEntry // Corrected import

class AnalyticsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        val barChart = BarChart(requireContext())
        val entries = listOf(
            BarEntry(1f, 3f), // Mon
            BarEntry(2f, 5f), // Tue
            BarEntry(3f, 2f), // Wed
            BarEntry(4f, 4f), // Thu
            BarEntry(5f, 6f), // Fri
            BarEntry(6f, 1f), // Sat
            BarEntry(7f, 0f)  // Sun
        )
        val dataSet = BarDataSet(entries, "Tasks Completed")
        dataSet.color = Color.parseColor("#4CAF50")
        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.description.text = "Weekly Task Completion"
        layout.addView(barChart)
        return layout
    }
}