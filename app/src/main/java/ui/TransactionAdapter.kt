package ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetracker.R
import model.Transaction

class TransactionAdapter(
    private val transactions: MutableList<Transaction>,
    private val listener: OnTransactionActionListener
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    interface OnTransactionActionListener {
        fun onEdit(transaction: Transaction, position: Int)
        fun onDelete(transaction: Transaction, position: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val categoryText: TextView = itemView.findViewById(R.id.tvCategory)
        val dateText: TextView = itemView.findViewById(R.id.tvDate)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reversedPosition = transactions.size - 1 - position
        val transaction = transactions[reversedPosition]
        holder.tvTitle.text = transaction.title
        holder.tvAmount.text = "Rs. %.2f".format(transaction.amount)
        holder.categoryText.text = transaction.category
        holder.dateText.text = transaction.date
        holder.tvAmount.setTextColor(
            if (transaction.amount >= 0) Color.GREEN else Color.RED
        )

        holder.btnEdit.setOnClickListener {
            listener.onEdit(transaction, reversedPosition)
        }

        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Yes") { _, _ ->
                    listener.onDelete(transaction, reversedPosition)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
