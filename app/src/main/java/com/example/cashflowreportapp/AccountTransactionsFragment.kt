package com.example.cashflowreportapp

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.model.SavingsGroup
import com.example.cashflowreportapp.model.Transaction
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AccountTransactionsFragment : Fragment() {

    // ... view components ...
    private lateinit var recyclerView: RecyclerView
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var textAccountName: TextView
    private lateinit var buttonExportPdf: Button
    private lateinit var backButton: ImageView
    private lateinit var btnAddMember: ImageView

    private lateinit var adapter: TransactionAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var transactionsList = mutableListOf<Transaction>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init Views
        recyclerView = view.findViewById(R.id.recycler_view_account_transactions)
        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)
        textAccountName = view.findViewById(R.id.text_account_name)
        buttonExportPdf = view.findViewById(R.id.buttonExportPdf)
        backButton = view.findViewById(R.id.iv_back)
        btnAddMember = view.findViewById(R.id.btn_add_member)

        val accountName = arguments?.getString("account_name") ?: "Detail Akun"
        val groupId = arguments?.getString("group_id")

        textAccountName.text = accountName
        backButton.setOnClickListener { findNavController().popBackStack() }

        setupRecyclerView()
        checkAdminRole(groupId)
        loadTransactions(accountName, groupId)

        buttonExportPdf.setOnClickListener {
            exportToPdf(accountName, transactionsList)
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onEditClick = { transaction -> showEditDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteConfirmation(transaction) },
            onItemClick = { transaction -> showOptionsDialog(transaction) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun checkAdminRole(groupId: String?) {
        if (groupId == null) {
            btnAddMember.visibility = View.GONE
            return
        }

        val myUid = auth.currentUser?.uid
        db.collection("savings_groups").document(groupId).get()
            .addOnSuccessListener { doc ->
                val group = doc.toObject(SavingsGroup::class.java)
                if (group != null && group.ownerId == myUid) {
                    btnAddMember.visibility = View.VISIBLE
                    btnAddMember.setOnClickListener { showAddMemberDialog(groupId) }
                } else {
                    btnAddMember.visibility = View.GONE
                }
            }
    }

    private fun loadTransactions(accountName: String, groupId: String?) {
        val myUid = auth.currentUser?.uid ?: return
        var query: Query = db.collection("transactions")

        query = if (groupId != null) {
            query.whereEqualTo("groupId", groupId)
        } else {
            query.whereEqualTo("userId", myUid).whereEqualTo("groupId", null)
        }

        query.addSnapshotListener { snapshots, _ ->
            if (snapshots != null) {
                transactionsList.clear()
                var income = 0.0
                var expense = 0.0

                for (doc in snapshots) {
                    val trx = doc.toObject(Transaction::class.java)
                    trx.id = doc.id
                    transactionsList.add(trx)
                    if (trx.type == "INCOME") income += trx.amount else expense += trx.amount
                }
                transactionsList.sortByDescending { it.date }
                adapter.submitList(ArrayList(transactionsList))

                val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply { maximumFractionDigits = 0 }
                textTotalIncome.text = formatter.format(income)
                textTotalExpense.text = formatter.format(expense)
                textBalance.text = formatter.format(income - expense)
            }
        }
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
            .setPositiveButton("Simpan") { _, _ ->
                val newTitle = titleInput.text.toString()
                val newAmount = amountInput.text.toString().toDoubleOrNull() ?: transaction.amount
                val newType = if (radioIncome.isChecked) "INCOME" else "EXPENSE"

                val updatedTransaction = transaction.copy(
                    title = newTitle, amount = newAmount, type = newType
                )
                updateTransactionToFirestore(updatedTransaction)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateTransactionToFirestore(transaction: Transaction) {
        if (transaction.id.isNotEmpty()) {
            db.collection("transactions").document(transaction.id)
                .set(transaction)
                .addOnSuccessListener {
                    val notifHelper = NotificationHelper(requireContext())
                    notifHelper.showNotification("Update Berhasil", "Transaksi '${transaction.title}' diperbarui.")
                    Toast.makeText(context, "Diperbarui", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Transaksi")
            .setMessage("Yakin hapus \"${transaction.title}\"?")
            .setPositiveButton("Hapus") { _, _ -> deleteTransactionFromFirestore(transaction) }
            .setNegativeButton("Batal") { d, _ -> d.dismiss() }
            .show()
    }

    private fun deleteTransactionFromFirestore(transaction: Transaction) {
        if (transaction.id.isNotEmpty()) {
            db.collection("transactions").document(transaction.id).delete()
                .addOnSuccessListener {
                    val notifHelper = NotificationHelper(requireContext())
                    notifHelper.showNotification("Dihapus", "Transaksi '${transaction.title}' telah dihapus.")
                    Toast.makeText(context, "Terhapus", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun exportToPdf(name: String, list: List<Transaction>) {
        if (list.isEmpty()) {
            Toast.makeText(context, "Data kosong, tidak bisa export", Toast.LENGTH_SHORT).show()
            return
        }

        val doc = Document()
        val fileName = "Laporan_${System.currentTimeMillis()}.pdf"
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName

        try {
            PdfWriter.getInstance(doc, FileOutputStream(filePath))
            doc.open()

            doc.add(Paragraph("Laporan Keuangan: $name"))
            doc.add(Paragraph("Tanggal: ${Date()}"))
            doc.add(Paragraph("\n"))

            val table = PdfPTable(4)
            table.addCell("Tanggal")
            table.addCell("Kategori")
            table.addCell("Ket")
            table.addCell("Jumlah")

            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

            for (trx in list) {
                table.addCell(dateFormat.format(trx.date))
                table.addCell(trx.category)
                table.addCell(trx.title)

                val amountStr = currencyFormat.format(trx.amount)
                if (trx.type == "EXPENSE") table.addCell("-$amountStr") else table.addCell(amountStr)
            }

            doc.add(table)
            doc.close()
            Toast.makeText(context, "PDF Disimpan di Download/$fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Gagal Export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showAddMemberDialog(groupId: String) {
        val inputEmail = TextInputEditText(requireContext())
        inputEmail.hint = "Email Anggota"
        val container = FrameLayout(requireContext())
        container.setPadding(50, 20, 50, 20)
        container.addView(inputEmail)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Anggota Keluarga")
            .setView(container)
            .setPositiveButton("Undang") { _, _ ->
                val email = inputEmail.text.toString().trim()
                if (email.isNotEmpty()) inviteMemberByEmail(groupId, email)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun inviteMemberByEmail(groupId: String, email: String) {
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(context, "Email user tidak ditemukan!", Toast.LENGTH_SHORT).show()
                } else {
                    val targetUid = docs.documents[0].getString("uid") ?: return@addOnSuccessListener

                    db.collection("savings_groups").document(groupId)
                        .update("members", FieldValue.arrayUnion(targetUid))
                        .addOnSuccessListener { Toast.makeText(context, "Berhasil!", Toast.LENGTH_SHORT).show() }
                }
            }
    }
}