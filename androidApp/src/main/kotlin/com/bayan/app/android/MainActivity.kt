package com.bayan.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.bayan.app.android.auth.AuthViewModel
import com.bayan.app.android.auth.LoginScreen
import com.bayan.app.android.dashboard.DashboardScreen
import com.bayan.app.android.dashboard.DashboardViewModel
import com.bayan.app.android.expenses.ExpensesScreen
import com.bayan.app.android.expenses.ExpensesViewModel
import com.bayan.app.android.invoices.InvoiceListScreen
import com.bayan.app.android.invoices.InvoiceListViewModel
import com.bayan.app.android.parties.PartyListScreen
import com.bayan.app.android.parties.PartyListViewModel
import com.bayan.app.android.products.ProductListScreen
import com.bayan.app.android.products.ProductListViewModel
import com.bayan.app.android.sales.SalesScreen
import com.bayan.app.android.sales.SalesViewModel
import com.bayan.app.data.DatabaseDriverFactory
import com.bayan.app.data.repository.AuthRepositoryImpl
import com.bayan.app.data.repository.ExpenseRepositoryImpl
import com.bayan.app.data.repository.PartyRepositoryImpl
import com.bayan.app.data.repository.ProductRepositoryImpl
import com.bayan.app.data.repository.SalesRepositoryImpl
import com.bayan.app.data.sync.NetworkMonitor
import com.bayan.app.data.sync.SyncEngine
import com.bayan.app.db.BayanDatabase
import com.bayan.app.domain.model.AuthState
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.repository.AuthRepository
import com.bayan.app.domain.repository.ExpenseRepository
import com.bayan.app.domain.repository.PartyRepository
import com.bayan.app.domain.repository.ProductRepository
import com.bayan.app.domain.repository.SalesRepository

private enum class BayanTab(val label: String) {
    DASHBOARD("الرئيسية"), SALES("بيع"), INVOICES("الفواتير"), EXPENSES("المصروفات"), PRODUCTS("المنتجات"), CUSTOMERS("العملاء"), SUPPLIERS("الموردون")
}

// كل شاشات التطبيق الرئيسية مع الـ ViewModels الخاصة بها، مبنية لنشاط تجاري (businessId) محدد
private class BayanViewModels(
    val dashboard: DashboardViewModel,
    val sales: SalesViewModel,
    val invoices: InvoiceListViewModel,
    val expenses: ExpensesViewModel,
    val products: ProductListViewModel,
    val customers: PartyListViewModel,
    val suppliers: PartyListViewModel
)

class MainActivity : ComponentActivity() {

