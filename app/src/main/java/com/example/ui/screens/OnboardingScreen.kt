package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.BusinessSettings
import com.example.localization.LocalizedStrings
import com.example.ui.theme.BrandGray
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.BrandToska
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val scrollState = rememberScrollState()

    var ownerName by remember { mutableStateOf(uiState.settings.ownerName) }
    var businessName by remember { mutableStateOf(uiState.settings.businessName) }
    var businessType by remember { mutableStateOf(uiState.settings.businessType) }
    var address by remember { mutableStateOf(uiState.settings.address) }
    var cityProvince by remember { mutableStateOf(uiState.settings.cityProvince) }
    var phone by remember { mutableStateOf(uiState.settings.phone) }
    var email by remember { mutableStateOf(uiState.settings.email) }
    var paperSize by remember { mutableStateOf(uiState.settings.paperSize) }
    var receiptFooter by remember { mutableStateOf(uiState.settings.receiptFooter) }
    var language by remember { mutableStateOf(uiState.language) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan Awal Usaha",
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_kasirdunia),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                    )
                }
            }

            Text(
                text = "Selamat datang di KASIR DUNIA! Lengkapi identitas usaha Anda untuk format struk belanja dan laporan penjualan. Seluruh data tersimpan aman secara offline di perangkat ini.",
                style = MaterialTheme.typography.bodyMedium.copy(color = BrandGray, lineHeight = 20.sp)
            )

            // Form Inputs
            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Nama Toko / Usaha *") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandToska) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_business_name"),
                singleLine = true
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Nama Pemilik / Penanggung Jawab *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandToska) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_owner_name"),
                singleLine = true
            )

            OutlinedTextField(
                value = businessType,
                onValueChange = { businessType = it },
                label = { Text("Jenis Usaha (misal: Kuliner, Retail, Jasa, Toko)") },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = BrandToska) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat Usaha") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandToska) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cityProvince,
                onValueChange = { cityProvince = it },
                label = { Text("Kota & Provinsi") },
                leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = BrandToska) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandToska) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Usaha (Opsional)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandToska) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Paper Size Selection
            Text(
                text = "Ukuran Kertas Printer Thermal",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("58mm", "80mm", "102mm").forEach { size ->
                    val selected = paperSize == size
                    FilterChip(
                        selected = selected,
                        onClick = { paperSize = size },
                        label = { Text(size, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandToska,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Language Selection
            Text(
                text = "Bahasa Aplikasi (Language)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("id" to "Bahasa Indonesia", "en" to "English").forEach { (code, name) ->
                    val selected = language == code
                    FilterChip(
                        selected = selected,
                        onClick = {
                            language = code
                            viewModel.setLanguage(code)
                        },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandToska,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = receiptFooter,
                onValueChange = { receiptFooter = it },
                label = { Text("Pesan Kaki Struk (Footer)") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = BrandToska) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (businessName.isNotBlank() && ownerName.isNotBlank()) {
                        val newSettings = uiState.settings.copy(
                            ownerName = ownerName.trim(),
                            businessName = businessName.trim(),
                            businessType = businessType.trim(),
                            address = address.trim(),
                            cityProvince = cityProvince.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            paperSize = paperSize,
                            receiptFooter = receiptFooter.trim(),
                            language = language
                        )
                        viewModel.completeOnboarding(newSettings)
                        onComplete()
                    }
                },
                enabled = businessName.isNotBlank() && ownerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_submit_button")
            ) {
                Text(
                    text = "Mulai Menggunakan Kasir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}
