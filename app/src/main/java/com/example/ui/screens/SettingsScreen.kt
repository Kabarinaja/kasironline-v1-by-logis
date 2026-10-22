package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.BackupResult
import com.example.data.preferences.BusinessSettings
import com.example.localization.LocalizedStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateToAbout: () -> Unit,
    onNavigateToCrew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val settings = uiState.settings
    val scrollState = rememberScrollState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showReceiptSettingsDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.settings,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Store Identity Overview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditProfileDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = BrandToskaLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandToska, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(settings.businessName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(settings.address, fontSize = 12.sp, color = BrandGray)
                        Text("Pemilik: ${settings.ownerName}", fontSize = 12.sp, color = BrandToska)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandGray)
                }
            }

            // Settings Group: Konfigurasi Kasir
            Text(
                text = "Konfigurasi Transaksi",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrandToska)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Print,
                        title = "Printer Thermal Bluetooth",
                        subtitle = if (settings.preferredPrinterName.isNotEmpty()) settings.preferredPrinterName else "Belum terhubung",
                        onClick = {
                            viewModel.refreshBluetoothPrinters()
                            showPrinterDialog = true
                        }
                    )
                    Divider(color = BrandGrayBorder, thickness = 0.5.dp)
                    SettingsItem(
                        icon = Icons.Default.Receipt,
                        title = "Pengaturan Format Struk",
                        subtitle = "Kertas ${settings.paperSize} • Footer & Promo QR",
                        onClick = { showReceiptSettingsDialog = true }
                    )
                    Divider(color = BrandGrayBorder, thickness = 0.5.dp)
                    SettingsItem(
                        icon = Icons.Default.Group,
                        title = "Manajemen Kasir & Crew",
                        subtitle = "Kelola ${uiState.activeCrew.size} anggota kasir",
                        onClick = onNavigateToCrew
                    )
                }
            }

            // Settings Group: Keamanan & Data
            Text(
                text = "Keamanan & Penyimpanan",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrandToska)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Ganti 6 Digit PIN Kasir",
                        subtitle = "Perbarui PIN keamanan akses lokal",
                        onClick = { showChangePinDialog = true }
                    )
                    Divider(color = BrandGrayBorder, thickness = 0.5.dp)
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Cadangkan & Pulihkan (Backup / Restore)",
                        subtitle = "Ekspor / impor database JSON & CSV offline",
                        onClick = { showBackupRestoreDialog = true }
                    )
                    Divider(color = BrandGrayBorder, thickness = 0.5.dp)
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = strings.language,
                        subtitle = if (uiState.language == "en") "English" else "Bahasa Indonesia",
                        onClick = {
                            val next = if (uiState.language == "id") "en" else "id"
                            viewModel.setLanguage(next)
                        }
                    )
                }
            }

            // Settings Group: Informasi
            Text(
                text = "Informasi Aplikasi",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrandToska)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Tentang KASIR DUNIA",
                        subtitle = "Versi 1.0 • Pengembang Logis Denis Prabowo",
                        onClick = onNavigateToAbout
                    )
                    Divider(color = BrandGrayBorder, thickness = 0.5.dp)
                    SettingsItem(
                        icon = Icons.Default.LockClock,
                        title = "Kunci Aplikasi Kasir",
                        subtitle = "Kembali ke layar kunci PIN",
                        onClick = { viewModel.lockTerminal() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Edit Business Profile Dialog
    if (showEditProfileDialog) {
        EditBusinessProfileDialog(
            settings = settings,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showEditProfileDialog = false
            }
        )
    }

    // Printer Setup Dialog
    if (showPrinterDialog) {
        PrinterSetupDialog(
            pairedPrinters = uiState.pairedPrinters,
            selectedAddress = settings.preferredPrinterAddress,
            onDismiss = { showPrinterDialog = false },
            onSelectPrinter = { device ->
                val updated = settings.copy(
                    preferredPrinterAddress = device.address,
                    preferredPrinterName = device.name
                )
                viewModel.updateSettings(updated)
            },
            onTestPrint = { address ->
                viewModel.testPrint(address) { success, msg ->
                    viewModel.showToast(msg)
                }
            }
        )
    }

    // Receipt Format Settings Dialog
    if (showReceiptSettingsDialog) {
        ReceiptFormatDialog(
            settings = settings,
            onDismiss = { showReceiptSettingsDialog = false },
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showReceiptSettingsDialog = false
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onConfirm = { oldPin, newPin ->
                viewModel.changePin(oldPin, newPin) { success, msg ->
                    viewModel.showToast(msg)
                    if (success) showChangePinDialog = false
                }
            }
        )
    }

    // Backup & Restore Dialog
    if (showBackupRestoreDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupRestoreDialog = false }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = BrandToska, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = BrandGray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandGrayBorder)
    }
}

