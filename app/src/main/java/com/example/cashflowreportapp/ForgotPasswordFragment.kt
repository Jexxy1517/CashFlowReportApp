package com.example.cashflowreportapp

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cashflowreportapp.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()

        binding.btnReset.setOnClickListener {
            val email = binding.edtForgotPasswordEmail.text.toString().trim()

            if (TextUtils.isEmpty(email)) {
                binding.edtForgotPasswordEmail.error = "Email tidak boleh kosong"
            } else {
                resetPassword(email)
            }
        }

        binding.btnForgotPasswordBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun resetPassword(email: String) {
        binding.forgetPasswordProgressbar.visibility = View.VISIBLE
        binding.btnReset.visibility = View.INVISIBLE

        mAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Link reset password telah dikirim ke email Anda.",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Gagal mengirim email: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.forgetPasswordProgressbar.visibility = View.INVISIBLE
                binding.btnReset.visibility = View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
