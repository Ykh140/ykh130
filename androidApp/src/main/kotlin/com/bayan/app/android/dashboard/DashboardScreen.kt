package com.bayan.app.android.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsState()
    var showAddExpense by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("لوحة التحكم") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExpense = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("مصروف") }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(title = "مبيعات اليوم", value = state.todaySales, modifier = Modifier.weight(1f))
                    StatCard(title = "أرباح اليوم", value = state.todayProfit, modifier = Modifier.weight(1f), highlight = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(title = "مصروفات اليوم", value = state.todayExpenses, modifier = Modifier.weight(1f))
                    StatCard(
                        title = "صافي اليوم",
                        value = state.netToday,
                        modifier = Modifier.weight(1f),
                        isNegativeBad = true
                    )
                }
            }
            item {
                StatCard(
                    title = "إجمالي الديون على العملاء",
                    value = state.totalCustomerDebt,
                    modifier = Modifier.fillMaxWidth(),
                    isNegativeBad = true
                )
            }
            item {
                Text("منتجات قليلة الكمية", style = MaterialTheme.typography.titleSmall)
            }
            if (state.lowStockProducts.isEmpty()) {
                item {
                    Text("لا يوجد منتجات منخفضة المخزون حاليًا", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(state.lowStockProducts, key = { it.id }) { product ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(product.name)
                            Text("متبقي: ${product.quantity.toInt()} ${product.unit}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddExpense) {
        AddExpenseDialog(
            onDismiss = { showAddExpense = false },
            onConfirm = { amount, category, note ->
                viewModel.addExpense(amount, category, note)
                showAddExpense = false
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: Double,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    isNegativeBad: Boolean = false
) {
    val color = when {
        isNegativeBad && value < 0 -> MaterialTheme.colorScheme.error
        highlight -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = color)
        }
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String?, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مصروف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("المبلغ") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("التصنيف (اختياري)") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("ملاحظة (اختياري)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    onConfirm(amt, category.ifBlank { null }, note.ifBlank { null })
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
