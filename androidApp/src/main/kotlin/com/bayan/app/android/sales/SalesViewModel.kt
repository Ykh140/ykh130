package com.bayan.app.android.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.android.products.DEFAULT_BUSINESS_ID
import com.bayan.app.domain.model.InvoiceLine
import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.model.PaymentMethod
import com.bayan.app.domain.model.Product
import com.bayan.app.domain.repository.PartyRepository
import com.bayan.app.domain.repository.ProductRepository
import com.bayan.app.domain.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartLine(val product: Product, val quantity: Double) {
    val lineTotal: Double get() = quantity * product.salePrice
}

class SalesViewModel(
    private val productRepository: ProductRepository,
    private val partyRepository: PartyRepository,
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _customers = MutableStateFlow<List<Party>>(emptyList())
    val customers: StateFlow<List<Party>> = _customers.asStateFlow()

    private val _cart = MutableStateFlow<List<CartLine>>(emptyList())
    val cart: StateFlow<List<CartLine>> = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Party?>(null)
    val selectedCustomer: StateFlow<Party?> = _selectedCustomer.asStateFlow()

    private val _lastInvoiceSaved = MutableStateFlow(false)
    val lastInvoiceSaved: StateFlow<Boolean> = _lastInvoiceSaved.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.observeProducts(DEFAULT_BUSINESS_ID).collect { _products.value = it }
        }
        viewModelScope.launch {
            partyRepository.observeParties(DEFAULT_BUSINESS_ID, PartyType.CUSTOMER).collect { _customers.value = it }
        }
    }

    fun selectCustomer(party: Party?) {
        _selectedCustomer.value = party
    }

    fun addToCart(product: Product) {
        val current = _cart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(CartLine(product, 1.0))
        }
        _cart.value = current
    }

    fun removeFromCart(productId: String) {
        _cart.value = _cart.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _selectedCustomer.value = null
    }

    val cartTotal: Double get() = _cart.value.sumOf { it.lineTotal }

    fun confirmSale(paymentMethod: PaymentMethod) {
        val lines = _cart.value
        if (lines.isEmpty()) return

        viewModelScope.launch {
            val invoiceLines = lines.map {
                InvoiceLine(
                    productId = it.product.id,
                    productName = it.product.name,
                    quantity = it.quantity,
                    unitPrice = it.product.salePrice,
                    unitCost = it.product.purchasePrice
                )
            }
            salesRepository.createInvoice(
                businessId = DEFAULT_BUSINESS_ID,
                customerId = _selectedCustomer.value?.id,
                items = invoiceLines,
                paymentMethod = paymentMethod
            )
            clearCart()
            _lastInvoiceSaved.value = true
        }
    }

    fun consumeSavedFlag() {
        _lastInvoiceSaved.value = false
    }
}
