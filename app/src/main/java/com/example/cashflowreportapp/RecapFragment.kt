package com.example.cashflowreportapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.example.cashflowreportapp.databinding.FragmentRecapBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.*

class RecapFragment : Fragment() {

    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding!!
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val years = (2020..Calendar.getInstance().get(Calendar.YEAR)).toList().reversed()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = adapter
        binding.spinnerYear.setSelection(0)

        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedYear = years[position]
                loadRecapData(selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadRecapData(year: Int) {
        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
        dao.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            val filtered = transactions.filter {
                val cal = Calendar.getInstance()
                cal.time = Date(it.date)
                cal.get(Calendar.YEAR) == year
            }
            setupBarChart(filtered)
        }
    }

    private fun setupBarChart(transactions: List<Transaction>) {
        val incomeByMonth = DoubleArray(12)
        val expenseByMonth = DoubleArray(12)

        for (t in transactions) {
            val cal = Calendar.getInstance()
            cal.time = Date(t.date)
            val month = cal.get(Calendar.MONTH)
            if (t.type == "INCOME") incomeByMonth[month] += t.amount
            else expenseByMonth[month] += t.amount
        }

        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()

        for (i in 0..11) {
            incomeEntries.add(BarEntry(i.toFloat(), incomeByMonth[i].toFloat()))
            expenseEntries.add(BarEntry(i.toFloat(), expenseByMonth[i].toFloat()))
        }

        val incomeSet = BarDataSet(incomeEntries, "Pemasukan")
        val expenseSet = BarDataSet(expenseEntries, "Pengeluaran")

        incomeSet.color = ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        expenseSet.color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        incomeSet.valueTextColor = Color.BLACK
        incomeSet.valueTextSize = 10f
        expenseSet.valueTextColor = Color.BLACK
        expenseSet.valueTextSize = 10f

        val barData = BarData(incomeSet, expenseSet)
        barData.barWidth = 0.35f
        binding.barChart.data = barData

        val months = arrayOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
        val xAxis = binding.barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(months)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textColor = Color.BLACK
        xAxis.textSize = 12f
        xAxis.setDrawGridLines(false)

        val leftAxis: YAxis = binding.barChart.axisLeft
        leftAxis.textColor = Color.BLACK
        leftAxis.axisMinimum = 0f
        binding.barChart.axisRight.isEnabled = false

        val legend: Legend = binding.barChart.legend
        legend.textColor = Color.BLACK
        legend.textSize = 12f
        legend.formSize = 10f
        legend.form = Legend.LegendForm.CIRCLE

        binding.barChart.description.isEnabled = false
        binding.barChart.setFitBars(true)
        binding.barChart.groupBars(0f, 0.4f, 0.05f)
        binding.barChart.animateY(1400, Easing.EaseInOutQuad)
        binding.barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
