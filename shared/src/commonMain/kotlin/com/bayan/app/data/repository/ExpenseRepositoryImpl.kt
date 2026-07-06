package com.bayan.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bayan.app.db.BayanDatabase
import com.bayan.app.domain.model.Expense
import com.bayan.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ExpenseRepositoryImpl(
    private val db: BayanDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ExpenseRepository {

    private val queries = db.expenseQueries

    override fun observeExpenses(businessId: String): Flow<List<Expense>> {
        return queries.selectExpensesByBusiness(businessId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addExpense(businessId: String, amount: Double, category: String?, note: String?) =
        withContext(ioDispatcher) {
            queries.insertExpense(
                id = Uuid.random().toString(),
                businessId = businessId,
                amount = amount,
                category = category,
                note = note,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
        }

    override suspend fun getTodayExpensesTotal(businessId: String, startOfDayMillis: Long): Double =
        withContext(ioDispatcher) {
            queries.sumExpensesToday(businessId, startOfDayMillis).executeAsOne()
        }
}

private fun com.bayan.app.db.Expense.toDomain() = Expense(
    id = id,
    businessId = businessId,
    amount = amount,
    category = category,
    note = note,
    createdAt = createdAt
)
