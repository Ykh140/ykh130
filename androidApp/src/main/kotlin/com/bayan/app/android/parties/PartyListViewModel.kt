package com.bayan.app.android.parties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.model.StatementEntry
import com.bayan.app.domain.repository.PartyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PartyListViewModel(
    private val repository: PartyRepository,
    private val type: PartyType,
    private val businessId: String
) : ViewModel() {

    private val _parties = MutableStateFlow<List<Party>>(emptyList())
    val parties: StateFlow<List<Party>> = _parties.asStateFlow()

    // الطرف المفتوح حاليًا لعرض كشف حسابه (null = قائمة العملاء/الموردين)
    private val _selectedParty = MutableStateFlow<Party?>(null)
    val selectedParty: StateFlow<Party?> = _selectedParty.asStateFlow()

    private val _statement = MutableStateFlow<List<StatementEntry>>(emptyList())
    val statement: StateFlow<List<StatementEntry>> = _statement.asStateFlow()

    private var statementJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeParties(businessId, type).collect { list ->
                _parties.value = list
                // إبقاء الطرف المفتوح محدّثًا (مثلاً بعد تغيّر رصيده)
                _selectedParty.value?.let { current ->
                    _selectedParty.value = list.find { it.id == current.id }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addParty(name: String, phone: String?, notes: String?) {
        viewModelScope.launch {
            repository.addParty(
                Party(
                    id = Uuid.random().toString(),
                    businessId = businessId,
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

    fun openParty(party: Party) {
        _selectedParty.value = party
        statementJob?.cancel()
        statementJob = viewModelScope.launch {
            repository.observeStatement(party.id).collect { entries ->
                _statement.value = entries
            }
        }
    }

    fun closeParty() {
        statementJob?.cancel()
        statementJob = null
        _selectedParty.value = null
        _statement.value = emptyList()
    }

    fun recordPayment(amount: Double, isCredit: Boolean, note: String?) {
        val partyId = _selectedParty.value?.id ?: return
        viewModelScope.launch {
            repository.recordPayment(businessId, partyId, amount, isCredit, note)
        }
    }
}
