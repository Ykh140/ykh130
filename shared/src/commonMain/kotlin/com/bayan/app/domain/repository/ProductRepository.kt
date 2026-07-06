package com.bayan.app.domain.repository

import com.bayan.app.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(businessId: String): Flow<List<Product>>
    suspend fun getById(id: String): Product?
    suspend fun getByBarcode(businessId: String, barcode: String): Product?
    suspend fun search(businessId: String, query: String): List<Product>
    suspend fun getLowStock(businessId: String): List<Product>
    suspend fun addProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun adjustQuantity(productId: String, newQuantity: Double)
    suspend fun deleteProduct(productId: String)
}
