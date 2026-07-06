package com.bayan.app.data.sync

import com.bayan.app.data.remote.supabaseClient
import com.bayan.app.data.sync.dto.CustomerDto
import com.bayan.app.data.sync.dto.ExpenseDto
import com.bayan.app.data.sync.dto.InvoiceDto
import com.bayan.app.data.sync.dto.InvoiceItemDto
import com.bayan.app.data.sync.dto.PaymentDto
import com.bayan.app.data.sync.dto.ProductDto
import com.bayan.app.db.BayanDatabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * طبقة المزامنة بين قاعدة البيانات المحلية (SQLDelight، مصدر الحقيقة دائمًا) و Supabase.
 *
 * المبدأ:
 *  - كل تعديل محلي يُسجَّل بـ SyncQueue (outbox) وقت حدوثه (من داخل نفس transaction).
 *  - push(): يقرأ قائمة الانتظار، يرفع كل صف كامل (upsert) لـ Supabase، ثم يحذفه من القائمة.
 *  - pull(): يجيب من Supabase كل الصفوف المعدّلة بعد آخر سحب ناجح (updated_at > lastPulledAt)،
 *    ويطبّق قاعدة "آخر تعديل يفوز" (Last-Write-Wins بالاعتماد على updated_at) عند الدمج محليًا.
 *  - نبضة (ping) خفيفة عند كل مزامنة ناجحة، لمنع توقف مشروع Supabase المجاني بعد 7 أيام
 *    بدون طلبات.
 *
 * ملاحظة: هذا المحرك لا يحل تعارضات على مستوى الحقل الواحد، فقط على مستوى الصف كاملاً
 * (الصف الأحدث بالكامل يفوز) — كافٍ لمرحلة الـMVP التي فيها مستخدم واحد بأجهزة متعددة.
 */
