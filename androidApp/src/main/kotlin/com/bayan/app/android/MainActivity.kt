package com.bayan.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.bayan.app.android.products.ProductListScreen
import com.bayan.app.android.products.ProductListViewModel
import com.bayan.app.data.DatabaseDriverFactory
import com.bayan.app.data.repository.ProductRepositoryImpl
import com.bayan.app.db.BayanDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تجميع قاعدة البيانات والمستودعات (لاحقًا سننقل هذا لـ Dependency Injection مناسب)
        val driverFactory = DatabaseDriverFactory(applicationContext)
        val database = BayanDatabase(driverFactory.createDriver())
        val productRepository = ProductRepositoryImpl(database)

        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    @Suppress("UNCHECKED_CAST")
                    return ProductListViewModel(productRepository) as T
                }
            }
        )[ProductListViewModel::class.java]

        setContent {
            BayanApp(viewModel)
        }
    }
}

@Composable
private fun BayanApp(viewModel: ProductListViewModel) {
    // دعم RTL الكامل للعربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                ProductListScreen(viewModel)
            }
        }
    }
}
