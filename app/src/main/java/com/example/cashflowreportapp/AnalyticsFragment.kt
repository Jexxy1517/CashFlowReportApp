package com.example.cashflowreportapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.cashflowreportapp.databinding.FragmentAnalyticsBinding
import com.example.cashflowreportapp.model.Transaction
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null) {
                updateSummaryCards(transactions)
                setupPieChart(transactions)
                setupBarChart(transactions)
            }
        }
    }

    private fun updateSummaryCards(transactions: List<Transaction>) {
        var income = 0.0
        var expense = 0.0

        for (trx in transactions) {
            if (trx.type == "INCOME") income += trx.amount
            else expense += trx.amount
        }

        val balance = income - expense
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0

        binding.tvTotalIncome.text = formatter.format(income)
        binding.tvTotalExpense.text = formatter.format(expense)
        binding.tvBalance.text = formatter.format(balance)
    }

    private fun setupPieChart(transactions: List<Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (trx in transactions) {
            if (trx.type == "INCOME") totalIncome += trx.amount
            else totalExpense += trx.amount
        }

        val entries = ArrayList<PieEntry>()
        if (totalIncome > 0) entries.add(PieEntry(totalIncome.toFloat(), "Pemasukan"))
        if (totalExpense > 0) entries.add(PieEntry(totalExpense.toFloat(), "Pengeluaran"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.pieChart))

        binding.pieChart.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK)
            centerText = "Rasio"
            setCenterTextSize(14f)
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }

        if (totalIncome == 0.0 && totalExpense == 0.0) binding.pieChart.clear()
    }

    private fun setupBarChart(transactions: List<Transaction>) {
        val expensesByCategory = HashMap<String, Double>()

        for (trx in transactions) {
            if (trx.type == "EXPENSE") {
                val cat = if (trx.category.isNotEmpty()) trx.category else "Tanpa Kategori"
                val currentTotal = expensesByCategory.getOrDefault(cat, 0.0)
                expensesByCategory[cat] = currentTotal + trx.amount
            }
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f

        for ((catName, amount) in expensesByCategory) {
            entries.add(BarEntry(index, amount.toFloat()))
            labels.add(catName)
            index++
        }

        val dataSet = BarDataSet(entries, "Pengeluaran per Kategori")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.expense_red)
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = Color.BLACK

        val data = BarData(dataSet)
        data.barWidth = 0.6f

        binding.barChart.apply {
            this.data = data
            description.isEnabled = false

            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.labelRotationAngle = 0f

            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f

            animateY(1000)
            invalidate()
        }
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        if (entries.isEmpty()) binding.barChart.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}