class SyncEngine(
    private val db: BayanDatabase,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var syncJob: Job? = null
    private var watchJob: Job? = null

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** يبدأ مراقبة الشبكة: أي رجوع للاتصال يشغّل مزامنة تلقائيًا. يُستدعى مرة واحدة بعد تسجيل الدخول */
    fun start(businessId: String) {
        watchJob?.cancel()
        watchJob = scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    syncNow(businessId)
                } else {
                    _status.value = SyncStatus.Offline
                }
            }
        }
    }

    fun stop() {
        watchJob?.cancel()
        syncJob?.cancel()
    }

    /** يشغّل مزامنة فورية (رفع ثم سحب). لا يشغّل أكثر من مزامنة بنفس الوقت */
    fun syncNow(businessId: String) {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            if (!networkMonitor.isCurrentlyOnline()) {
                _status.value = SyncStatus.Offline
                return@launch
            }
            _status.value = SyncStatus.Syncing
            try {
                push()
                pull(businessId)
                pingKeepAlive()
                _status.value = SyncStatus.Idle
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(e.message ?: "تعذّرت المزامنة")
            }
        }
    }

    // ------------------------------------------------------------------
    // الرفع (Push): محلي → Supabase
    // ------------------------------------------------------------------

    private suspend fun push() {
        val pending = db.syncQueueQueries.selectAllPending().executeAsList()
        for (entry in pending) {
            val success = try {
                pushOne(entry.tableName, entry.entityId)
                true
            } catch (_: Exception) {
                // نسيب الصف بقائمة الانتظار لإعادة المحاولة بالمزامنة الجاية، ونكمل على باقي الصفوف
                false
            }
            if (success) {
                db.syncQueueQueries.removeFromQueue(entry.tableName, entry.entityId)
            }
        }
    }

    private suspend fun pushOne(tableName: String, entityId: String) {
        when (tableName) {
            SyncTables.PRODUCT -> {
                val row = db.productQueries.selectById(entityId).executeAsOneOrNull() ?: return
                supabaseClient.from(SyncTables.PRODUCT).upsert(
                    ProductDto(
                        id = row.id,
                        businessId = row.businessId,
                        name = row.name,
                        barcode = row.barcode,
                        internalCode = row.internalCode,
                        category = row.category,
                        unit = row.unit,
                        purchasePrice = row.purchasePrice,
                        salePrice = row.salePrice,
                        quantity = row.quantity,
                        lowStockThreshold = row.lowStockThreshold,
                        imagePath = row.imagePath,
                        isDeleted = row.isDeleted == 1L,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt
                    )
                )
            }

            SyncTables.CUSTOMER -> {
                val row = db.customerQueries.selectCustomerById(entityId).executeAsOneOrNull() ?: return
                supabaseClient.from(SyncTables.CUSTOMER).upsert(
                    CustomerDto(
                        id = row.id,
                        businessId = row.businessId,
                        name = row.name,
                        phone = row.phone,
                        type = row.type,
                        balance = row.balance,
                        notes = row.notes,
                        isDeleted = row.isDeleted == 1L,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt
                    )
                )
            }

            SyncTables.INVOICE -> {
                val row = db.invoiceQueries.selectInvoiceById(entityId).executeAsOneOrNull() ?: return
                supabaseClient.from(SyncTables.INVOICE).upsert(
                    InvoiceDto(
                        id = row.id,
                        businessId = row.businessId,
                        customerId = row.customerId,
                        totalAmount = row.totalAmount,
                        paymentMethod = row.paymentMethod,
                        isDeleted = row.isDeleted == 1L,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt
                    )
                )
                // بنود الفاتورة ثابتة بعد إنشائها؛ نرفعها معها أول مرة فقط
                val items = db.invoiceQueries.selectInvoiceItems(entityId).executeAsList()
                if (items.isNotEmpty()) {
                    supabaseClient.from("invoice_item").upsert(
                        items.map { item ->
                            InvoiceItemDto(
                                id = item.id,
                                invoiceId = item.invoiceId,
                                productId = item.productId,
                                productName = item.productName,
                                quantity = item.quantity,
                                unitPrice = item.unitPrice,
                                unitCost = item.unitCost
                            )
                        }
                    )
                }
            }

            SyncTables.PAYMENT -> {
                val payment = db.paymentQueries.selectPaymentById(entityId).executeAsOneOrNull() ?: return
                supabaseClient.from(SyncTables.PAYMENT).upsert(
                    PaymentDto(
                        id = payment.id,
                        businessId = payment.businessId,
                        partyId = payment.partyId,
                        amount = payment.amount,
                        balanceDelta = payment.balanceDelta,
                        note = payment.note,
                        createdAt = payment.createdAt,
                        updatedAt = payment.updatedAt
                    )
                )
            }

            SyncTables.EXPENSE -> {
                val row = db.expenseQueries.selectExpenseById(entityId).executeAsOneOrNull() ?: return
                supabaseClient.from(SyncTables.EXPENSE).upsert(
                    ExpenseDto(
                        id = row.id,
                        businessId = row.businessId,
                        amount = row.amount,
                        category = row.category,
                        note = row.note,
                        isDeleted = row.isDeleted == 1L,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // السحب (Pull): Supabase → محلي، بقاعدة "آخر تعديل يفوز"
    // ------------------------------------------------------------------

    private suspend fun pull(businessId: String) {
        pullProducts(businessId)
        pullCustomers(businessId)
        pullInvoicesAndItems(businessId)
        pullPayments(businessId)
        pullExpenses(businessId)
    }

    private suspend fun pullProducts(businessId: String) {
        val since = getLastPulled(SyncTables.PRODUCT)
        val remoteRows = supabaseClient.from(SyncTables.PRODUCT)
            .select {
                filter {
                    eq("business_id", businessId)
                    gt("updated_at", since)
                }
            }
            .decodeList<ProductDto>()

        var maxUpdatedAt = since
        db.transaction {
            for (r in remoteRows) {
                val local = db.productQueries.selectById(r.id).executeAsOneOrNull()
                if (local == null || r.updatedAt > local.updatedAt) {
                    db.productQueries.upsertFromRemote(
                        id = r.id,
                        businessId = r.businessId,
                        name = r.name,
                        barcode = r.barcode,
                        internalCode = r.internalCode,
                        category = r.category,
                        unit = r.unit,
                        purchasePrice = r.purchasePrice,
                        salePrice = r.salePrice,
                        quantity = r.quantity,
                        lowStockThreshold = r.lowStockThreshold,
                        imagePath = r.imagePath,
                        isDeleted = if (r.isDeleted) 1L else 0L,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt
                    )
                }
                if (r.updatedAt > maxUpdatedAt) maxUpdatedAt = r.updatedAt
            }
        }
        setLastPulled(SyncTables.PRODUCT, maxUpdatedAt)
    }

    private suspend fun pullCustomers(businessId: String) {
        val since = getLastPulled(SyncTables.CUSTOMER)
        val remoteRows = supabaseClient.from(SyncTables.CUSTOMER)
            .select {
                filter {
                    eq("business_id", businessId)
                    gt("updated_at", since)
                }
            }
            .decodeList<CustomerDto>()

        var maxUpdatedAt = since
        db.transaction {
            for (r in remoteRows) {
                val local = db.customerQueries.selectCustomerById(r.id).executeAsOneOrNull()
                if (local == null || r.updatedAt > local.updatedAt) {
                    db.customerQueries.upsertFromRemote(
                        id = r.id,
                        businessId = r.businessId,
                        name = r.name,
                        phone = r.phone,
                        type = r.type,
                        balance = r.balance,
                        notes = r.notes,
                        isDeleted = if (r.isDeleted) 1L else 0L,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt
                    )
                }
                if (r.updatedAt > maxUpdatedAt) maxUpdatedAt = r.updatedAt
            }
        }
        setLastPulled(SyncTables.CUSTOMER, maxUpdatedAt)
    }

    private suspend fun pullInvoicesAndItems(businessId: String) {
        val since = getLastPulled(SyncTables.INVOICE)
        val remoteInvoices = supabaseClient.from(SyncTables.INVOICE)
            .select {
                filter {
                    eq("business_id", businessId)
                    gt("updated_at", since)
                }
            }
            .decodeList<InvoiceDto>()

        var maxUpdatedAt = since
        val newlyPulledIds = mutableListOf<String>()

        db.transaction {
            for (r in remoteInvoices) {
                val local = db.invoiceQueries.selectInvoiceById(r.id).executeAsOneOrNull()
                if (local == null || r.updatedAt > local.updatedAt) {
                    db.invoiceQueries.upsertInvoiceFromRemote(
                        id = r.id,
                        businessId = r.businessId,
                        customerId = r.customerId,
                        totalAmount = r.totalAmount,
                        paymentMethod = r.paymentMethod,
                        isDeleted = if (r.isDeleted) 1L else 0L,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt
                    )
                }
                if (local == null) newlyPulledIds += r.id
                if (r.updatedAt > maxUpdatedAt) maxUpdatedAt = r.updatedAt
            }
        }

        // بنود الفواتير الجديدة فقط (البنود ثابتة، ما تحتاج مقارنة updatedAt)
        for (invoiceId in newlyPulledIds) {
            val remoteItems = supabaseClient.from("invoice_item")
                .select {
                    filter { eq("invoice_id", invoiceId) }
                }
                .decodeList<InvoiceItemDto>()

            db.transaction {
                for (item in remoteItems) {
                    db.invoiceQueries.upsertInvoiceItemFromRemote(
                        id = item.id,
                        invoiceId = item.invoiceId,
                        productId = item.productId,
                        productName = item.productName,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        unitCost = item.unitCost
                    )
                }
            }
        }

        setLastPulled(SyncTables.INVOICE, maxUpdatedAt)
    }

    private suspend fun pullPayments(businessId: String) {
        val since = getLastPulled(SyncTables.PAYMENT)
        val remoteRows = supabaseClient.from(SyncTables.PAYMENT)
            .select {
                filter {
                    eq("business_id", businessId)
                    gt("updated_at", since)
                }
            }
            .decodeList<PaymentDto>()

        var maxUpdatedAt = since
        db.transaction {
            for (r in remoteRows) {
                // الدفعات سجل ثابت (append-only) — إدراج فقط إن لم تكن موجودة محليًا
                db.paymentQueries.upsertFromRemote(
                    id = r.id,
                    businessId = r.businessId,
                    partyId = r.partyId,
                    amount = r.amount,
                    balanceDelta = r.balanceDelta,
                    note = r.note,
                    createdAt = r.createdAt,
                    updatedAt = r.updatedAt
                )
                if (r.updatedAt > maxUpdatedAt) maxUpdatedAt = r.updatedAt
            }
        }
        setLastPulled(SyncTables.PAYMENT, maxUpdatedAt)
    }

    private suspend fun pullExpenses(businessId: String) {
        val since = getLastPulled(SyncTables.EXPENSE)
        val remoteRows = supabaseClient.from(SyncTables.EXPENSE)
            .select {
                filter {
                    eq("business_id", businessId)
                    gt("updated_at", since)
                }
            }
            .decodeList<ExpenseDto>()

        var maxUpdatedAt = since
        db.transaction {
            for (r in remoteRows) {
                val local = db.expenseQueries.selectExpenseById(r.id).executeAsOneOrNull()
                if (local == null || r.updatedAt > local.updatedAt) {
                    db.expenseQueries.upsertFromRemote(
                        id = r.id,
                        businessId = r.businessId,
                        amount = r.amount,
                        category = r.category,
                        note = r.note,
                        isDeleted = if (r.isDeleted) 1L else 0L,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt
                    )
                }
                if (r.updatedAt > maxUpdatedAt) maxUpdatedAt = r.updatedAt
            }
        }
        setLastPulled(SyncTables.EXPENSE, maxUpdatedAt)
    }

    // ------------------------------------------------------------------
    // نبضة Keep-alive + تخزين وقت آخر سحب لكل جدول
    // ------------------------------------------------------------------

    /** طلب خفيف جدًا لمشروع Supabase حتى ما يتوقف تلقائيًا بعد 7 أيام بدون نشاط */
    private suspend fun pingKeepAlive() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastPing = db.syncMetaQueries.selectMeta("last_ping_at").executeAsOneOrNull()?.metaValue?.toLongOrNull() ?: 0L
        // نبضة مرة كل 24 ساعة كحد أقصى، ما فيه فايدة نعمل أكثر من هيك
        if (now - lastPing < 24L * 60 * 60 * 1000) return
        try {
            supabaseClient.from(SyncTables.PRODUCT).select(columns = Columns.list("id")) {
                limit(1)
            }
            db.syncMetaQueries.upsertMeta("last_ping_at", now.toString())
        } catch (_: Exception) {
            // فشل النبضة ما يوقف باقي المزامنة، بنعيد المحاولة المرة الجاية
        }
    }

    private fun getLastPulled(tableName: String): Long {
        return db.syncMetaQueries.selectMeta("last_pulled_$tableName").executeAsOneOrNull()
            ?.metaValue?.toLongOrNull() ?: 0L
    }

    private fun setLastPulled(tableName: String, value: Long) {
        db.syncMetaQueries.upsertMeta("last_pulled_$tableName", value.toString())
    }
}
