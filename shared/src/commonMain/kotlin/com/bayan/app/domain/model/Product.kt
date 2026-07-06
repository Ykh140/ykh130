package com.bayan.app.domain.model

/**
 * نموذج المنتج في طبقة الـ domain (مستقل عن قاعدة البيانات)
 */
data class Product(
    val id: String,
    val businessId: String,
    val name: String,
    val barcode: String?,
    val internalCode: String?,
    val category: String?,
    val unit: String,
    val purchasePrice: Double,
    val salePrice: Double,
    val quantity: Double,
    val lowStockThreshold: Double,
    val imagePath: String?
) {
    val isLowStock: Boolean
        get() = quantity <= lowStockThreshold

    val profitPerUnit: Double
        get() = salePrice - purchasePrice
}
