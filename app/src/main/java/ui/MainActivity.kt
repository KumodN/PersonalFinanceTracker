package ui

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.example.personalfinancetracker.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import data.SharedPrefHelper
import model.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvAvailableBudget: TextView
    private lateinit var btnAddTransaction: Button
    private lateinit var btnViewSummary: Button
    private lateinit var btnSetBudget: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var sharedPrefHelper: SharedPrefHelper
    private lateinit var adapter: TransactionAdapter
    private lateinit var barChart: BarChart
    private lateinit var budgetProgressBar: ProgressBar
    private var transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupButtonListeners()
        createNotificationChannel()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SET_BUDGET_REQUEST && resultCode == RESULT_OK) {
            updateSummary()
            checkBudget()
        }
    }

    private fun initViews() {
        sharedPrefHelper = SharedPrefHelper(this)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvAvailableBudget = findViewById(R.id.tvAvailableBudget)
        btnAddTransaction = findViewById(R.id.btnAddTransaction)
        btnViewSummary = findViewById(R.id.btnViewSummary)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        recyclerView = findViewById(R.id.recyclerViewTransactions)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        barChart = findViewById(R.id.barChart)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(transactions, object : TransactionAdapter.OnTransactionActionListener {
            override fun onEdit(transaction: Transaction, position: Int) {
                val intent = Intent(this@MainActivity, AddTransactionActivity::class.java).apply {
                    putExtra("transaction", transaction)
                    putExtra("position", position)
                }
                startActivityForResult(intent, EDIT_TRANSACTION_REQUEST)
            }

            override fun onDelete(transaction: Transaction, position: Int) {
                transactions.removeAt(position)
                sharedPrefHelper.saveTransactions(transactions)
                adapter.notifyItemRemoved(position)
                updateSummary()
                showWeeklyChart()
            }
        })
        recyclerView.adapter = adapter
    }

    private fun setupButtonListeners() {
        btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        btnViewSummary.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        btnSetBudget.setOnClickListener {
            startActivityForResult(Intent(this, BudgetActivity::class.java), SET_BUDGET_REQUEST)
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_settings, popup.menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_backup -> exportTransactionsAsJson()
                    R.id.menu_restore -> importTransactionsFromJson()
                    R.id.menu_logout -> startActivity(Intent(this, LoginActivity::class.java))
                    R.id.menu_view_summary -> startActivity(Intent(this, SummaryActivity::class.java))
                    R.id.menu_privacy -> startActivity(Intent(this, PrivacyPolicyActivity::class.java))
                    R.id.menu_profile -> Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                    R.id.menu_export_text -> exportTransactionsAsText()
                }
                true
            }
            popup.show()
        }
    }

    private fun refreshData() {
        loadTransactions()
        updateSummary()
        checkBudget()
        showWeeklyChart()
    }

    private fun loadTransactions() {
        transactions.clear()
        transactions.addAll(sharedPrefHelper.getTransactions())
        adapter.notifyDataSetChanged()
    }

    private fun updateSummary() {
        val income = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = transactions.filter { it.amount < 0 }.sumOf { it.amount }.let { kotlin.math.abs(it) }
        val budget = sharedPrefHelper.getBudget()
        val availableBudget = budget + income - expense

        tvTotalIncome.text = "Main Salary: Rs. %.2f".format(income)
        tvTotalExpense.text = "Expenses: Rs. %.2f".format(expense)
        tvAvailableBudget.text = "Rs. %.2f".format(availableBudget)

        val progressPercent = if (budget == 0.0) 0 else ((expense / budget) * 100).toInt()
        budgetProgressBar.max = 100
        budgetProgressBar.progress = progressPercent.coerceAtMost(100)
    }

    private fun showWeeklyChart() {
        val calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) }
        val weeklyData = FloatArray(7)

        for (i in 0..6) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = calendar.timeInMillis

            weeklyData[i] = transactions.filter {
                it.amount < 0 && parseDateToMillis(it.date) in dayStart until dayEnd
            }.sumOf { it.amount }.let { kotlin.math.abs(it).toFloat() }
        }

        val entries = weeklyData.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "Weekly Expenses").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.yellow)
        }

        val barData = BarData(dataSet).apply { barWidth = 0.9f }

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
        }

        barChart.invalidate()
    }

    private fun parseDateToMillis(dateStr: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun checkBudget() {
        val expense = transactions.filter { it.amount < 0 }.sumOf { it.amount }.let { kotlin.math.abs(it) }
        val budget = sharedPrefHelper.getBudget()

        if (expense >= budget) {
            sendBudgetExceededNotification()
            budgetProgressBar.progressTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        } else {
            budgetProgressBar.progressTintList = ContextCompat.getColorStateList(this, R.color.green)
        }
    }

    private fun sendBudgetExceededNotification() {
        val notification = NotificationCompat.Builder(this, "BUDGET_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Budget Exceeded!")
            .setContentText("You have exceeded your monthly budget!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "BUDGET_CHANNEL", "Budget Notifications", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Channel for budget exceeded notifications" }

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    //Backup
    private fun exportTransactionsAsJson() {
        val json = Gson().toJson(transactions)
        File(filesDir, "transactions_backup.json").writeText(json)
        Toast.makeText(this, "Backup saved to internal storage", Toast.LENGTH_SHORT).show()
    }

    private fun importTransactionsFromJson() {
        val file = File(filesDir, "transactions_backup.json")
        if (file.exists()) {
            val json = file.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            val importedTransactions: List<Transaction> = Gson().fromJson(json, type)
            transactions.clear()
            transactions.addAll(importedTransactions)
            sharedPrefHelper.saveTransactions(transactions)
            adapter.notifyDataSetChanged()
            updateSummary()
            showWeeklyChart()
            Toast.makeText(this, "Backup restored", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportTransactionsAsText() {
        val file = File(filesDir, "transactions_backup.txt")
        val text = transactions.joinToString("\n") {
            "Date: ${it.date}, Category: ${it.category}, Amount: ${it.amount}, Note: ${it.note}"
        }
        file.writeText(text)
        Toast.makeText(this, "Text backup saved", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission denied, notifications won't be sent.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EDIT_TRANSACTION_REQUEST = 1
        const val SET_BUDGET_REQUEST = 2
    }
}