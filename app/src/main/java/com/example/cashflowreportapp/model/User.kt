package com.example.cashflowreportapp.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "MEMBER",
    val familyGroupId: String? = null
)