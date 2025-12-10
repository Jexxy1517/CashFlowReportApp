package com.example.cashflowreportapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.model.SavingsGroup

class AccountAdapter(
    private val onClick: (SavingsGroup) -> Unit
) : ListAdapter<SavingsGroup, AccountAdapter.AccountViewHolder>(AccountDiffCallback) {

    class AccountViewHolder(itemView: View, val onClick: (SavingsGroup) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val accountNameTextView: TextView = itemView.findViewById(R.id.tv_account_name)
        private var currentGroup: SavingsGroup? = null

        init {
            itemView.setOnClickListener {
                currentGroup?.let {
                    onClick(it)
                }
            }
        }

        fun bind(group: SavingsGroup) {
            currentGroup = group
            accountNameTextView.text = group.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object AccountDiffCallback : DiffUtil.ItemCallback<SavingsGroup>() {
    override fun areItemsTheSame(oldItem: SavingsGroup, newItem: SavingsGroup): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SavingsGroup, newItem: SavingsGroup): Boolean {
        return oldItem == newItem
    }
}