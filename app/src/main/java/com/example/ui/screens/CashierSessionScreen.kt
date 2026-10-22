package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CashierSession
import com.example.data.local.entity.Crew
import com.example.localization.LocalizedStrings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Formatters
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierSessionScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val session = uiState.activeSession
    val isOpen = session != null

    var showOpenSessionDialog by remember { mutableStateOf(false) }
    var showCloseSessionDialog by remember { mutableStateOf(false) }
    var showCashMovementDialog by remember { mutableStateOf<String?>(null) } // "CASH_IN" or "CASH_OUT"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sesi Kasir & Uang Laci",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Session Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = if (isOpen) Color(0xFF2E7D32) else BrandGray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOpen) "SESI SEDANG AKTIF" else "SESI BELUM DIBUKA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isOpen) Color(0xFF2E7D32) else BrandGray
                                )
                            }

                            if (!isOpen) {
                                Button(
                                    onClick = { showOpenSessionDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("open_session_btn")
                                ) {
                                    Text("Buka Sesi", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = { showCloseSessionDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("close_session_btn")
                                ) {
                                    Text("Tutup Sesi", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        if (isOpen && session != null) {
                            Divider(color = BrandGrayBorder)

                            SessionDetailRow("Kasir", "${session.cashierName} (${session.cashierRole})")
                            SessionDetailRow("Waktu Buka", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(session.openedAt)))
                            SessionDetailRow("Modal Awal Laci", Formatters.formatRupiah(session.openingCash))
                            SessionDetailRow("Penjualan Tunai (+)", Formatters.formatRupiah(session.cashSalesTotal))
                            SessionDetailRow("Pemasukan Kas Masuk (+)", Formatters.formatRupiah(session.cashInTotal))
                            SessionDetailRow("Pengeluaran Kas Keluar (-)", Formatters.formatRupiah(session.cashOutTotal))

                            val expectedCash = session.openingCash + session.cashSalesTotal + session.cashInTotal - session.cashOutTotal
                            Surface(
                                color = BrandToskaLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Perkiraan Uang Tunai di Laci:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandToskaDark)
                                    Text(Formatters.formatRupiah(expectedCash), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandToskaDark)
                                }
                            }

                            SessionDetailRow("Penjualan QRIS (Non-Tunai)", Formatters.formatRupiah(session.qrisSalesTotal))

                            // Quick Movement Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showCashMovementDialog = "CASH_IN" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = BrandToska)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kas Masuk (+)", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showCashMovementDialog = "CASH_OUT" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kas Keluar (-)", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "Buka sesi sebelum kasir mulai melayani pelanggan untuk menjaga akurasi pembukuan uang fisik kasir.",
                                fontSize = 13.sp,
                                color = BrandGray
                            )
                        }
                    }
                }
            }
        }
    }

    // Open Session Dialog
    if (showOpenSessionDialog) {
        OpenSessionDialog(
            crews = uiState.activeCrew,
            onDismiss = { showOpenSessionDialog = false },
            onConfirm = { crew, openingCash, notes ->
                viewModel.openSession(crew, openingCash, notes) {
                    showOpenSessionDialog = false
                }
            }
        )
    }

    // Close Session Dialog
    if (showCloseSessionDialog && session != null) {
        val expectedCash = session.openingCash + session.cashSalesTotal + session.cashInTotal - session.cashOutTotal
        CloseSessionDialog(
            session = session,
            expectedCash = expectedCash,
            onDismiss = { showCloseSessionDialog = false },
            onConfirm = { actualCash, notes ->
                viewModel.closeSession(session.id, actualCash, notes) {
                    showCloseSessionDialog = false
                }
            }
        )
    }

    // Cash In / Cash Out Dialog
    if (showCashMovementDialog != null) {
        val movementType = showCashMovementDialog!!
        CashMovementDialog(
            type = movementType,
            onDismiss = { showCashMovementDialog = null },
            onConfirm = { amount, reason ->
                viewModel.recordCashMovement(movementType, amount, reason) {
                    showCashMovementDialog = null
                }
            }
        )
    }
}

