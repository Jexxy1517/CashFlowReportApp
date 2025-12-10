package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.model.Transaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    private val viewModel: MainViewModel by activityViewModels()
    private val db = FirebaseFirestore.getInstance()
    private var currentList = listOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)
        tvGreeting = view.findViewById(R.id.tv_greeting)
        recyclerView = view.findViewById(R.id.recycler_view)
        fab = view.findViewById(R.id.fab_add_transaction)

        recyclerView.layoutManager = LinearLayoutManager(context)

        transactionAdapter = TransactionAdapter(
            onEditClick = { transaction -> showEditDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteConfirmation(transaction) },
            onItemClick = { transaction -> showOptionsDialog(transaction) }
        )
        recyclerView.adapter = transactionAdapter

        viewModel.transactions.observe(viewLifecycleOwner) { list ->
            currentList = list
            transactionAdapter.submitList(ArrayList(list))
        }

        viewModel.financialSummary.observe(viewLifecycleOwner) { (income, expense) ->
            updateBalanceUI(income, expense)
        }

        viewModel.userName.observe(viewLifecycleOwner) { name ->
            tvGreeting.text = "Halo, $name!"
        }

        fab.setOnClickListener {
            findNavController().navigate(R.id.addTransactionFragment)
        }

        setupSwipeGestures()
    }

    private fun updateBalanceUI(income: Double, expense: Double) {
        val balance = income - expense
        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID"))
        formatter.maximumFractionDigits = 0

        textTotalIncome.text = formatter.format(income)
        textTotalExpense.text = formatter.format(expense)
        textBalance.text = formatter.format(balance)
    }

    private fun showOptionsDialog(transaction: Transaction) {
        val options = arrayOf("Edit Transaksi", "Hapus Transaksi")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Aksi")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(transaction)
                    1 -> showDeleteConfirmation(transaction)
                }
            }
            .show()
    }

    private fun setupSwipeGestures() {
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION && position < currentList.size) {
                    val transaction = currentList[position]
                    if (direction == ItemTouchHelper.RIGHT) {
                        showEditDialog(transaction)
                    } else if (direction == ItemTouchHelper.LEFT) {
                        showDeleteConfirmation(transaction)
                    }
                    transactionAdapter.notifyItemChanged(position)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Transaksi")
            .setMessage("Apakah kamu yakin ingin menghapus '${transaction.title}'? Data yang dihapus tidak dapat dikembalikan.")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                deleteTransactionFromFirestore(transaction)
            }
            .setNegativeButton("Batal") { d, _ ->
                d.dismiss()
                transactionAdapter.notifyDataSetChanged()
            }
            .setCancelable(false)
            .show()
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_edit_transaction, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.input_title)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.input_amount)
        val radioIncome = dialogView.findViewById<RadioButton>(R.id.radio_income)
        val radioExpense = dialogView.findViewById<RadioButton>(R.id.radio_expense)

        titleInput.setText(transaction.title)
        amountInput.setText(BigDecimal(transaction.amount).toPlainString())
        if (transaction.type == "INCOME") radioIncome.isChecked = true else radioExpense.isChecked = true

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Transaksi")
            .setView(dialogView)
            .setPositiveButton("Simpan Perubahan") { _, _ ->
                val newTitle = titleInput.text.toString()
                val newAmount = amountInput.text.toString().toDoubleOrNull() ?: transaction.amount
                val newType = if (radioIncome.isChecked) "INCOME" else "EXPENSE"

                val updatedTransaction = transaction.copy(
                    title = newTitle, amount = newAmount, type = newType
                )
                updateTransactionToFirestore(updatedTransaction)
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                transactionAdapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun updateTransactionToFirestore(transaction: Transaction) {
        if (transaction.id.isNotEmpty()) {
            db.collection("transactions").document(transaction.id)
                .set(transaction)
                .addOnSuccessListener {
                    val notifHelper = NotificationHelper(requireContext())
                    notifHelper.showNotification(
                        "Transaksi Diupdate",
                        "Data '${transaction.title}' berhasil diperbarui."
                    )
                    Toast.makeText(context, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteTransactionFromFirestore(transaction: Transaction) {
        if (transaction.id.isNotEmpty()) {
            db.collection("transactions").document(transaction.id).delete()
                .addOnSuccessListener {
                    val notifHelper = NotificationHelper(requireContext())
                    notifHelper.showNotification(
                        "Transaksi Dihapus",
                        "Data '${transaction.title}' telah dihapus."
                    )
                    Toast.makeText(context, "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                }
        }
    }
}