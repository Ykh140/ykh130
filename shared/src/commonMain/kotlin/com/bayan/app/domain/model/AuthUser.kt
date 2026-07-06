package com.bayan.app.domain.model

/**
 * مستخدم مسجّل دخول عبر Supabase.
 * businessId = نفس الـ userId (uid) بالضبط: كل حساب Supabase يمثّل نشاط تجاري واحد بالـ MVP،
 * وهذا يبسّط قواعد RLS (business_id = auth.uid()) ويجهّز البنية لدعم تعدد الأنشطة لاحقًا.
 */
data class AuthUser(
    val userId: String,
    val email: String?
) {
    val businessId: String get() = userId
}

sealed class AuthState {
    data object Loading : AuthState()
    data object SignedOut : AuthState()
    data class SignedIn(val user: AuthUser) : AuthState()
}
