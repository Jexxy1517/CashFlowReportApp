package com.example.cashflowreportapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cashflowreportapp.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var transactionListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    private val _selectedGroupId = MutableLiveData<String?>(null)
    val selectedGroupId: LiveData<String?> = _selectedGroupId

    private val _currentAccountName = MutableLiveData<String>("Keuangan Pribadi")
    val currentAccountName: LiveData<String> = _currentAccountName

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _financialSummary = MutableLiveData<Pair<Double, Double>>()
    val financialSummary: LiveData<Pair<Double, Double>> = _financialSummary

    init {
        listenToUserProfile()
    }

    private fun listenToUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name") ?: "User"
                    _userName.value = name
                }
            }
    }

    fun selectAccount(groupId: String?, name: String) {
        _selectedGroupId.value = groupId
        _currentAccountName.value = name
        fetchTransactions()
    }

    fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return
        val groupId = _selectedGroupId.value

        transactionListener?.remove()

        var query: Query = db.collection("transactions")

        if (groupId == null) {
            query = query.whereEqualTo("userId", userId).whereEqualTo("groupId", null)
        } else {
            query = query.whereEqualTo("groupId", groupId)
        }

        transactionListener = query.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener

            val list = mutableListOf<Transaction>()
            var income = 0.0
            var expense = 0.0

            for (doc in snapshots) {
                val trx = doc.toObject(Transaction::class.java)
                trx.id = doc.id
                list.add(trx)

                if (trx.type == "INCOME") income += trx.amount
                else expense += trx.amount
            }

            list.sortByDescending { it.date }
            _transactions.value = list
            _financialSummary.value = Pair(income, expense)
        }
    }

    override fun onCleared() {
        super.onCleared()
        transactionListener?.remove()
        userListener?.remove()
    }
}