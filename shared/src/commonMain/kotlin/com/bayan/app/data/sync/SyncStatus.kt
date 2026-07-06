package com.bayan.app.data.sync

/** حالة المزامنة الحالية، تُعرض بالواجهة كمؤشر بسيط ("متزامن" / "جارِ الرفع" / "بدون اتصال") */
sealed class SyncStatus {
    /** لا يوجد عمل جارٍ حاليًا. لا تعني بالضرورة أن كل شيء مرفوع (تحقق من hasPendingChanges) */
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data object Offline : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
