package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tentang Aplikasi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_kasirdunia),
                        contentDescription = "Logo KASIR DUNIA",
                        modifier = Modifier.height(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "KASIR DUNIA — Aplikasi Kasir Offline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandToskaDark
                    )
                    Text(
                        text = "Versi 1.0 (Produksi Offline)",
                        fontSize = 12.sp,
                        color = BrandGray
                    )
                }
            }

            // Description Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tentang KASIR DUNIA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandToska
                    )
                    Text(
                        text = "KASIR DUNIA merupakan aplikasi kasir yang dirancang untuk membantu pelaku usaha mengelola transaksi penjualan, produk, persediaan, laporan, serta pencatatan kas secara praktis melalui perangkat Android.\n\nAplikasi ini mengutamakan penggunaan offline dengan penyimpanan data lokal pada perangkat pengguna. Fitur yang tersedia meliputi pengelolaan produk, transaksi tunai dan QRIS, laporan penjualan, pengaturan struk, dukungan printer thermal yang kompatibel, serta pencadangan dan pemulihan data.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = BrandGrayDark
                    )
                }
            }

            // Developer Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pengembang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandToska
                    )
                    Text(
                        text = "Aplikasi ini dikembangkan oleh Logis Denis Prabowo dengan dukungan teknologi kecerdasan buatan (AI) dalam proses perancangan dan pengembangan. Pengembangan dilakukan dengan tujuan menyediakan solusi kasir yang praktis, mudah digunakan, dan dapat diakses oleh pelaku usaha.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = BrandGrayDark
                    )
                }
            }

            // Developer Contact Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Kontak Pengembang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandToska
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Email", fontSize = 11.sp, color = BrandGray)
                            Text("kasirdunia.com@gmail.com", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = BrandToska, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("WhatsApp", fontSize = 11.sp, color = BrandGray)
                            Text("0823-7947-4173 (Logis Denis Prabowo)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Divider(color = BrandGrayBorder)

                    Text(
                        text = "“Untuk pertanyaan, masukan, laporan kendala, atau informasi mengenai pembaruan aplikasi, silakan menghubungi pengembang melalui kontak di atas.”",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = BrandGrayDark,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
