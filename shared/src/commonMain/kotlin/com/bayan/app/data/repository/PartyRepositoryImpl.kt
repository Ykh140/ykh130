package com.bayan.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bayan.app.db.BayanDatabase
import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.repository.PartyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
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
}

private fun PartyType.toDbValue(): String = when (this) {
    PartyType.CUSTOMER -> "customer"
    PartyType.SUPPLIER -> "supplier"
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
