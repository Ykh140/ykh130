package com.bayan.app.android.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.domain.model.Product
import com.bayan.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Business ثابت مؤقتًا للـ MVP (نشاط تجاري واحد فقط حاليًا)
const val DEFAULT_BUSINESS_ID = "default-business"

class ProductListViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeProducts(DEFAULT_BUSINESS_ID).collect { list ->
                _products.value = list
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addProduct(
        name: String,
        purchasePrice: Double,
        salePrice: Double,
        quantity: Double,
        lowStockThreshold: Double,
        unit: String,
        barcode: String?
    ) {
        viewModelScope.launch {
            repository.addProduct(
                Product(
                    id = Uuid.random().toString(),
                    businessId = DEFAULT_BUSINESS_ID,
                    name = name,
                    barcode = barcode,
                    internalCode = null,
                    category = null,
                    unit = unit,
                    purchasePrice = purchasePrice,
                    salePrice = salePrice,
                    quantity = quantity,
                    lowStockThreshold = lowStockThreshold,
                    imagePath = null
                )
            )
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }
}
