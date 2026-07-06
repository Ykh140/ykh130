package com.bayan.app.android.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.android.products.DEFAULT_BUSINESS_ID
import com.bayan.app.domain.model.Invoice
import com.bayan.app.domain.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InvoiceListViewModel(
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    // الفاتورة المفتوحة حاليًا لعرض تفاصيلها (null = قائمة الفواتير)
    private val _selectedInvoiceId = MutableStateFlow<String?>(null)
    val selectedInvoiceId: StateFlow<String?> = _selectedInvoiceId.asStateFlow()

    init {
        viewModelScope.launch {
            salesRepository.observeInvoices(DEFAULT_BUSINESS_ID).collect { list ->
                _invoices.value = list
            }
        }
    }

    fun openInvoice(invoiceId: String) {
        _selectedInvoiceId.value = invoiceId
    }

    fun closeInvoice() {
        _selectedInvoiceId.value = null
    }
}
