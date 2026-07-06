package com.bayan.app.data.repository

import com.bayan.app.data.remote.supabaseClient
import com.bayan.app.domain.model.AuthState
import com.bayan.app.domain.model.AuthUser
import com.bayan.app.domain.repository.AuthRepository
import com.bayan.app.domain.repository.AuthResult
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthRepositoryImpl : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // نتابع حالة الجلسة تلقائيًا: أي تغيير (دخول/خروج/تجديد توكن) ينعكس فورًا
        scope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                _authState.value = when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        if (user != null) {
                            AuthState.SignedIn(AuthUser(userId = user.id, email = user.email))
                        } else {
                            AuthState.SignedOut
                        }
                    }
                    is SessionStatus.NotAuthenticated -> AuthState.SignedOut
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.RefreshFailure -> AuthState.SignedOut
                }
            }
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success
        } catch (e: RestException) {
            AuthResult.Error(e.message ?: "تعذّر إنشاء الحساب")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "تعذّر إنشاء الحساب، تأكد من الاتصال بالإنترنت")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success
        } catch (e: RestException) {
            AuthResult.Error(e.message ?: "بيانات الدخول غير صحيحة")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "تعذّر تسجيل الدخول، تأكد من الاتصال بالإنترنت")
        }
    }

    override suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (_: Exception) {
            // لو ما في نت، بنكتفي بمسح الجلسة المحلية
        }
    }

    override suspend fun restoreSession() {
        // supabase-kt يسترجع الجلسة المحفوظة محليًا تلقائيًا عند التهيئة؛
        // هذا الاستدعاء يضمن انتظار انتهاء التهيئة قبل ما نعرض الشاشة الأولى.
        supabaseClient.auth.awaitInitialization()
    }
}
