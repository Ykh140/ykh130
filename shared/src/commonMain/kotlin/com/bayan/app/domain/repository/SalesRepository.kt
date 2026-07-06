package com.bayan.app.domain.repository

import com.bayan.app.domain.model.Invoice
import com.bayan.app.domain.model.InvoiceLine
import com.bayan.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface SalesRepository {
    /**
     * ينشئ فاتورة بيع كاملة داخل عملية واحدة (transaction):
     * - يسجل الفاتورة وبنودها
     * - ينقص كمية كل منتج تلقائيًا
     * - إذا كانت طريقة الدفع "دين" ووجد عميل، يزيد دين العميل تلقائيًا
     */
    suspend fun createInvoice(
        businessId: String,
        customerId: String?,
        items: List<InvoiceLine>,
        paymentMethod: PaymentMethod
    ): String // يرجع invoiceId

    fun observeInvoices(businessId: String): Flow<List<Invoice>>
    suspend fun getInvoiceWithItems(invoiceId: String): Invoice?
    suspend fun getTodaySalesTotal(businessId: String, startOfDayMillis: Long): Double
}
