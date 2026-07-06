package com.bayan.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bayan.app.data.sync.SyncTables
import com.bayan.app.data.sync.enqueueSync
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
            val id = Uuid.random().toString()
            val now = Clock.System.now().toEpochMilliseconds()
            db.transaction {
                queries.insertExpense(
                    id = id,
                    businessId = businessId,
                    amount = amount,
                    category = category,
                    note = note,
                    createdAt = now,
                    updatedAt = now
                )
                db.enqueueSync(SyncTables.EXPENSE, id, now)
            }
        }

    override suspend fun getTodayExpensesTotal(businessId: String, startOfDayMillis: Long): Double =
        withContext(ioDispatcher) {
            queries.sumExpensesToday(businessId, startOfDayMillis).executeAsOne()
        }

    override suspend fun deleteExpense(expenseId: String) = withContext(ioDispatcher) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            queries.softDeleteExpense(updatedAt = now, id = expenseId)
            db.enqueueSync(SyncTables.EXPENSE, expenseId, now)
        }
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