@Composable
fun SessionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = BrandGray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun OpenSessionDialog(
    crews: List<Crew>,
    onDismiss: () -> Unit,
    onConfirm: (Crew, Double, String) -> Unit
) {
    var selectedCrew by remember { mutableStateOf(crews.firstOrNull()) }
    var openingCashInput by remember { mutableStateOf("100000") }
    var notesInput by remember { mutableStateOf("") }
    var crewDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buka Sesi Kasir Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedTextField(
                        value = "${selectedCrew?.name ?: "Pilih Kasir"} (${selectedCrew?.role ?: "-"})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kasir Bertugas") },
                        trailingIcon = {
                            IconButton(onClick = { crewDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = crewDropdownExpanded,
                        onDismissRequest = { crewDropdownExpanded = false }
                    ) {
                        crews.forEach { cr ->
                            DropdownMenuItem(
                                text = { Text("${cr.name} (${cr.role})") },
                                onClick = {
                                    selectedCrew = cr
                                    crewDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = openingCashInput,
                    onValueChange = { openingCashInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Modal Awal Laci Kasir (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Catatan Pembukaan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cash = openingCashInput.toDoubleOrNull() ?: 0.0
                    if (selectedCrew != null) {
                        onConfirm(selectedCrew!!, cash, notesInput.trim())
                    }
                },
                enabled = selectedCrew != null,
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
            ) {
                Text("Buka Sesi Sekarang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun CloseSessionDialog(
    session: CashierSession,
    expectedCash: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var actualCashInput by remember { mutableStateOf("${expectedCash.toInt()}") }
    var notesInput by remember { mutableStateOf("") }

    val actualCash = actualCashInput.toDoubleOrNull() ?: 0.0
    val variance = actualCash - expectedCash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tutup Sesi Kasir", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Kasir: ${session.cashierName} (${session.cashierRole})", fontWeight = FontWeight.SemiBold)
                SessionDetailRow("Modal Awal", Formatters.formatRupiah(session.openingCash))
                SessionDetailRow("Penjualan Tunai", Formatters.formatRupiah(session.cashSalesTotal))
                SessionDetailRow("Pemasukan Kas", Formatters.formatRupiah(session.cashInTotal))
                SessionDetailRow("Pengeluaran Kas", Formatters.formatRupiah(session.cashOutTotal))
                Divider()
                SessionDetailRow("Perkiraan Uang Laci", Formatters.formatRupiah(expectedCash))

                OutlinedTextField(
                    value = actualCashInput,
                    onValueChange = { actualCashInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Uang Tunai Aktual Fisik di Laci (Rp) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Variance Card
                Surface(
                    color = if (variance == 0.0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Selisih Kas:",
                            fontWeight = FontWeight.Bold,
                            color = if (variance == 0.0) Color(0xFF2E7D32) else BrandOrangeDark
                        )
                        Text(
                            (if (variance > 0) "+" else "") + Formatters.formatRupiah(variance),
                            fontWeight = FontWeight.Bold,
                            color = if (variance == 0.0) Color(0xFF2E7D32) else BrandOrangeDark
                        )
                    }
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Catatan Penutupan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(actualCash, notesInput.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Konfirmasi Tutup Sesi", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun CashMovementDialog(
    type: String, // CASH_IN or CASH_OUT
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, reason: String) -> Unit
) {
    val isCashIn = type == "CASH_IN"
    var amountInput by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isCashIn) "Pencatatan Kas Masuk (+)" else "Pencatatan Kas Keluar (-)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isCashIn) "Catat penambahan uang tunai ke dalam laci (misal: tambah modal kecil, dll)." else "Catat pengeluaran tunai dari laci (misal: beli es batu, parkir, dll).",
                    fontSize = 12.sp,
                    color = BrandGray
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal (Rp) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Alasan / Keterangan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0 && reasonInput.isNotBlank()) {
                        onConfirm(amt, reasonInput.trim())
                    }
                },
                enabled = (amountInput.toDoubleOrNull() ?: 0.0) > 0.0 && reasonInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCashIn) BrandToska else MaterialTheme.colorScheme.error
                )
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
