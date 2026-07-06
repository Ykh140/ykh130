package com.bayan.app.domain.repository

import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun observeParties(businessId: String, type: PartyType): Flow<List<Party>>
    suspend fun getById(id: String): Party?
    suspend fun search(businessId: String, type: PartyType, query: String): List<Party>
    suspend fun addParty(party: Party)
    suspend fun updateParty(party: Party)
    suspend fun adjustBalance(partyId: String, delta: Double)
    suspend fun deleteParty(partyId: String)
}
