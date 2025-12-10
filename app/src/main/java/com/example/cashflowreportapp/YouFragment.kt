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
import androidx.navigation.fragment.findNavController
import com.example.cashflowreportapp.databinding.FragmentYouBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class YouFragment : Fragment() {

    private var _binding: FragmentYouBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var myRole = "USER"
    private var myFamilyGroupId: String? = null
    private var currentName: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserProfile()

        binding.btnEditName.setOnClickListener {
            showEditNameDialog()
        }

        binding.btnViewPersonal.setOnClickListener {
            val displayName = if (currentName.isNotEmpty()) currentName else "You"
            val bundle = Bundle().apply { putString("account_name", displayName) }
            findNavController().navigate(R.id.accountTransactionsFragment, bundle)
        }

        binding.btnInviteMember.setOnClickListener {
            if (myFamilyGroupId != null) {
                showInviteDialog(myFamilyGroupId!!)
            } else {
                Toast.makeText(context, "Grup keluarga belum siap.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email

        binding.tvEmailDisplay.text = email

        db.collection("users").document(uid).addSnapshotListener { doc, e ->
            if (e != null) return@addSnapshotListener
            if (doc != null && doc.exists()) {
                myRole = doc.getString("role") ?: "USER"
                currentName = doc.getString("name") ?: "Pengguna"
                val familyId = doc.getString("familyGroupId")

                binding.tvRoleDisplay.text = if (myRole == "ADMIN") "ADMIN (KEPALA KELUARGA)" else "ANGGOTA"
                binding.tvNameDisplay.text = currentName

                if (myRole == "ADMIN") {
                    binding.btnInviteMember.visibility = View.VISIBLE
                    if (familyId != null) {
                        myFamilyGroupId = familyId
                    } else {
                        db.collection("savings_groups").whereEqualTo("ownerId", uid).get()
                            .addOnSuccessListener { groups ->
                                if (!groups.isEmpty) myFamilyGroupId = groups.documents[0].id
                            }
                    }
                } else {
                    binding.btnInviteMember.visibility = View.GONE
                    myFamilyGroupId = familyId
                }
            }
        }
    }

    private fun showEditNameDialog() {
        val inputName = TextInputEditText(requireContext())
        inputName.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        inputName.setText(currentName)
        inputName.hint = "Nama Baru"

        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(60, 20, 60, 0)
        inputName.layoutParams = params
        container.addView(inputName)

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Nama")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = inputName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateNameInFirestore(newName)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateNameInFirestore(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("name", newName)
            .addOnSuccessListener {
                Toast.makeText(context, "Nama berhasil diubah!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal mengubah nama: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                auth.signOut()
                findNavController().navigate(R.id.loginFragment)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showInviteDialog(groupId: String) {
        val inputEmail = TextInputEditText(requireContext())
        inputEmail.hint = "Masukkan Email Anggota"
        val container = FrameLayout(requireContext())
        container.setPadding(60, 30, 60, 10)
        container.addView(inputEmail)

        AlertDialog.Builder(requireContext())
            .setTitle("Undang Anggota Keluarga")
            .setMessage("Pastikan anggota tersebut sudah mendaftar aplikasi.")
            .setView(container)
            .setPositiveButton("Tambahkan") { _, _ ->
                val targetEmail = inputEmail.text.toString().trim()
                if (targetEmail.isNotEmpty()) {
                    processAddMember(groupId, targetEmail)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun processAddMember(groupId: String, targetEmail: String) {
        db.collection("users").whereEqualTo("email", targetEmail).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(context, "Email tidak ditemukan! Pastikan user sudah register.", Toast.LENGTH_LONG).show()
                } else {
                    val targetUid = docs.documents[0].getString("uid") ?: return@addOnSuccessListener
                    db.collection("savings_groups").document(groupId)
                        .update("members", FieldValue.arrayUnion(targetUid))
                        .addOnSuccessListener {
                            db.collection("users").document(targetUid)
                                .update("familyGroupId", groupId)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Berhasil menambahkan $targetEmail!", Toast.LENGTH_SHORT).show()
                                }
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}