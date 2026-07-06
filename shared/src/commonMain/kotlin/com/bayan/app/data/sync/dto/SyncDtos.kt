package com.bayan.app.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * نماذج نقل البيانات (DTO) لكل جدول قابل للمزامنة، بأسماء أعمدة snake_case
 * تطابق تمامًا مخطط Supabase (supabase/schema.sql). تُستخدم فقط لطبقة
 * المزامنة (الرفع والسحب)؛ الشاشات والمستودعات تتعامل دائمًا مع نماذج
 * الدومين المحلية (Product, Party...) وجداول SQLDelight.
 */

@Serializable
data class ProductDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    val name: String,
    val barcode: String? = null,
    @SerialName("internal_code") val internalCode: String? = null,
    val category: String? = null,
    val unit: String,
    @SerialName("purchase_price") val purchasePrice: Double,
    @SerialName("sale_price") val salePrice: Double,
    val quantity: Double,
    @SerialName("low_stock_threshold") val lowStockThreshold: Double,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CustomerDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    val name: String,
    val phone: String? = null,
    val type: String,
    val balance: Double,
    val notes: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class InvoiceDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class InvoiceItemDto(
    val id: String,
    @SerialName("invoice_id") val invoiceId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("unit_cost") val unitCost: Double
)

@Serializable
data class PaymentDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("party_id") val partyId: String,
    val amount: Double,
    @SerialName("balance_delta") val balanceDelta: Double,
    val note: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class ExpenseDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    val amount: Double,
    val category: String? = null,
    val note: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)
