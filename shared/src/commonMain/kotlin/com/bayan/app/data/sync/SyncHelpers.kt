package com.bayan.app.data.sync

import com.bayan.app.db.BayanDatabase

/**
 * أسماء الجداول القابلة للمزامنة، بنفس القيمة المستخدمة كـ tableName بقائمة الانتظار (SyncQueue)
 * وكاسم الجدول المطابق بـ Supabase (public schema, snake_case).
 */
object SyncTables {
    const val PRODUCT = "product"
    const val CUSTOMER = "customer"
    const val INVOICE = "invoice"
    const val PAYMENT = "payment"
    const val EXPENSE = "expense"

    /** كل الجداول القابلة للمزامنة، بالترتيب المناسب للسحب (لا يوجد تبعيات فعلية بينها عدا invoice/invoice_item) */
    val ALL = listOf(PRODUCT, CUSTOMER, INVOICE, PAYMENT, EXPENSE)
}

/**
 * يسجّل تغييرًا محليًا (إضافة/تعديل/حذف ناعم) بقائمة انتظار الرفع.
 * يُستدعى دائمًا من داخل نفس db.transaction { } التي حصل فيها التعديل، حتى يبقى
 * تسجيل المزامنة والتعديل الفعلي عملية واحدة (إما ينجحان معًا أو يفشلان معًا).
 */
fun BayanDatabase.enqueueSync(tableName: String, entityId: String, atMillis: Long) {
    syncQueueQueries.enqueue(tableName = tableName, entityId = entityId, enqueuedAt = atMillis)
}
