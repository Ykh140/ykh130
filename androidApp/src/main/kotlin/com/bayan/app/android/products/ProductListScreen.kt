package com.bayan.app.android.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bayan.app.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(viewModel: ProductListViewModel) {
    val products by viewModel.products.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("المنتجات") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة منتج")
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد منتجات بعد\nاضغط + لإضافة أول منتج",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductRow(product = product, onDelete = { viewModel.deleteProduct(product.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, purchase, sale, qty, threshold, unit, barcode ->
                viewModel.addProduct(name, purchase, sale, qty, threshold, unit, barcode)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ProductRow(product: Product, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "الكمية: ${product.quantity.toInt()} ${product.unit} • السعر: ${product.salePrice}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (product.isLowStock) {
                    Text(
                        "⚠️ الكمية منخفضة",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف")
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, purchase: Double, sale: Double, qty: Double, threshold: Double, unit: String, barcode: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var salePrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("قطعة") }
    var barcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة منتج") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج") }, singleLine = true)
                OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("سعر الشراء") }, singleLine = true)
                OutlinedTextField(value = salePrice, onValueChange = { salePrice = it }, label = { Text("سعر البيع") }, singleLine = true)
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("الكمية") }, singleLine = true)
                OutlinedTextField(value = threshold, onValueChange = { threshold = it }, label = { Text("أقل كمية للتنبيه") }, singleLine = true)
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("الوحدة") }, singleLine = true)
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("الباركود (اختياري)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(
                        name.trim(),
                        purchasePrice.toDoubleOrNull() ?: 0.0,
                        salePrice.toDoubleOrNull() ?: 0.0,
                        quantity.toDoubleOrNull() ?: 0.0,
                        threshold.toDoubleOrNull() ?: 0.0,
                        unit.ifBlank { "قطعة" },
                        barcode.ifBlank { null }
                    )
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