    // تجميع قاعدة البيانات المحلية والمستودعات (لاحقًا سننقل هذا لـ Dependency Injection مناسب)
    // نفس قاعدة البيانات المحلية تُستخدم لأي حساب يسجّل دخول على هذا الجهاز؛
    // كل الاستعلامات مفلترة بـ businessId (= معرّف المستخدم بـ Supabase) فما فيه تداخل بيانات.
    private val driverFactory by lazy { DatabaseDriverFactory(applicationContext) }
    private val database by lazy { BayanDatabase(driverFactory.createDriver()) }
    private val productRepository: ProductRepository by lazy { ProductRepositoryImpl(database) }
    private val partyRepository: PartyRepository by lazy { PartyRepositoryImpl(database) }
    private val salesRepository: SalesRepository by lazy { SalesRepositoryImpl(database) }
    private val expenseRepository: ExpenseRepository by lazy { ExpenseRepositoryImpl(database) }
    private val authRepository: AuthRepository by lazy { AuthRepositoryImpl() }
    private val networkMonitor by lazy { NetworkMonitor(applicationContext) }
    // نفس محرك المزامنة يُعاد استخدامه طول عمر الـ Activity؛ start() يُستدعى مرة واحدة بعد كل تسجيل دخول
    private val syncEngine by lazy { SyncEngine(database, networkMonitor) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authViewModel = ViewModelProvider(
            this,
            viewModelFactory { AuthViewModel(authRepository) }
        )[AuthViewModel::class.java]

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    val authState by authViewModel.authState.collectAsState()

                    when (val state = authState) {
                        is AuthState.Loading -> LoadingScreen()
                        is AuthState.SignedOut -> LoginScreen(authViewModel)
                        is AuthState.SignedIn -> {
                            val viewModels = remember(state.user.businessId) {
                                buildViewModels(state.user.businessId)
                            }
                            // تشغيل المزامنة مرة واحدة لكل حساب يسجّل دخول (رفع + سحب فوري، وبعدها تلقائي مع الشبكة)
                            LaunchedEffect(state.user.businessId) {
                                syncEngine.start(state.user.businessId)
                            }
                            val syncStatus by syncEngine.status.collectAsState()
                            BayanApp(
                                viewModels,
                                syncStatus = syncStatus,
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildViewModels(businessId: String): BayanViewModels {
        val dashboardViewModel = ViewModelProvider(
            this,
            viewModelFactory { DashboardViewModel(salesRepository, expenseRepository, partyRepository, productRepository, businessId) }
        )["$businessId:dashboard", DashboardViewModel::class.java]

        val salesViewModel = ViewModelProvider(
            this,
            viewModelFactory { SalesViewModel(productRepository, partyRepository, salesRepository, businessId) }
        )["$businessId:sales", SalesViewModel::class.java]

        val invoiceListViewModel = ViewModelProvider(
            this,
            viewModelFactory { InvoiceListViewModel(salesRepository, businessId) }
        )["$businessId:invoices", InvoiceListViewModel::class.java]

        val expensesViewModel = ViewModelProvider(
            this,
            viewModelFactory { ExpensesViewModel(expenseRepository, businessId) }
        )["$businessId:expenses", ExpensesViewModel::class.java]

        val productViewModel = ViewModelProvider(
            this,
            viewModelFactory { ProductListViewModel(productRepository, businessId) }
        )["$businessId:products", ProductListViewModel::class.java]

        val customerViewModel = ViewModelProvider(
            this,
            viewModelFactory { PartyListViewModel(partyRepository, PartyType.CUSTOMER, businessId) }
        )["$businessId:customers", PartyListViewModel::class.java]

        val supplierViewModel = ViewModelProvider(
            this,
            viewModelFactory { PartyListViewModel(partyRepository, PartyType.SUPPLIER, businessId) }
        )["$businessId:suppliers", PartyListViewModel::class.java]

        return BayanViewModels(
            dashboard = dashboardViewModel,
            sales = salesViewModel,
            invoices = invoiceListViewModel,
            expenses = expensesViewModel,
            products = productViewModel,
            customers = customerViewModel,
            suppliers = supplierViewModel
        )
    }
}

private fun <T : ViewModel> viewModelFactory(create: () -> T) = object : ViewModelProvider.Factory {
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM {
        @Suppress("UNCHECKED_CAST")
        return create() as VM
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BayanApp(
    viewModels: BayanViewModels,
    syncStatus: com.bayan.app.data.sync.SyncStatus,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BayanTab.DASHBOARD) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BayanTab.DASHBOARD,
                    onClick = {
                        selectedTab = BayanTab.DASHBOARD
                        viewModels.dashboard.refresh()
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
                    selected = selectedTab == BayanTab.INVOICES,
                    onClick = { selectedTab = BayanTab.INVOICES },
                    icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                    label = { Text(BayanTab.INVOICES.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == BayanTab.EXPENSES,
                    onClick = { selectedTab = BayanTab.EXPENSES },
                    icon = { Icon(Icons.Filled.Payments, contentDescription = null) },
                    label = { Text(BayanTab.EXPENSES.label) }
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
                BayanTab.DASHBOARD -> DashboardScreen(viewModels.dashboard, syncStatus = syncStatus, onSignOut = onSignOut)
                BayanTab.SALES -> SalesScreen(viewModels.sales)
                BayanTab.INVOICES -> InvoiceListScreen(viewModels.invoices)
                BayanTab.EXPENSES -> ExpensesScreen(viewModels.expenses)
                BayanTab.PRODUCTS -> ProductListScreen(viewModels.products)
                BayanTab.CUSTOMERS -> PartyListScreen(viewModels.customers, PartyType.CUSTOMER)
                BayanTab.SUPPLIERS -> PartyListScreen(viewModels.suppliers, PartyType.SUPPLIER)
            }
        }
    }
}
