package com.example.cashflowreportapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.cashflowreportapp.database.Transaction
import com.example.cashflowreportapp.database.TransactionDao

class AnalyticsViewModel(private val transactionDao: TransactionDao) : ViewModel() {

    val totalIncome: LiveData<Double?> = transactionDao.
    getTotalAmountByType("INCOME")
    val totalExpense: LiveData<Double?> = transactionDao.
    getTotalAmountByType("EXPENSE")
    val allTransactions: LiveData<List<Transaction>> = transactionDao.
    getAllTransactions()
}