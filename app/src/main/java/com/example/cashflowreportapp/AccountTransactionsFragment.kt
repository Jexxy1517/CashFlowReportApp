package com.example.cashflowreportapp

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*

class AccountTransactionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var textAccountName: TextView
    private lateinit var buttonExportPdf: Button
    private lateinit var backButton: ImageView
    private lateinit var adapter: TransactionAdapter

    private var totalIncome = 0.0
    private var totalExpense = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_account_transactions)
        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)
        textAccountName = view.findViewById(R.id.text_account_name)
        buttonExportPdf = view.findViewById(R.id.buttonExportPdf)
        backButton = view.findViewById(R.id.iv_back)

        val accountName = arguments?.getString("account_name") ?: "Unknown Account"
        textAccountName.text = accountName

        backButton.setOnClickListener { findNavController().popBackStack() }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = TransactionAdapter(
            onEditClick = { transaction ->
                Toast.makeText(context, "Edit for '${transaction.title}' clicked", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { transaction ->
                Toast.makeText(context, "Delete for '${transaction.title}' clicked", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter

        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
        dao.getTransactionsByAccount(accountName).observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
            updateSummary(transactions)
        }

        buttonExportPdf.setOnClickListener {
            exportToPdf(accountName)
        }
    }

    private fun updateSummary(transactions: List<Transaction>) {
        totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID")).apply {
            maximumFractionDigits = 0
        }

        textTotalIncome.text = formatter.format(totalIncome)
        textTotalExpense.text = formatter.format(totalExpense)
        textBalance.text = formatter.format(balance)
    }

    private fun exportToPdf(accountName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
            val transactions = dao.getTransactionsByAccountSync(accountName)

            if (transactions.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Tidak ada transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val pdfDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "CashFlowReports")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val pdfFile = File(pdfDir, "${accountName}_transactions_${System.currentTimeMillis()}.pdf")

            try {
                val document = Document()
                PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                document.open()

                val title = Paragraph("Laporan Transaksi: $accountName\n\n")
                title.alignment = Element.ALIGN_CENTER
                document.add(title)

                val table = PdfPTable(3)
                table.addCell("Judul")
                table.addCell("Jenis")
                table.addCell("Jumlah")

                val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID"))

                for (t in transactions) {
                    table.addCell(t.title)
                    table.addCell(if (t.type == "INCOME") "Pemasukan" else "Pengeluaran")
                    table.addCell(formatter.format(t.amount))
                }

                document.add(table)
                document.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "PDF disimpan di: ${pdfFile.path}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal mengekspor PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

