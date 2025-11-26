package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cashflowreportapp.databinding.FragmentYouBinding
import com.google.firebase.auth.FirebaseAuth

class YouFragment : Fragment() {
    private var _binding: FragmentYouBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        binding.tvUserEmail.text = currentUser?.email ?: "Pengguna tidak ditemukan"

        binding.btnViewBalance.setOnClickListener {
            val bundle = Bundle().apply {
                putString("account_name", "You")
            }
            findNavController().navigate(R.id.action_youFragment_to_accountTransactionsFragment, bundle)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Anda telah keluar", Toast.LENGTH_SHORT).show()

            findNavController().navigate(R.id.loginFragment)
            val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}