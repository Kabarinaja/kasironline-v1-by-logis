package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.LocalizedStrings
import com.example.ui.components.NumericKeypad
import com.example.ui.components.PinDots
import com.example.ui.theme.BrandGray
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.BrandToska
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@Composable
fun PinAuthScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val isInitialSetup = !uiState.isPinSet

    var step by remember { mutableStateOf(if (isInitialSetup) "create" else "login") }
    var enteredPin by remember { mutableStateOf("") }
    var firstPinCandidate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    val handleDigit: (String) -> Unit = { digit ->
        if (uiState.lockoutSeconds <= 0 && enteredPin.length < 6) {
            val updated = enteredPin + digit
            enteredPin = updated
            errorMessage = null

            if (updated.length == 6) {
                when (step) {
                    "login" -> {
                        viewModel.submitPin(updated) { success, error ->
                            if (success) {
                                onSuccess()
                            } else {
                                enteredPin = ""
                                errorMessage = error
                            }
                        }
                    }
                    "create" -> {
                        firstPinCandidate = updated
                        enteredPin = ""
                        step = "confirm"
                    }
                    "confirm" -> {
                        if (updated == firstPinCandidate) {
                            viewModel.setupInitialPin(updated) {
                                onSuccess()
                            }
                        } else {
                            enteredPin = ""
                            firstPinCandidate = ""
                            step = "create"
                            errorMessage = strings.pinMismatch
                        }
                    }
                }
            }
        }
    }

    val handleDelete: () -> Unit = {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_kasirdunia),
                    contentDescription = "Logo KASIR DUNIA",
                    modifier = Modifier
                        .height(54.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = when (step) {
                        "create" -> strings.createPin
                        "confirm" -> strings.confirmPin
                        else -> strings.enterPin
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Text(
                    text = if (isInitialSetup) {
                        if (step == "create") "Amankan data transaksi kasir lokal perangkat Anda" else "Ketik ulang PIN untuk verifikasi"
                    } else {
                        "Aplikasi Kasir Offline Profesional"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = BrandGray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // PIN Dots and Error / Lockout Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                PinDots(pinLength = enteredPin.length)

                if (uiState.lockoutSeconds > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Akses ditunda: coba lagi dalam ${uiState.lockoutSeconds} detik",
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Keypad
            NumericKeypad(
                onDigitClick = handleDigit,
                onDeleteClick = handleDelete,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Footer note & Forgot PIN button
            if (!isInitialSetup) {
                TextButton(
                    onClick = { showForgotPinDialog = true },
                    modifier = Modifier.testTag("forgot_pin_button")
                ) {
                    Text(
                        text = "Lupa PIN Kasir?",
                        color = BrandToska,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BrandOrange
                )
            },
            title = {
                Text("Informasi Keamanan PIN Offline")
            },
            text = {
                Text(
                    "KASIR DUNIA adalah aplikasi 100% offline. Demi privasi, PIN tersimpan secara kriptografis di perangkat dan tidak pernah dikirim ke server online mana pun.\n\nJika lupa PIN, Anda dapat memulihkan akses dengan memasukkan berkas cadangan (backup JSON) terakhir atau melakukan reset data melalui pengaturan aplikasi Android (Hapus Data Penyimpanan)."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showForgotPinDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
                ) {
                    Text("Mengerti")
                }
            }
        )
    }
}
