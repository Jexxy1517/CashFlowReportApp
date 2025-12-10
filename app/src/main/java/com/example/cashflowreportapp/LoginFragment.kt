package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.cashflowreportapp.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navigateToTransactions()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.forgotPasswordFragment)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            val roleDipilihUser = if (binding.rbLoginAdmin.isChecked) "ADMIN" else "USER"

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Memeriksa..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser!!.uid

                        checkRoleDatabase(uid, roleDipilihUser)
                    } else {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Masuk"
                        Toast.makeText(context, "Login Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun checkRoleDatabase(uid: String, rolePilihan: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val roleAsli = document.getString("role") ?: "USER"

                    if (roleAsli == rolePilihan) {
                        Toast.makeText(context, "Login Berhasil sebagai $roleAsli", Toast.LENGTH_SHORT).show()
                        navigateToTransactions()
                    } else {

                        auth.signOut()
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Masuk"

                        Toast.makeText(context, "Akses Ditolak! Akun Anda terdaftar sebagai $roleAsli, bukan $rolePilihan", Toast.LENGTH_LONG).show()
                    }
                } else {
                    auth.signOut()
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Masuk"
                    Toast.makeText(context, "Data pengguna tidak ditemukan!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                auth.signOut()
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Masuk"
                Toast.makeText(context, "Gagal koneksi database: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToTransactions() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()

        findNavController().navigate(R.id.transactionsFragment, null, navOptions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}