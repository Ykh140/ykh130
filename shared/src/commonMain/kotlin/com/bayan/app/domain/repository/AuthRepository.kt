package com.bayan.app.domain.repository

import com.bayan.app.domain.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/** نتيجة عملية مصادقة (تسجيل دخول / إنشاء حساب) بدون رمي استثناءات للواجهة */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    /** حالة تسجيل الدخول الحالية، تتحدث تلقائيًا (Loading / SignedOut / SignedIn) */
    val authState: StateFlow<AuthState>

    suspend fun signUp(email: String, password: String): AuthResult

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun signOut()

    /** يحاول استرجاع جلسة محفوظة محليًا عند فتح التطبيق (يشتغل حتى بدون نت) */
    suspend fun restoreSession()
}
