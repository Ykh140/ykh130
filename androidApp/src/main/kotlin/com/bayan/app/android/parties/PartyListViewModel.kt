package com.bayan.app.android.parties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.android.products.DEFAULT_BUSINESS_ID
import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.repository.PartyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PartyListViewModel(
    private val repository: PartyRepository,
    private val type: PartyType
) : ViewModel() {

    private val _parties = MutableStateFlow<List<Party>>(emptyList())
    val parties: StateFlow<List<Party>> = _parties.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeParties(DEFAULT_BUSINESS_ID, type).collect { list ->
                _parties.value = list
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addParty(name: String, phone: String?, notes: String?) {
        viewModelScope.launch {
            repository.addParty(
                Party(
                    id = Uuid.random().toString(),
                    businessId = DEFAULT_BUSINESS_ID,
                    name = name,
                    phone = phone,
                    type = type,
                    balance = 0.0,
                    notes = notes
                )
            )
        }
    }

    fun adjustBalance(partyId: String, delta: Double) {
        viewModelScope.launch {
            repository.adjustBalance(partyId, delta)
        }
    }

    fun deleteParty(partyId: String) {
        viewModelScope.launch {
            repository.deleteParty(partyId)
        }
    }
}