@Composable
fun EditBusinessProfileDialog(
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onSave: (BusinessSettings) -> Unit
) {
    var businessName by remember { mutableStateOf(settings.businessName) }
    var ownerName by remember { mutableStateOf(settings.ownerName) }
    var businessType by remember { mutableStateOf(settings.businessType) }
    var address by remember { mutableStateOf(settings.address) }
    var cityProvince by remember { mutableStateOf(settings.cityProvince) }
    var phone by remember { mutableStateOf(settings.phone) }
    var email by remember { mutableStateOf(settings.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Profil Usaha", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Nama Usaha") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Nama Pemilik") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = businessType, onValueChange = { businessType = it }, label = { Text("Jenis Usaha") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cityProvince, onValueChange = { cityProvince = it }, label = { Text("Kota & Provinsi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Usaha") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            businessName = businessName.trim(),
                            ownerName = ownerName.trim(),
                            businessType = businessType.trim(),
                            address = address.trim(),
                            cityProvince = cityProvince.trim(),
                            phone = phone.trim(),
                            email = email.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun PrinterSetupDialog(
    pairedPrinters: List<com.example.printer.PrinterDeviceInfo>,
    selectedAddress: String,
    onDismiss: () -> Unit,
    onSelectPrinter: (com.example.printer.PrinterDeviceInfo) -> Unit,
    onTestPrint: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Printer Thermal", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Aplikasi mendukung printer thermal ESC/POS melalui koneksi Bluetooth. Pastikan printer telah dipasangkan (paired) di pengaturan Bluetooth Android.",
                    fontSize = 12.sp,
                    color = BrandGray
                )

                if (pairedPrinters.isEmpty()) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tidak ada perangkat Bluetooth yang terpasang. Silakan hidupkan printer thermal dan pasangkan di Pengaturan Bluetooth HP/Tablet Anda.",
                            fontSize = 12.sp,
                            color = BrandOrangeDark,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Text("Pilih Printer Thermal:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    pairedPrinters.forEach { device ->
                        val isSelected = device.address == selectedAddress
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrandToskaLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPrinter(device) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(device.address, fontSize = 11.sp, color = BrandGray)
                                }
                                if (isSelected) {
                                    Button(
                                        onClick = { onTestPrint(device.address) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandToska),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Uji Cetak", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun ReceiptFormatDialog(
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onSave: (BusinessSettings) -> Unit
) {
    var paperSize by remember { mutableStateOf(settings.paperSize) }
    var showHeaderInfo by remember { mutableStateOf(settings.showHeaderInfo) }
    var showCashierInfo by remember { mutableStateOf(settings.showCashierInfo) }
    var footerText by remember { mutableStateOf(settings.receiptFooter) }
    var showPromoQr by remember { mutableStateOf(settings.showPromoQr) }
    var promoQrText by remember { mutableStateOf(settings.promoQrText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Format Struk Pembayaran", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ukuran Lebar Kertas:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("58mm", "80mm", "102mm").forEach { size ->
                        FilterChip(
                            selected = paperSize == size,
                            onClick = { paperSize = size },
                            label = { Text(size) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandToska, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan Info Toko & Alamat", fontSize = 13.sp)
                    Switch(checked = showHeaderInfo, onCheckedChange = { showHeaderInfo = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan Nama Kasir", fontSize = 13.sp)
                    Switch(checked = showCashierInfo, onCheckedChange = { showCashierInfo = it })
                }

                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    label = { Text("Pesan Penutup Struk (Footer)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cetak QR Promo / Website di Struk", fontSize = 13.sp)
                    Switch(checked = showPromoQr, onCheckedChange = { showPromoQr = it })
                }

                if (showPromoQr) {
                    OutlinedTextField(
                        value = promoQrText,
                        onValueChange = { promoQrText = it },
                        label = { Text("Tautan Website / Teks Promo QR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            paperSize = paperSize,
                            showHeaderInfo = showHeaderInfo,
                            showCashierInfo = showCashierInfo,
                            receiptFooter = footerText.trim(),
                            showPromoQr = showPromoQr,
                            promoQrText = promoQrText.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
            ) {
                Text("Simpan Format")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (oldPin: String, newPin: String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti PIN Kasir", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 6) oldPin = it.filter { c -> c.isDigit() } },
                    label = { Text("PIN Lama (6 Digit) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it.filter { c -> c.isDigit() } },
                    label = { Text("PIN Baru (6 Digit) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 6) confirmNewPin = it.filter { c -> c.isDigit() } },
                    label = { Text("Konfirmasi PIN Baru *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(errorText ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length != 6) {
                        errorText = "PIN baru harus 6 digit angka!"
                    } else if (newPin != confirmNewPin) {
                        errorText = "Konfirmasi PIN baru tidak cocok!"
                    } else {
                        onConfirm(oldPin, newPin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
            ) {
                Text("Ganti PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun BackupRestoreDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pencadangan & Pemulihan Offline", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Seluruh data transaksi, produk, crew, sesi, dan pengaturan tersimpan lokal di perangkat ini. Buat cadangan berkala agar data dapat dipindahkan ke perangkat lain.",
                    fontSize = 12.sp,
                    color = BrandGray
                )

                Button(
                    onClick = {
                        viewModel.backupData { result ->
                            when (result) {
                                is BackupResult.Success -> {
                                    statusMessage = result.message
                                    viewModel.shareBackupFile(result.file, "application/json", "Cadangan Data KASIR DUNIA")
                                }
                                is BackupResult.Error -> statusMessage = result.error
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandToska),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor Cadangan (JSON)")
                }

                Button(
                    onClick = {
                        viewModel.exportCsv { file ->
                            viewModel.shareBackupFile(file, "text/csv", "Ekspor Katalog Produk KASIR DUNIA")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor Produk ke CSV")
                }

                if (statusMessage != null) {
                    Text(statusMessage ?: "", color = BrandToska, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
