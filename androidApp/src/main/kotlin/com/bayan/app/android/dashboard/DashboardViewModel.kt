package com.bayan.app.android.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.domain.model.Product
import com.bayan.app.domain.repository.ExpenseRepository
import com.bayan.app.domain.repository.PartyRepository
import com.bayan.app.domain.repository.ProductRepository
import com.bayan.app.domain.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.atStartOfDayIn

data class DashboardState(
    val todaySales: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val totalCustomerDebt: Double = 0.0,
    val lowStockProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true
) {
    val netToday: Double get() = todayProfit - todayExpenses
}

class DashboardViewModel(
    private val salesRepository: SalesRepository,
    private val expenseRepository: ExpenseRepository,
    private val partyRepository: PartyRepository,
    private val productRepository: ProductRepository,
    private val businessId: String
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val startOfDay = todayStartMillis()

            val sales = salesRepository.getTodaySalesTotal(businessId, startOfDay)
            val profit = salesRepository.getTodayProfitTotal(businessId, startOfDay)
            val expenses = expenseRepository.getTodayExpensesTotal(businessId, startOfDay)
            val debt = partyRepository.getTotalCustomerDebt(businessId)
            val lowStock = productRepository.getLowStock(businessId)

            _state.value = DashboardState(
                todaySales = sales,
                todayProfit = profit,
                todayExpenses = expenses,
                totalCustomerDebt = debt,
                lowStockProducts = lowStock,
                isLoading = false
            )
        }
    }

    fun addExpense(amount: Double, category: String?, note: String?) {
        viewModelScope.launch {
            expenseRepository.addExpense(businessId, amount, category, note)
            refresh()
        }
    }

    private fun todayStartMillis(): Long {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(timeZone)
        return today.atStartOfDayIn(timeZone).toEpochMilliseconds()
    }
}
