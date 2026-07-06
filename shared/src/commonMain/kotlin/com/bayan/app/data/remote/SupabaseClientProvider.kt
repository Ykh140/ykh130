package com.bayan.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * إعدادات الاتصال بمشروع Supabase.
 *
 * ملاحظة أمان: المفتاح هون هو "publishable/anon key" وهو مصمم أصلاً للتضمين
 * بتطبيقات العميل (client-side) وما يعطي أي صلاحية إلا اللي تسمح فيها
 * سياسات Row Level Security (RLS) المكتوبة بقاعدة البيانات. ما تستخدم
 * أبدًا الـ "service_role/secret key" هون لأنه يتجاوز كل صلاحيات RLS.
 */
object SupabaseConfig {
    const val PROJECT_URL: String = "https://rkmbtrdgcrkbuvuigtzx.supabase.co"
    const val PUBLISHABLE_KEY: String = "sb_publishable_Pz__sphhxThSTDbgTqWlGQ_22RdiwaN"
}

/**
 * عميل Supabase - نسخة واحدة (singleton) تُستخدم بكل التطبيق.
 * التطبيق يبقى يشتغل بدون نت بالكامل (SQLDelight هو مصدر الحقيقة محليًا)،
 * وهذا العميل يُستخدم بس لما يكون في اتصال إنترنت لأغراض المصادقة والمزامنة.
 */
val supabaseClient: SupabaseClient by lazy {
    createSupabaseClient(
        supabaseUrl = SupabaseConfig.PROJECT_URL,
        supabaseKey = SupabaseConfig.PUBLISHABLE_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
