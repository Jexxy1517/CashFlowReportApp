package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
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
    private lateinit var auth: FirebaseAuth

    private var totalIncome: Double = 0.0
    private var totalExpense: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)
        tvGreeting = view.findViewById(R.id.tv_greeting)
        recyclerView = view.findViewById(R.id.recycler_view)
        fab = view.findViewById(R.id.fab_add_transaction)
        recyclerView.layoutManager = LinearLayoutManager(context)

        setupGreeting()

        val database = AppDatabase.getDatabase(requireContext())
        val transactionDao = database.transactionDao()

        transactionAdapter = TransactionAdapter(
            onEditClick = { transaction -> showEditDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteDialog(transaction) }
        )
        recyclerView.adapter = transactionAdapter

        transactionDao.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
        }

        transactionDao.getTotalAmountByType("INCOME").observe(viewLifecycleOwner) { income ->
            totalIncome = income ?: 0.0
            updateBalanceUI()
        }

        transactionDao.getTotalAmountByType("EXPENSE").observe(viewLifecycleOwner) { expense ->
            totalExpense = expense ?: 0.0
            updateBalanceUI()
        }

        fab.setOnClickListener {
            findNavController().navigate(R.id.addTransactionFragment)
        }

        setupSwipeGestures()
    }

    private fun setupGreeting() {
        val user = auth.currentUser
        val userEmail = user?.email
        val userName = userEmail?.split("@")?.get(0)?.replaceFirstChar { if (it.isLowerCase())
            it.titlecase(Locale.getDefault()) else it.toString() } ?: "Pengguna"
        tvGreeting.text = "Halo, $userName!"
    }

    private fun updateBalanceUI() {
        val balance = totalIncome - totalExpense
        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID"))
        formatter.maximumFractionDigits = 0

        textTotalIncome.text = formatter.format(totalIncome)
        textTotalExpense.text = formatter.format(totalExpense)
        textBalance.text = formatter.format(balance)
    }

    private fun setupSwipeGestures() {
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = transactionAdapter.getTransactionAt(position)

                if (direction == ItemTouchHelper.RIGHT) {
                    showEditDialog(transaction)
                } else if (direction == ItemTouchHelper.LEFT) {
                    showDeleteDialog(transaction)
                }
                transactionAdapter.notifyItemChanged(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showEditDialog(transaction: Transaction) {
        // ... (Fungsi ini tetap sama seperti sebelumnya)
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
            .setPositiveButton("Simpan") { _, _ ->
                val updated = transaction.copy(
                    title = titleInput.text.toString(),
                    amount = amountInput.text.toString().toDoubleOrNull() ?: transaction.amount,
                    type = if (radioIncome.isChecked) "INCOME" else "EXPENSE"
                )
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().update(updated)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteDialog(transaction: Transaction) {
        // ... (Fungsi ini tetap sama seperti sebelumnya)
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Transaksi")
            .setMessage("Yakin mau hapus transaksi \"${transaction.title}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().delete(transaction)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}