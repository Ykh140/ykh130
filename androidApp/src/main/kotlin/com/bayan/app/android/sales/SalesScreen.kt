package com.bayan.app.android.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PaymentMethod
import com.bayan.app.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: SalesViewModel) {
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val saved by viewModel.lastInvoiceSaved.collectAsState()
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (saved) viewModel.consumeSavedFlag()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("فاتورة بيع جديدة") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            CustomerPicker(
                customers = customers,
                selected = selectedCustomer,
                onSelect = { viewModel.selectCustomer(it) }
            )

            Divider()

            Text(
                "اضغط على منتج لإضافته للفاتورة",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("أضف منتجات أولًا من تبويب المنتجات", textAlign = TextAlign.Center)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductChip(product = product, onClick = { viewModel.addToCart(product) })
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("بنود الفاتورة", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)

            if (cart.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("السلة فارغة")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cart, key = { it.product.id }) { line ->
                        CartRow(line = line, onRemove = { viewModel.removeFromCart(line.product.id) })
                    }
                }
            }

            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الإجمالي: ${viewModel.cartTotal}", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showPaymentDialog = true }, enabled = cart.isNotEmpty()) {
                        Text("إتمام البيع")
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        PaymentMethodDialog(
            requiresCustomer = selectedCustomer == null,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { method ->
                viewModel.confirmSale(method)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
private fun CustomerPicker(customers: List<Party>, selected: Party?, onSelect: (Party?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.name ?: "بيع بدون عميل محدد (اختياري)")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("بدون عميل") }, onClick = { onSelect(null); expanded = false })
            customers.forEach { customer ->
                DropdownMenuItem(text = { Text(customer.name) }, onClick = { onSelect(customer); expanded = false })
            }
        }
    }
}

@Composable
private fun ProductChip(product: Product, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.width(140.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text("${product.salePrice}", style = MaterialTheme.typography.bodySmall)
            Text("متوفر: ${product.quantity.toInt()}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CartRow(line: CartLine, onRemove: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(line.product.name)
                Text("${line.quantity.toInt()} × ${line.product.salePrice} = ${line.lineTotal}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "إزالة")
            }
        }
    }
}

@Composable
private fun PaymentMethodDialog(
    requiresCustomer: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("طريقة الدفع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (requiresCustomer) {
                    Text(
                        "ملاحظة: البيع بالدين يحتاج اختيار عميل أولًا",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(onClick = { onConfirm(PaymentMethod.CASH) }, modifier = Modifier.fillMaxWidth()) {
                    Text("نقدًا")
                }
                Button(onClick = { onConfirm(PaymentMethod.TRANSFER) }, modifier = Modifier.fillMaxWidth()) {
                    Text("تحويل")
                }
                Button(
                    onClick = { onConfirm(PaymentMethod.DEBT) },
                    enabled = !requiresCustomer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("دين")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
