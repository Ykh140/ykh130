package com.bayan.app.domain.model

data class Expense(
    val id: String,
    val businessId: String,
    val amount: Double,
    val category: String?,
    val note: String?,
    val createdAt: Long
)
