package com.bayan.app.data

import app.cash.sqldelight.db.SqlDriver

/**
 * كل منصة (Android, Desktop, iOS) تزودنا بطريقتها الخاصة لإنشاء SqlDriver.
 * هذا هو أساس عمل SQLDelight على منصات متعددة من نفس الكود المشترك.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
