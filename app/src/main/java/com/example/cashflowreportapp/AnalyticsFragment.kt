package com.example.cashflowreportapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.example.cashflowreportapp.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.*

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao()

        observeData(transactionDao)
    }

    private fun observeData(dao: com.example.cashflowreportapp.database.TransactionDao) {
        dao.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            if (transactions.isNotEmpty()) {
                val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                updateSummaryUI(totalIncome, totalExpense)
                setupPieChart(totalIncome, totalExpense)
                setupBarChart(transactions)
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    private fun updateSummaryUI(income: Double, expense: Double) {
        binding.tvTotalIncome.text = formatCurrency(income)
        binding.tvTotalExpense.text = formatCurrency(expense)
        binding.tvBalance.text = formatCurrency(income - expense)
    }

    private fun setupPieChart(income: Double, expense: Double) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(income.toFloat(), "Pemasukan"))
        entries.add(PieEntry(expense.toFloat(), "Pengeluaran"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(binding.pieChart))

        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 61f
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupBarChart(transactions: List<Transaction>) {
        val expenseByAccount = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.account }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var i = 0
        for ((account, total) in expenseByAccount) {
            entries.add(BarEntry(i.toFloat(), total.toFloat()))
            labels.add(account)
            i++
        }

        if (entries.isEmpty()) {
            binding.barChart.visibility = View.GONE
            return
        }

        val dataSet = BarDataSet(entries, "Pengeluaran per Akun")
        dataSet.color = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        binding.barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
        }

        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}