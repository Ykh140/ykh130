package com.bayan.app.domain.model

/**
 * بند واحد في كشف حساب عميل/مورد.
 * يجمع عمليات البيع (فواتير) مع الدفعات اليدوية (تسديد/سلفة) في قائمة واحدة مرتبة بالتاريخ.
 */
enum class StatementEntryType {
    SALE_DEBT,      // بيع بالدين - يزيد ما هو مستحق على الطرف
    SALE_CASH,      // بيع نقدي أو تحويل - لا يؤثر على الرصيد لكنه جزء من السجل
    PAYMENT_IN,     // تسديد من الطرف - يحسن الرصيد
    PAYMENT_OUT     // دفعة/سلفة له - يقلل الرصيد
}

data class StatementEntry(
    val id: String,
    val type: StatementEntryType,
    val description: String,
    val amount: Double,       // القيمة المعروضة (دائمًا موجبة)
    val balanceDelta: Double, // الأثر الفعلي على Party.balance (قد يكون صفر للبيع النقدي)
    val createdAt: Long
)
