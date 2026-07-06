package com.bayan.app.android.parties

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bayan.app.domain.model.Party
import com.bayan.app.domain.model.PartyType
import com.bayan.app.domain.model.StatementEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyListScreen(viewModel: PartyListViewModel, type: PartyType) {
    val parties by viewModel.parties.collectAsState()
    val selectedParty by viewModel.selectedParty.collectAsState()
    val statement by viewModel.statement.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val title = if (type == PartyType.CUSTOMER) "العملاء" else "الموردون"

    if (selectedParty != null) {
        PartyDetailScreen(
            party = selectedParty!!,
            statement = statement,
            onBack = { viewModel.closeParty() },
            onRecordPayment = { amount, isCredit, note -> viewModel.recordPayment(amount, isCredit, note) }
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة")
            }
        }
    ) { padding ->
        if (parties.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا يوجد $title بعد\nاضغط + للإضافة",
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
                items(parties, key = { it.id }) { party ->
                    PartyRow(
                        party = party,
                        onClick = { viewModel.openParty(party) },
                        onDelete = { viewModel.deleteParty(party.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPartyDialog(
            title = title,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, notes ->
                viewModel.addParty(name, phone, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PartyRow(party: Party, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(party.name, style = MaterialTheme.typography.titleMedium)
                party.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                val balanceLabel = if (party.balance >= 0) "له: ${party.balance}" else "عليه: ${-party.balance}"
                Text(
                    balanceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (party.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartyDetailScreen(
    party: Party,
    statement: List<StatementEntry>,
    onBack: () -> Unit,
    onRecordPayment: (amount: Double, isCredit: Boolean, note: String?) -> Unit
) {
    var showPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(party.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPaymentDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "تسجيل دفعة")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("الرصيد الحالي", style = MaterialTheme.typography.bodyMedium)
                        val balanceLabel = if (party.balance >= 0) "له: ${formatAmount(party.balance)}" else "عليه: ${formatAmount(-party.balance)}"
                        Text(
                            balanceLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (party.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Text("كشف الحساب", style = MaterialTheme.typography.titleMedium)
            }

            if (statement.isEmpty()) {
                item {
                    Text(
                        "لا توجد عمليات بعد",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(statement, key = { it.id }) { entry ->
                    StatementRow(entry)
                }
            }
        }
    }

    if (showPaymentDialog) {
        RecordPaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, isCredit, note ->
                onRecordPayment(amount, isCredit, note)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
private fun StatementRow(entry: StatementEntry) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.description, style = MaterialTheme.typography.bodyLarge)
                Text(formatDateTime(entry.createdAt), style = MaterialTheme.typography.bodySmall)
            }
            val (sign, color) = when {
                entry.balanceDelta > 0 -> "+" to MaterialTheme.colorScheme.primary
                entry.balanceDelta < 0 -> "-" to MaterialTheme.colorScheme.error
                else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                "$sign${formatAmount(entry.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun RecordPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, isCredit: Boolean, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل دفعة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("تسديد (يحسّن الرصيد)") }
                    )
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("دفعة/سلفة (تقلل الرصيد)") }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة (اختياري)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toDoubleOrNull()
                if (value != null && value > 0) {
                    onConfirm(value, isCredit, note.ifBlank { null })
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun AddPartyDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة إلى $title") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف (اختياري)") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات (اختياري)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name.trim(), phone.ifBlank { null }, notes.ifBlank { null })
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
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
