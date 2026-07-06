package com.bayan.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bayan.app.db.BayanDatabase
import com.bayan.app.domain.model.Product
import com.bayan.app.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import com.bayan.app.db.Product as ProductEntity

class ProductRepositoryImpl(
    private val db: BayanDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ProductRepository {

    private val queries = db.productQueries

    override fun observeProducts(businessId: String): Flow<List<Product>> {
        return queries.selectAllByBusiness(businessId)
            .asFlow()
            .mapToList(ioDispatcher)
            .let { flow -> kotlinx.coroutines.flow.map(flow) { list -> list.map { it.toDomain() } } }
    }

    override suspend fun getById(id: String): Product? = withContext(ioDispatcher) {
        queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getByBarcode(businessId: String, barcode: String): Product? =
        withContext(ioDispatcher) {
            queries.selectByBarcode(businessId, barcode).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun search(businessId: String, query: String): List<Product> =
        withContext(ioDispatcher) {
            queries.searchByName(businessId, query).executeAsList().map { it.toDomain() }
        }

    override suspend fun getLowStock(businessId: String): List<Product> =
        withContext(ioDispatcher) {
            queries.selectLowStock(businessId).executeAsList().map { it.toDomain() }
        }

    override suspend fun addProduct(product: Product) = withContext(ioDispatcher) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertProduct(
            id = product.id,
            businessId = product.businessId,
            name = product.name,
            barcode = product.barcode,
            internalCode = product.internalCode,
            category = product.category,
            unit = product.unit,
            purchasePrice = product.purchasePrice,
            salePrice = product.salePrice,
            quantity = product.quantity,
            lowStockThreshold = product.lowStockThreshold,
            imagePath = product.imagePath,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun updateProduct(product: Product) = withContext(ioDispatcher) {
        queries.updateProduct(
            name = product.name,
            barcode = product.barcode,
            internalCode = product.internalCode,
            category = product.category,
            unit = product.unit,
            purchasePrice = product.purchasePrice,
            salePrice = product.salePrice,
            quantity = product.quantity,
            lowStockThreshold = product.lowStockThreshold,
            imagePath = product.imagePath,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = product.id
        )
    }

    override suspend fun adjustQuantity(productId: String, newQuantity: Double) =
        withContext(ioDispatcher) {
            queries.updateQuantity(
                quantity = newQuantity,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                id = productId
            )
        }

    override suspend fun deleteProduct(productId: String) = withContext(ioDispatcher) {
        queries.softDeleteProduct(
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = productId
        )
    }
}

private fun ProductEntity.toDomain() = Product(
    id = id,
    businessId = businessId,
    name = name,
    barcode = barcode,
    internalCode = internalCode,
    category = category,
    unit = unit,
    purchasePrice = purchasePrice,
    salePrice = salePrice,
    quantity = quantity,
    lowStockThreshold = lowStockThreshold,
    imagePath = imagePath
)
