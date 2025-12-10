package com.example.cashflowreportapp.model

import com.google.firebase.firestore.Exclude

data class SavingsGroup(
    @get:Exclude var id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = listOf(),
    val createdAt: Long = System.currentTimeMillis()
)