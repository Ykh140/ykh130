package com.bayan.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bayan.app.db.BayanDatabase
import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.model.StatementEntry
import com.bayan.app.domain.model.StatementEntryType
import com.bayan.app.domain.repository.PartyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.bayan.app.db.Customer as PartyEntity

class PartyRepositoryImpl(
    private val db: BayanDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : PartyRepository {

    private val queries = db.customerQueries

    override fun observeParties(businessId: String, type: PartyType): Flow<List<Party>> {
        return queries.selectAllByBusinessAndType(businessId, type.toDbValue())
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Party? = withContext(ioDispatcher) {
        queries.selectCustomerById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun search(businessId: String, type: PartyType, query: String): List<Party> =
        withContext(ioDispatcher) {
            queries.searchCustomers(businessId, type.toDbValue(), query).executeAsList().map { it.toDomain() }
        }

    override suspend fun addParty(party: Party) = withContext(ioDispatcher) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertCustomer(
            id = party.id,
            businessId = party.businessId,
            name = party.name,
            phone = party.phone,
            type = party.type.toDbValue(),
            notes = party.notes,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun updateParty(party: Party) = withContext(ioDispatcher) {
        queries.updateCustomer(
            name = party.name,
            phone = party.phone,
            notes = party.notes,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = party.id
        )
    }

    override suspend fun adjustBalance(partyId: String, delta: Double) = withContext(ioDispatcher) {
        queries.adjustBalance(
            balance = delta,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = partyId
        )
    }

    override suspend fun deleteParty(partyId: String) = withContext(ioDispatcher) {
        queries.softDeleteCustomer(
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = partyId
        )
    }

    override suspend fun getTotalCustomerDebt(businessId: String): Double = withContext(ioDispatcher) {
        queries.selectTotalDebt(businessId).executeAsOne()
    }

    override fun observeStatement(partyId: String): Flow<List<StatementEntry>> {
        val invoiceEntries = db.invoiceQueries.selectInvoicesByCustomer(partyId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { invoices ->
                invoices.map { inv ->
                    val isDebt = inv.paymentMethod == "debt"
                    StatementEntry(
                        id = inv.id,
                        type = if (isDebt) StatementEntryType.SALE_DEBT else StatementEntryType.SALE_CASH,
                        description = if (isDebt) "بيع بالدين" else "بيع (${inv.paymentMethod.methodLabel()})",
                        amount = inv.totalAmount,
                        balanceDelta = if (isDebt) -inv.totalAmount else 0.0,
                        createdAt = inv.createdAt
                    )
                }
            }

        val paymentEntries = db.paymentQueries.selectPaymentsByParty(partyId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { payments ->
                payments.map { p ->
                    StatementEntry(
                        id = p.id,
                        type = if (p.balanceDelta > 0) StatementEntryType.PAYMENT_IN else StatementEntryType.PAYMENT_OUT,
                        description = p.note?.takeIf { it.isNotBlank() }
                            ?: if (p.balanceDelta > 0) "تسديد" else "دفعة/سلفة",
                        amount = p.amount,
                        balanceDelta = p.balanceDelta,
                        createdAt = p.createdAt
                    )
                }
            }

        return combine(invoiceEntries, paymentEntries) { invoices, payments ->
            (invoices + payments).sortedByDescending { it.createdAt }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun recordPayment(
        businessId: String,
        partyId: String,
        amount: Double,
        isCredit: Boolean,
        note: String?
    ) = withContext(ioDispatcher) {
        val now = Clock.System.now().toEpochMilliseconds()
        val delta = if (isCredit) amount else -amount
        db.transaction {
            db.paymentQueries.insertPayment(
                id = Uuid.random().toString(),
                businessId = businessId,
                partyId = partyId,
                amount = amount,
                balanceDelta = delta,
                note = note,
                createdAt = now
            )
            queries.adjustBalance(
                balance = delta,
                updatedAt = now,
                id = partyId
            )
        }
    }
}

private fun PartyType.toDbValue(): String = when (this) {
    PartyType.CUSTOMER -> "customer"
    PartyType.SUPPLIER -> "supplier"
}

private fun String.methodLabel(): String = when (this) {
    "transfer" -> "تحويل"
    "debt" -> "دين"
    else -> "نقدًا"
}

private fun PartyEntity.toDomain() = Party(
    id = id,
    businessId = businessId,
    name = name,
    phone = phone,
    type = if (type == "supplier") PartyType.SUPPLIER else PartyType.CUSTOMER,
    balance = balance,
    notes = notes
)
