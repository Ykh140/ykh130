package com.bayan.app.android.invoices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bayan.app.domain.model.Invoice
import com.bayan.app.domain.model.PaymentMethod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun InvoiceListScreen(viewModel: InvoiceListViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    val selectedInvoiceId by viewModel.selectedInvoiceId.collectAsState()

    if (selectedInvoiceId != null) {
        val invoice = invoices.find { it.id == selectedInvoiceId }
        if (invoice != null) {
            InvoiceDetailScreen(invoice = invoice, onBack = { viewModel.closeInvoice() })
        } else {
            // الفاتورة لم تعد موجودة (حذفت أو ما زالت تحمّل) - ارجع للقائمة
            viewModel.closeInvoice()
        }
    } else {
        InvoiceListContent(invoices = invoices, onOpenInvoice = { viewModel.openInvoice(it) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceListContent(invoices: List<Invoice>, onOpenInvoice: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("الفواتير") }) }
    ) { padding ->
        if (invoices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد فواتير بعد\nستظهر هنا كل عملية بيع تتم",
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
                items(invoices, key = { it.id }) { invoice ->
                    InvoiceRow(invoice = invoice, onClick = { onOpenInvoice(invoice.id) })
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice, onClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    invoice.customerName ?: "بيع نقدي عابر",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${invoice.items.size} صنف • ${formatDateTime(invoice.createdAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${formatAmount(invoice.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    invoice.paymentMethod.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = invoice.paymentMethod.color()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDetailScreen(invoice: Invoice, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الفاتورة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow("العميل", invoice.customerName ?: "بيع نقدي عابر")
                        InfoRow("التاريخ والوقت", formatDateTime(invoice.createdAt))
                        InfoRow("طريقة الدفع", invoice.paymentMethod.label())
                        InfoRow("عدد الأصناف", invoice.items.size.toString())
                    }
                }
            }

            item {
                Text("الأصناف", style = MaterialTheme.typography.titleMedium)
            }

            items(invoice.items) { line ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(line.productName, style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${line.quantity} × ${formatAmount(line.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                formatAmount(line.lineTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow("الإجمالي", formatAmount(invoice.totalAmount), bold = true)
                        InfoRow("الربح", formatAmount(invoice.totalProfit))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.CASH -> "نقدًا"
    PaymentMethod.TRANSFER -> "تحويل"
    PaymentMethod.DEBT -> "دين"
}

@Composable
private fun PaymentMethod.color() = when (this) {
    PaymentMethod.DEBT -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

private fun formatAmount(amount: Double): String {
    val rounded = kotlin.math.round(amount * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun pad(n: Int) = n.toString().padStart(2, '0')
    return "${pad(dt.dayOfMonth)}/${pad(dt.monthNumber)}/${dt.year} ${pad(dt.hour)}:${pad(dt.minute)}"
}
