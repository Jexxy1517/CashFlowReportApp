package com.example.cashflowreportapp

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cashflowreportapp.databinding.FragmentAccountsBinding
import com.example.cashflowreportapp.model.SavingsGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountsFragment : Fragment() {

    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var accountAdapter: AccountAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadMySavingsGroups()
    }

    private fun setupRecyclerView() {
        accountAdapter = AccountAdapter { group ->
            viewModel.selectAccount(group.id, group.name)
            findNavController().navigate(R.id.transactionsFragment)
        }

        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accountAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cardPersonal.setOnClickListener {
            viewModel.selectAccount(null, "Keuangan Pribadi")
            findNavController().navigate(R.id.transactionsFragment)
        }

        binding.fabAddGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun loadMySavingsGroups() {
        val myUid = auth.currentUser?.uid ?: return

        db.collection("savings_groups")
            .whereArrayContains("members", myUid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val groupList = mutableListOf<SavingsGroup>()
                    for (doc in snapshots) {
                        val group = doc.toObject(SavingsGroup::class.java)
                        group.id = doc.id
                        groupList.add(group)
                    }
                    accountAdapter.submitList(groupList)

                }
            }
    }

    private fun showCreateGroupDialog() {
        val context = requireContext()

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Buat Grup Tabungan Baru")

        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 20)

        val textInputLayout = TextInputLayout(context)
        val inputName = TextInputEditText(context)
        inputName.hint = "Nama Grup (Mis: Liburan Keluarga)"
        inputName.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS

        textInputLayout.addView(inputName)
        container.addView(textInputLayout, params)

        builder.setView(container)

        builder.setPositiveButton("Buat") { _, _ ->
            val groupName = inputName.text.toString().trim()
            if (groupName.isNotEmpty()) {
                createNewGroupToFirestore(groupName)
            } else {
                Toast.makeText(context, "Nama grup tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun createNewGroupToFirestore(groupName: String) {
        val myUid = auth.currentUser?.uid ?: return

        val newGroup = hashMapOf(
            "name" to groupName,
            "ownerId" to myUid,
            "members" to listOf(myUid),
            "createdAt" to System.currentTimeMillis(),
            "totalBalance" to 0.0
        )

        binding.progressBar?.visibility = View.VISIBLE

        db.collection("savings_groups")
            .add(newGroup)
            .addOnSuccessListener { documentReference ->
                binding.progressBar?.visibility = View.GONE
                Toast.makeText(context, "Grup '$groupName' berhasil dibuat!", Toast.LENGTH_SHORT).show()

                viewModel.selectAccount(documentReference.id, groupName)
                findNavController().navigate(R.id.transactionsFragment)
            }
            .addOnFailureListener { e ->
                binding.progressBar?.visibility = View.GONE
                Toast.makeText(context, "Gagal membuat grup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}