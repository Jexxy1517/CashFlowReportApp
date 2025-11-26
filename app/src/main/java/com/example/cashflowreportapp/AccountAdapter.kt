package com.example.cashflowreportapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AccountAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<String, AccountAdapter.AccountViewHolder>(AccountDiffCallback) {

    class AccountViewHolder(itemView: View, val onClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val accountNameTextView: TextView = itemView.findViewById(R.id.tv_account_name)
        private var currentAccount: String? = null

        init {
            itemView.setOnClickListener {
                currentAccount?.let {
                    onClick(it)
                }
            }
        }

        fun bind(accountName: String) {
            currentAccount = accountName
            accountNameTextView.text = accountName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_account, parent, false)
        return AccountViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        holder.bind(account)
    }
}

object AccountDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}