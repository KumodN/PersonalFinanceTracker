package ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.R
import data.SharedPrefHelper

class BudgetActivity : AppCompatActivity() {

    private lateinit var etMonthlyBudget: EditText
    private lateinit var btnSaveBudget: Button
    private lateinit var tvUsedBudget: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var tvIncome: TextView
    private lateinit var tvExpenses: TextView
    private lateinit var tvRemainingBudget: TextView

    private lateinit var sharedPrefHelper: SharedPrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        sharedPrefHelper = SharedPrefHelper(this)

        etMonthlyBudget = findViewById(R.id.etMonthlyBudget)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        tvUsedBudget = findViewById(R.id.tvUsedBudget)
        progressBudget = findViewById(R.id.progressBudget)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpenses = findViewById(R.id.tvExpenses)
        tvRemainingBudget = findViewById(R.id.tvRemainingBudget)

        btnSaveBudget.setOnClickListener {
            val budgetAmount = etMonthlyBudget.text.toString().toDoubleOrNull()
            if (budgetAmount != null && budgetAmount >= 0) {
                sharedPrefHelper.saveBudget(budgetAmount)
                hideKeyboard()
                updateProgress()
                Toast.makeText(this, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        updateProgress()
    }

    private fun updateProgress() {
        val budget = sharedPrefHelper.getBudget()
        val income = sharedPrefHelper.getIncome()
        val expenses = sharedPrefHelper.getExpenses()
        val remaining = budget - expenses

        tvIncome.text = "Income: Rs. %.2f".format(income)
        tvExpenses.text = "Expenses: Rs. %.2f".format(expenses)
        tvUsedBudget.text = "Used: Rs. %.2f".format(expenses)
        tvRemainingBudget.text = "Remaining: Rs. %.2f".format(remaining)

        val progressPercent = if (budget > 0) ((expenses / budget) * 100).toInt().coerceAtMost(100) else 0
        progressBudget.progress = progressPercent
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(etMonthlyBudget.windowToken, 0)
    }
}
