package com.example.cashflowreportapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cashflowreportapp.model.Transaction
import java.text.NumberFormat
import java.util.*

class TransactionAdapter(
    // Parameter ini dipertahankan dari kode lama/baru untuk skalabilitas fitur edit/hapus di masa depan
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit,
    private val onItemClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.transaction_title)
        private val account: TextView = view.findViewById(R.id.transaction_account)
        private val amount: TextView = view.findViewById(R.id.transaction_amount)
        private val icon: ImageView = view.findViewById(R.id.iv_transaction_icon)
        private val imgReceipt: ImageView = view.findViewById(R.id.img_receipt) // Fitur Baru
        private val context = view.context

        fun bind(transaction: Transaction, onItemClick: (Transaction) -> Unit) {
            title.text = transaction.title
            account.text = "Akun: ${transaction.account}"

            val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID"))
            formatter.maximumFractionDigits = 0
            amount.text = formatter.format(transaction.amount)

            // Logika Warna dan Icon
            if (transaction.type == "EXPENSE") {
                amount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                icon.setImageResource(R.drawable.ic_arrow_downward)
                icon.setColorFilter(ContextCompat.getColor(context, R.color.expense_red))
            } else {
                amount.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                icon.setImageResource(R.drawable.ic_arrow_upward)
                icon.setColorFilter(ContextCompat.getColor(context, R.color.income_green))
            }

            // Fitur Baru: Tampilkan Gambar dengan Glide
            if (!transaction.receiptUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(transaction.receiptUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imgReceipt)
            } else {
                // Default placeholder jika tidak ada gambar
                imgReceipt.setImageResource(R.drawable.ic_image_placeholder)
            }

            itemView.setOnClickListener {
                onItemClick(transaction)
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