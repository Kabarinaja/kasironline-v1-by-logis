package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.entity.Crew
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    modifier: Modifier = Modifier
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var crewToEdit by remember { mutableStateOf<Crew?>(null) }
    var crewToDelete by remember { mutableStateOf<Crew?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manajemen Kasir & Crew",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    crewToEdit = null
                    showAddEditDialog = true
                },
                containerColor = BrandOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Tambah Crew", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_crew")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Daftar Kasir & Tim Usaha",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Kasir yang terdaftar akan muncul pada pilihan sebelum sesi dibuka dan dicantumkan pada setiap struk transaksi.",
                    fontSize = 12.sp,
                    color = BrandGray
                )
            }

            items(uiState.activeCrew) { crew ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(crew.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (crew.role.equals("Owner", ignoreCase = true)) BrandOrangeLight else BrandToskaLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = crew.role,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (crew.role.equals("Owner", ignoreCase = true)) BrandOrangeDark else BrandToskaDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (crew.phone.isNotEmpty()) {
                                Text("Telp: ${crew.phone}", fontSize = 12.sp, color = BrandGray)
                            }
                            if (crew.notes.isNotEmpty()) {
                                Text(crew.notes, fontSize = 11.sp, color = BrandGray)
                            }
                        }

                        Row {
                            IconButton(onClick = {
                                crewToEdit = crew
                                showAddEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandGray)
                            }
                            IconButton(onClick = {
                                crewToDelete = crew
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditCrewDialog(
            crew = crewToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedCrew ->
                viewModel.saveCrew(updatedCrew) {
                    showAddEditDialog = false
                }
            }
        )
    }

    if (crewToDelete != null) {
        val cr = crewToDelete!!
        AlertDialog(
            onDismissRequest = { crewToDelete = null },
            title = { Text("Hapus Crew?") },
            text = { Text("Yakin ingin menonaktifkan '${cr.name}'? Riwayat transaksi lama kasir ini tetap tersimpan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCrew(cr.id) {
                            crewToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { crewToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun AddEditCrewDialog(
    crew: Crew?,
    onDismiss: () -> Unit,
    onSave: (Crew) -> Unit
) {
    var name by remember { mutableStateOf(crew?.name ?: "") }
    var role by remember { mutableStateOf(crew?.role ?: "Kasir") }
    var phone by remember { mutableStateOf(crew?.phone ?: "") }
    var notes by remember { mutableStateOf(crew?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (crew == null) "Tambah Crew Baru" else "Ubah Data Crew", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Jabatan (Owner / Kasir / Karyawan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newCrew = Crew(
                            id = crew?.id ?: 0L,
                            name = name.trim(),
                            role = if (role.isNotBlank()) role.trim() else "Kasir",
                            phone = phone.trim(),
                            notes = notes.trim()
                        )
                        onSave(newCrew)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
            ) {
                Text("Simpan Crew")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
