package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.example.cashflowreportapp.databinding.FragmentAddTransacctionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class AddTransactionFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddTransacctionBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransacctionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val accountAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            AccountsFragment.globalAccounts
        )
        (binding.spinnerAccount as? AutoCompleteTextView)?.setAdapter(accountAdapter)

        binding.buttonSave.setOnClickListener {

            if (binding.toggleButtonGroup.checkedButtonId == -1) {
                Toast.makeText(requireContext(),
                    "Harap pilih tipe transaksi (Pemasukan/Pengeluaran)",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val title = binding.inputTitle.text.toString().trim()
            val amount = binding.inputAmount.text.toString().toDoubleOrNull() ?: 0.0

            val type = if (binding.toggleButtonGroup.checkedButtonId == R.id.btn_income) {
                "INCOME"
            } else {
                "EXPENSE"
            }

            val date = System.currentTimeMillis()
            val account = binding.spinnerAccount.text.toString()

            if (title.isNotBlank() && amount > 0 && account.isNotBlank()) {
                val transaction = Transaction(0, title, amount, type, date, account)
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().insert(transaction)
                    dismiss()
                }
            } else {
                Toast.makeText(requireContext(),
                    "Harap isi semua kolom dengan benar!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}