package com.bayan.app.domain.repository

import com.bayan.app.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(businessId: String): Flow<List<Expense>>
    suspend fun addExpense(businessId: String, amount: Double, category: String?, note: String?)
    suspend fun getTodayExpensesTotal(businessId: String, startOfDayMillis: Long): Double
}
