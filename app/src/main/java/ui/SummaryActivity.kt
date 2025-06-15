package ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import data.SharedPrefHelper
import model.Transaction
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class SummaryActivity : AppCompatActivity() {

    private lateinit var sharedPrefHelper: SharedPrefHelper
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var timePeriodSpinner: Spinner
    private lateinit var categoryFilter: Spinner
    private lateinit var amountFilter: EditText
    private lateinit var dateFilterButton: Button
    private lateinit var applyFiltersButton: Button

    private var selectedDate: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        sharedPrefHelper = SharedPrefHelper(this)
        pieChart = findViewById(R.id.pieChart)
        lineChart = findViewById(R.id.lineChart)
        timePeriodSpinner = findViewById(R.id.spinnerTimePeriod)
        categoryFilter = findViewById(R.id.spinnerCategoryFilter)
        amountFilter = findViewById(R.id.etAmountFilter)
        dateFilterButton = findViewById(R.id.btnDateFilter)
        applyFiltersButton = findViewById(R.id.btnApplyFilters)

        val timeOptions = arrayOf("Weekly", "Monthly")
        timePeriodSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeOptions)

        val categories = arrayOf("All", "Food", "Transport", "Bills", "Entertainment", "Other")
        categoryFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        dateFilterButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val dpd = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateFilterButton.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            dpd.show()
        }

        applyFiltersButton.setOnClickListener {
            updateCharts()
        }

        updateCharts()
    }

    private fun updateCharts() {
        val transactions = sharedPrefHelper.getTransactions()

        val filteredTransactions = transactions.filter {
            val categorySelected = categoryFilter.selectedItem.toString()
            val amountText = amountFilter.text.toString()
            val minAmount = amountText.toDoubleOrNull()
            val matchesCategory = categorySelected == "All" || it.category == categorySelected
            val matchesAmount = minAmount == null || abs(it.amount) >= minAmount
            val matchesDate = selectedDate == null || it.date == selectedDate

            matchesCategory && matchesAmount && matchesDate
        }

        setupPieChart(filteredTransactions)
        setupLineChart(filteredTransactions)
    }

    private fun setupPieChart(transactions: List<Transaction>) {
        val pieEntries = ArrayList<PieEntry>()
        val grouped = transactions.groupBy { it.category }
            .mapValues { it.value.sumOf { t -> abs(t.amount) } }

        for ((category, total) in grouped) {
            pieEntries.add(PieEntry(total.toFloat(), category))
        }

        val pieDataSet = PieDataSet(pieEntries, "Expenses by Category")
        pieDataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        pieDataSet.valueTextSize = 14f

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupLineChart(transactions: List<Transaction>) {
        val grouped = transactions.groupBy { it.date }
            .toSortedMap()
            .mapValues { it.value.sumOf { t -> t.amount } }

        val lineEntries = ArrayList<Entry>()
        var index = 0f
        for ((_, total) in grouped) {
            lineEntries.add(Entry(index++, total.toFloat()))
        }

        val lineDataSet = LineDataSet(lineEntries, "Net Balance")
        lineDataSet.setColors(*ColorTemplate.COLORFUL_COLORS)
        lineDataSet.circleRadius = 5f
        lineDataSet.lineWidth = 2f
        lineDataSet.valueTextSize = 12f

        val lineData = LineData(lineDataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.animateX(1000)
        lineChart.invalidate()
    }
}
