package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AccountsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inputAccount: TextInputEditText
    private lateinit var addButton: MaterialButton
    private lateinit var accountAdapter: AccountAdapter

    companion object {
        var globalAccounts = mutableListOf<String>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_accounts,
            container, false)

        recyclerView = view.findViewById(R.id.recyclerViewAccounts)
        inputAccount = view.findViewById(R.id.editTextAccount)
        addButton = view.findViewById(R.id.buttonAddAccount)

        if (!globalAccounts.contains("You")) {
            globalAccounts.add(0, "You")
        }

        accountAdapter = AccountAdapter { accountName ->
            val bundle = Bundle().apply {
                putString("account_name", accountName)
            }
            findNavController().navigate(R.id.accountTransactionsFragment, bundle)
        }
        recyclerView.adapter = accountAdapter
        accountAdapter.submitList(globalAccounts.toList())

        addButton.setOnClickListener {
            val newAccount = inputAccount.text.toString().trim()
            if (newAccount.isNotEmpty() && !globalAccounts.contains(newAccount)) {
                globalAccounts.add(newAccount)
                accountAdapter.submitList(globalAccounts.toList())
                inputAccount.text?.clear()
            } else if (newAccount.isEmpty()) {
                Toast.makeText(requireContext(), "Nama akun tidak boleh kosong",
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Nama akun sudah ada",
                    Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}