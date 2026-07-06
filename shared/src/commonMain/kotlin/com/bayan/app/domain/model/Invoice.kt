package com.bayan.app.domain.model

enum class PaymentMethod {
    CASH, TRANSFER, DEBT
}

data class InvoiceLine(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val unitCost: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
    val lineProfit: Double get() = quantity * (unitPrice - unitCost)
}

data class Invoice(
    val id: String,
    val businessId: String,
    val customerId: String?,
    val customerName: String?,
    val items: List<InvoiceLine>,
    val paymentMethod: PaymentMethod,
    val createdAt: Long
) {
    val totalAmount: Double get() = items.sumOf { it.lineTotal }
    val totalProfit: Double get() = items.sumOf { it.lineProfit }
}
