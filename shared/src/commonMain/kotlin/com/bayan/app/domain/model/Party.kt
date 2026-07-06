package com.bayan.app.domain.model

enum class PartyType {
    CUSTOMER, SUPPLIER
}

/**
 * نموذج العميل/المورد - نفس الجدول يخدم الاثنين عبر type
 */
data class Party(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String?,
    val type: PartyType,
    val balance: Double, // موجب = له عندنا، سالب = علينا (أو العكس حسب الاصطلاح المعروض)
    val notes: String?
)
