package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cashflowreportapp.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Data tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = if (binding.rbAdmin.isChecked) "ADMIN" else "USER"

            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Mendaftarkan..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    val defaultName = email.split("@")[0].replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    saveUserToFirestore(uid, email, defaultName, role)
                }
                .addOnFailureListener {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Daftar Sekarang"
                    Toast.makeText(context, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserToFirestore(uid: String, email: String, name: String, role: String) {
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "name" to name,
            "role" to role
        )

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                if (role == "ADMIN") {
                    createDefaultFamilyGroup(uid, name)
                } else {
                    finishRegistration()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal simpan user database", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createDefaultFamilyGroup(adminUid: String, adminName: String) {
        val groupName = "Keluarga $adminName"

        val newGroup = hashMapOf(
            "name" to groupName,
            "ownerId" to adminUid,
            "members" to listOf(adminUid),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("savings_groups").add(newGroup)
            .addOnSuccessListener { docRef ->
                db.collection("users").document(adminUid).update("familyGroupId", docRef.id)
                finishRegistration()
            }
    }

    private fun finishRegistration() {
        Toast.makeText(context, "Registrasi Berhasil! Silakan Login.", Toast.LENGTH_SHORT).show()
        auth.signOut()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}