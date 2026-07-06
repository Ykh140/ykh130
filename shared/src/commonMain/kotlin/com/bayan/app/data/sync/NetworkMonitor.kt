package com.bayan.app.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * يراقب توفر اتصال الإنترنت (لا يضمن اتصالًا فعليًا بالسيرفر، فقط أن الجهاز متصل بشبكة).
 * تطبيق كل منصة (Android/Desktop/iOS) يزوّد النسخة الفعلية عبر `expect`/`actual`.
 */
expect class NetworkMonitor {
    /** يبعث true/false في كل مرة تتغيّر حالة الاتصال */
    val isOnline: Flow<Boolean>

    /** فحص فوري لحالة الاتصال الحالية بدون انتظار بث جديد */
    fun isCurrentlyOnline(): Boolean
}
