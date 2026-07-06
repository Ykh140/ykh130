package com.bayan.app.domain.repository

import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.model.StatementEntry
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun observeParties(businessId: String, type: PartyType): Flow<List<Party>>
    suspend fun getById(id: String): Party?
    suspend fun search(businessId: String, type: PartyType, query: String): List<Party>
    suspend fun addParty(party: Party)
    suspend fun updateParty(party: Party)
    suspend fun adjustBalance(partyId: String, delta: Double)
    suspend fun deleteParty(partyId: String)
    suspend fun getTotalCustomerDebt(businessId: String): Double

    /** كشف حساب كامل للطرف: كل فواتيره + كل الدفعات اليدوية، مرتبة من الأحدث للأقدم */
    fun observeStatement(partyId: String): Flow<List<StatementEntry>>

    /**
     * تسجيل دفعة يدوية على حساب الطرف (تسديد دين أو دفعة/سلفة جديدة) وتحديث رصيده تلقائيًا
     * بعملية واحدة (transaction).
     * isCredit = true  → تسديد من الطرف (يحسّن رصيده، مثل عميل يسدد دينه)
     * isCredit = false → دفعة/سلفة جديدة له (تقلل رصيده)
     */
    suspend fun recordPayment(
        businessId: String,
        partyId: String,
        amount: Double,
        isCredit: Boolean,
        note: String?
    )
}
