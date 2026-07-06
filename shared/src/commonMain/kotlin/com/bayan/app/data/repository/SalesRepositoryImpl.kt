package com.bayan.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bayan.app.data.sync.SyncTables
import com.bayan.app.data.sync.enqueueSync
import com.bayan.app.db.BayanDatabase
import com.bayan.app.domain.model.Invoice
import com.bayan.app.domain.model.InvoiceLine
import com.bayan.app.domain.model.PaymentMethod
import com.bayan.app.domain.repository.SalesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SalesRepositoryImpl(
    private val db: BayanDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : SalesRepository {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createInvoice(
        businessId: String,
        customerId: String?,
        items: List<InvoiceLine>,
        paymentMethod: PaymentMethod
    ): String = withContext(ioDispatcher) {
        val invoiceId = Uuid.random().toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val total = items.sumOf { it.quantity * it.unitPrice }

        // كل هذا يحصل داخل عملية واحدة: إما ينجح بالكامل أو يفشل بالكامل (لا فاتورة بدون تحديث مخزون)
        db.transaction {
            db.invoiceQueries.insertInvoice(
                id = invoiceId,
                businessId = businessId,
                customerId = customerId,
                totalAmount = total,
                paymentMethod = paymentMethod.toDbValue(),
                createdAt = now,
                updatedAt = now
            )
            db.enqueueSync(SyncTables.INVOICE, invoiceId, now)

            items.forEach { line ->
                db.invoiceQueries.insertInvoiceItem(
                    id = Uuid.random().toString(),
                    invoiceId = invoiceId,
                    productId = line.productId,
                    productName = line.productName,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    unitCost = line.unitCost
                )

                // إنقاص الكمية تلقائيًا
                val currentProduct = db.productQueries.selectById(line.productId).executeAsOneOrNull()
                if (currentProduct != null) {
                    db.productQueries.updateQuantity(
                        quantity = currentProduct.quantity - line.quantity,
                        updatedAt = now,
                        id = line.productId
                    )
                    db.enqueueSync(SyncTables.PRODUCT, line.productId, now)
                }
            }

            // تحديث دين العميل تلقائيًا إذا كان البيع بالدين
            if (paymentMethod == PaymentMethod.DEBT && customerId != null) {
                db.customerQueries.adjustBalance(
                    balance = -total,
                    updatedAt = now,
                    id = customerId
                )
                db.enqueueSync(SyncTables.CUSTOMER, customerId, now)
            }
        }

        invoiceId
    }

    override fun observeInvoices(businessId: String): Flow<List<Invoice>> {
        return db.invoiceQueries.selectInvoicesByBusiness(businessId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { invoices ->
                invoices.map { inv ->
                    val items = db.invoiceQueries.selectInvoiceItems(inv.id).executeAsList()
                    val customerName = inv.customerId?.let {
                        db.customerQueries.selectCustomerById(it).executeAsOneOrNull()?.name
                    }
                    Invoice(
                        id = inv.id,
                        businessId = inv.businessId,
                        customerId = inv.customerId,
                        customerName = customerName,
                        items = items.map { it.toDomain() },
                        paymentMethod = inv.paymentMethod.toPaymentMethod(),
                        createdAt = inv.createdAt
                    )
                }
            }
    }

    override suspend fun getInvoiceWithItems(invoiceId: String): Invoice? = withContext(ioDispatcher) {
        val inv = db.invoiceQueries.selectInvoiceById(invoiceId).executeAsOneOrNull() ?: return@withContext null
        val items = db.invoiceQueries.selectInvoiceItems(invoiceId).executeAsList()
        val customerName = inv.customerId?.let {
            db.customerQueries.selectCustomerById(it).executeAsOneOrNull()?.name
        }
        Invoice(
            id = inv.id,
            businessId = inv.businessId,
            customerId = inv.customerId,
            customerName = customerName,
            items = items.map { it.toDomain() },
            paymentMethod = inv.paymentMethod.toPaymentMethod(),
            createdAt = inv.createdAt
        )
    }

    override suspend fun getTodaySalesTotal(businessId: String, startOfDayMillis: Long): Double =
        withContext(ioDispatcher) {
            db.invoiceQueries.sumSalesToday(businessId, startOfDayMillis).executeAsOne()
        }

    override suspend fun getTodayProfitTotal(businessId: String, startOfDayMillis: Long): Double =
        withContext(ioDispatcher) {
            db.invoiceQueries.sumProfitToday(businessId, startOfDayMillis).executeAsOne()
        }
}

private fun PaymentMethod.toDbValue(): String = when (this) {
    PaymentMethod.CASH -> "cash"
    PaymentMethod.TRANSFER -> "transfer"
    PaymentMethod.DEBT -> "debt"
}

private fun String.toPaymentMethod(): PaymentMethod = when (this) {
    "transfer" -> PaymentMethod.TRANSFER
    "debt" -> PaymentMethod.DEBT
    else -> PaymentMethod.CASH
}

private fun com.bayan.app.db.InvoiceItem.toDomain() = InvoiceLine(
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    unitCost = unitCost
)
