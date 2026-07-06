package com.bayan.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.bayan.app.android.dashboard.DashboardScreen
import com.bayan.app.android.dashboard.DashboardViewModel
import com.bayan.app.android.parties.PartyListScreen
import com.bayan.app.android.parties.PartyListViewModel
import com.bayan.app.android.products.ProductListScreen
import com.bayan.app.android.products.ProductListViewModel
import com.bayan.app.android.sales.SalesScreen
import com.bayan.app.android.sales.SalesViewModel
import com.bayan.app.data.DatabaseDriverFactory
import com.bayan.app.data.repository.ExpenseRepositoryImpl
import com.bayan.app.data.repository.PartyRepositoryImpl
import com.bayan.app.data.repository.ProductRepositoryImpl
import com.bayan.app.data.repository.SalesRepositoryImpl
import com.bayan.app.db.BayanDatabase
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.repository.ExpenseRepository
import com.bayan.app.domain.repository.PartyRepository
import com.bayan.app.domain.repository.ProductRepository
import com.bayan.app.domain.repository.SalesRepository

private enum class BayanTab(val label: String) {
    DASHBOARD("الرئيسية"), SALES("بيع"), PRODUCTS("المنتجات"), CUSTOMERS("العملاء"), SUPPLIERS("الموردون")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تجميع قاعدة البيانات والمستودعات (لاحقًا سننقل هذا لـ Dependency Injection مناسب)
        val driverFactory = DatabaseDriverFactory(applicationContext)
        val database = BayanDatabase(driverFactory.createDriver())
        val productRepository: ProductRepository = ProductRepositoryImpl(database)
        val partyRepository: PartyRepository = PartyRepositoryImpl(database)
        val salesRepository: SalesRepository = SalesRepositoryImpl(database)
        val expenseRepository: ExpenseRepository = ExpenseRepositoryImpl(database)

        val dashboardViewModel = ViewModelProvider(
            this,
            viewModelFactory { DashboardViewModel(salesRepository, expenseRepository, partyRepository, productRepository) }
        )[DashboardViewModel::class.java]

        val salesViewModel = ViewModelProvider(
            this,
            viewModelFactory { SalesViewModel(productRepository, partyRepository, salesRepository) }
        )[SalesViewModel::class.java]

        val productViewModel = ViewModelProvider(
            this,
            viewModelFactory { ProductListViewModel(productRepository) }
        )[ProductListViewModel::class.java]

        val customerViewModel = ViewModelProvider(
            this,
            viewModelFactory { PartyListViewModel(partyRepository, PartyType.CUSTOMER) }
        )["customers", PartyListViewModel::class.java]

        val supplierViewModel = ViewModelProvider(
            this,
            viewModelFactory { PartyListViewModel(partyRepository, PartyType.SUPPLIER) }
        )["suppliers", PartyListViewModel::class.java]

        setContent {
            BayanApp(dashboardViewModel, salesViewModel, productViewModel, customerViewModel, supplierViewModel)
        }
    }
}

private fun <T : ViewModel> viewModelFactory(create: () -> T) = object : ViewModelProvider.Factory {
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM {
        @Suppress("UNCHECKED_CAST")
        return create() as VM
    }
}

@Composable
private fun BayanApp(
    dashboardViewModel: DashboardViewModel,
    salesViewModel: SalesViewModel,
    productViewModel: ProductListViewModel,
    customerViewModel: PartyListViewModel,
    supplierViewModel: PartyListViewModel
) {
    // دعم RTL الكامل للعربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme {
            var selectedTab by remember { mutableStateOf(BayanTab.DASHBOARD) }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == BayanTab.DASHBOARD,
                            onClick = {
                                selectedTab = BayanTab.DASHBOARD
                                dashboardViewModel.refresh()
                            },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text(BayanTab.DASHBOARD.label) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == BayanTab.SALES,
                            onClick = { selectedTab = BayanTab.SALES },
                            icon = { Icon(Icons.Filled.PointOfSale, contentDescription = null) },
                            label = { Text(BayanTab.SALES.label) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == BayanTab.PRODUCTS,
                            onClick = { selectedTab = BayanTab.PRODUCTS },
                            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                            label = { Text(BayanTab.PRODUCTS.label) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == BayanTab.CUSTOMERS,
                            onClick = { selectedTab = BayanTab.CUSTOMERS },
                            icon = { Icon(Icons.Filled.People, contentDescription = null) },
                            label = { Text(BayanTab.CUSTOMERS.label) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == BayanTab.SUPPLIERS,
                            onClick = { selectedTab = BayanTab.SUPPLIERS },
                            icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
                            label = { Text(BayanTab.SUPPLIERS.label) }
                        )
                    }
                }
            ) { padding ->
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        BayanTab.DASHBOARD -> DashboardScreen(dashboardViewModel)
                        BayanTab.SALES -> SalesScreen(salesViewModel)
                        BayanTab.PRODUCTS -> ProductListScreen(productViewModel)
                        BayanTab.CUSTOMERS -> PartyListScreen(customerViewModel, PartyType.CUSTOMER)
                        BayanTab.SUPPLIERS -> PartyListScreen(supplierViewModel, PartyType.SUPPLIER)
                    }
                }
            }
        }
    }
}
