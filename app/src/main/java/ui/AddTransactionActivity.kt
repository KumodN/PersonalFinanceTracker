package ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.R
import data.SharedPrefHelper
import model.Transaction
import kotlin.math.absoluteValue

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSaveTransaction: Button
    private lateinit var btnClearAll: Button
    private lateinit var rgType: RadioGroup
    private lateinit var calendarView: CalendarView
    private var selectedDate: String = ""
    private lateinit var sharedPrefHelper: SharedPrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        sharedPrefHelper = SharedPrefHelper(this)

        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction)
        btnClearAll = findViewById(R.id.btnClear)
        rgType = findViewById(R.id.rgType)
        calendarView = findViewById(R.id.calendarView)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val categories = arrayOf("Food", "Transport", "Bills", "Entertainment", "Other")
        spinnerCategory.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            tvSelectedDate.text = "Date: $selectedDate"
        }

        val editingTransaction = intent.getSerializableExtra("transaction") as? Transaction
        val editingPosition = intent.getIntExtra("position", -1)

        editingTransaction?.let {
            etTitle.setText(it.title)
            etAmount.setText(it.amount.absoluteValue.toString())
            selectedDate = it.date
            tvSelectedDate.text = "Date: $selectedDate"

            val isIncome = it.amount >= 0
            val rbType = if (isIncome) R.id.rbIncome else R.id.rbExpense
            rgType.check(rbType)

            val categoryIndex = categories.indexOf(it.category)
            if (categoryIndex >= 0) spinnerCategory.setSelection(categoryIndex)
        }

        btnSaveTransaction.setOnClickListener {
            val title = etTitle.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = spinnerCategory.selectedItem.toString()

            if (title.isBlank() || amount == null || selectedDate.isBlank() || rgType.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isIncome = rgType.checkedRadioButtonId == R.id.rbIncome
            val signedAmount = if (isIncome) amount else -amount

            val transaction = Transaction(
                title = title,
                amount = signedAmount,
                category = category,
                date = selectedDate
            )

            val currentList = sharedPrefHelper.getTransactions().toMutableList()
            if (editingPosition >= 0) {
                currentList[editingPosition] = transaction
            } else {
                currentList.add(transaction)
            }

            sharedPrefHelper.saveTransactions(currentList)
            Toast.makeText(this, "Transaction Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnClearAll.setOnClickListener {
            etTitle.text.clear()
            etAmount.text.clear()
            rgType.clearCheck()
            spinnerCategory.setSelection(0)
            selectedDate = ""
            tvSelectedDate.text = "Date: Not selected"
            calendarView.date = System.currentTimeMillis()
        }
    }
}
