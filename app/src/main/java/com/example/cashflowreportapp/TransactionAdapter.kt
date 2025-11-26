package com.example.cashflowreportapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    fun getTransactionAt(position: Int): Transaction {
        return getItem(position)
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.transaction_title)
        private val account: TextView = view.findViewById(R.id.transaction_account)
        private val amount: TextView = view.findViewById(R.id.transaction_amount)
        private val icon: ImageView = view.findViewById(R.id.iv_transaction_icon)
        private val context = view.context

        fun bind(transaction: Transaction) {
            title.text = transaction.title
            account.text = "Akun: ${transaction.account}"

            val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID"))
            formatter.maximumFractionDigits = 0
            amount.text = formatter.format(transaction.amount)

            if (transaction.type == "EXPENSE") {
                amount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                icon.setImageResource(R.drawable.ic_arrow_downward)
                icon.setColorFilter(ContextCompat.getColor(context, R.color.expense_red))
            } else { // INCOME
                amount.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                icon.setImageResource(R.drawable.ic_arrow_upward)
                icon.setColorFilter(ContextCompat.getColor(context, R.color.income_green))
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}