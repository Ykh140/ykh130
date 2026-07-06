package com.bayan.app.android.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.domain.model.Expense
import com.bayan.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// تصنيفات مقترحة للمستخدم (يبقى بإمكانه كتابة تصنيف حر أيضًا)
val SUGGESTED_EXPENSE_CATEGORIES = listOf("إيجار", "كهرباء وماء", "رواتب", "نقل ومواصلات", "صيانة", "أخرى")

// تمثل "الكل" في فلتر التصنيفات
const val ALL_CATEGORIES = "الكل"

class ExpensesViewModel(
    private val repository: ExpenseRepository,
    private val businessId: String
) : ViewModel() {

    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORIES)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _filteredExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val filteredExpenses: StateFlow<List<Expense>> = _filteredExpenses.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(listOf(ALL_CATEGORIES))
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeExpenses(businessId).collect { list ->
                _allExpenses.value = list
                recomputeCategories()
                applyFilter()
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        applyFilter()
    }

    fun addExpense(amount: Double, category: String?, note: String?) {
        viewModelScope.launch {
            repository.addExpense(businessId, amount, category, note)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    private fun recomputeCategories() {
        val distinctCategories = _allExpenses.value
            .mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }
            .distinct()
            .sorted()
        _availableCategories.value = listOf(ALL_CATEGORIES) + distinctCategories
        // إذا اختفى التصنيف المحدد حاليًا (كل مصروفاته حُذفت)، ارجع لعرض الكل
        if (_selectedCategory.value != ALL_CATEGORIES && _selectedCategory.value !in distinctCategories) {
            _selectedCategory.value = ALL_CATEGORIES
        }
    }

    private fun applyFilter() {
        val category = _selectedCategory.value
        _filteredExpenses.value = if (category == ALL_CATEGORIES) {
            _allExpenses.value
        } else {
            _allExpenses.value.filter { it.category == category }
        }
    }
}
