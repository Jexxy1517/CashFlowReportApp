package com.example.cashflowreportapp.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Transaction(
    @get:Exclude var id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "",
    val category: String = "",
    val description: String = "",
    val receiptUrl: String? = null,
    val date: Long = 0,
    val account: String = "",
    val userId: String = "",
    val groupId: String? = null
)