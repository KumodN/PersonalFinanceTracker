package data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import model.Transaction
import java.io.File

class SharedPrefHelper(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)

    fun saveTransactions(transactionList: List<Transaction>) {
        val json = Gson().toJson(transactionList)
        prefs.edit().putString("transactions", json).apply()
    }

    fun getTransactions(): List<Transaction> {
        val json = prefs.getString("transactions", null)
        return if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveBudget(budget: Double) {
        prefs.edit().putFloat("BUDGET", budget.toFloat()).apply()
    }

    fun getBudget(): Double {
        return prefs.getFloat("BUDGET", 0f).toDouble()
    }

    fun saveIncome(income: Double) {
        prefs.edit().putFloat("income", income.toFloat()).apply()
    }

    fun getIncome(): Double {
        return prefs.getFloat("income", 0f).toDouble()
    }

    fun saveExpense(expense: Double) {
        prefs.edit().putFloat("expense", expense.toFloat()).apply()
    }

    fun getExpenses(): Double {
        return prefs.getFloat("expense", 0f).toDouble()
    }

    fun backupData(transactions: List<Transaction>) {
        val file = File(context.filesDir, "transactions_backup.json")
        file.writeText(Gson().toJson(transactions))
    }

    fun restoreData(): List<Transaction> {
        val file = File(context.filesDir, "transactions_backup.json")
        return if (file.exists()) {
            val json = file.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
