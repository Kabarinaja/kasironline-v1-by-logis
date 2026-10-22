# KASIR DUNIA — Aplikasi Kasir Offline Profesional

Aplikasi kasir (Point of Sale) profesional untuk UMKM, ritel, kuliner, dan bisnis, dirancang untuk berjalan **100% offline** di perangkat HP maupun tablet Android.

Dikembangkan oleh **Logis Denis Prabowo**.

---

## Fitur Utama

1. **Keamanan Login PIN Offline (6 Digit)**:
   - Pembuatan dan konfirmasi PIN saat pertama kali dipasang.
   - Hashing kriptografis aman (SHA-256 + Salt acak) disimpan lokal di perangkat via DataStore.
   - Proteksi anti-brute force (cooldown delay otomatis setelah 5 kali percobaan salah).
   - Fitur ganti PIN di menu pengaturan dengan verifikasi PIN lama.

2. **Onboarding & Profil Usaha**:
   - Pengaturan awal nama usaha, pemilik, alamat, telepon, format struk, dan ukuran kertas.
   - Seluruh biodata dapat disunting kapan saja melalui menu Pengaturan.

3. **Dashboard Real-time**:
   - Total penjualan hari ini, jumlah transaksi, total pembayaran tunai, dan total QRIS dihitung langsung dari Room SQLite database lokal.
   - Indikator peringatan stok menipis secara real-time.
   - Status sesi kasir yang sedang berjalan.
   - Akses cepat transaksi kasir.

4. **Transaksi Kasir Cepat (POS)**:
   - Pencarian instan berdasarkan nama produk, SKU, atau pemindaian Barcode.
   - Filter kategori produk.
   - Keranjang belanja interaktif (tambah, kurangi, diskon item, diskon transaksi, dan pajak PPN).
   - Antarmuka adaptif: split-screen side-by-side untuk tablet/landscape dan layout ringkas untuk HP.
   - Pembayaran **TUNAI** dengan tombol uang pas/nominal cepat dan perhitungan kembalian otomatis.
   - Pembayaran **QRIS** dengan tampilan QR, verifikasi kasir, dan pencatatan nomor referensi / RRN.

5. **Manajemen Kasir & Crew**:
   - Mendukung multi-kasir dengan jabatan (Owner, Kasir, Karyawan).
   - Pilihan kasir bertugas sebelum memulai transaksi dan tercatat permanen pada struk audit.

6. **Sesi Kasir & Uang Laci (Cash Drawer)**:
   - Pencatatan modal awal laci sebelum berjualan.
   - Pencatatan kas masuk (+) dan kas keluar (-) dengan alasan pengeluaran.
   - Perhitungan otomatis estimasi uang fisik di laci: `Modal Awal + Penjualan Tunai + Kas Masuk - Kas Keluar`.
   - Hitung selisih kas saat tutup sesi kasir.

7. **Manajemen Produk & Persediaan (Stok)**:
   - Katalog produk lengkap (Nama, SKU, Barcode, Kategori, Harga Modal, Harga Jual, Stok, Satuan, Batas Minimum).
   - Penyesuaian stok (Masuk, Keluar, Koreksi) dengan pencatatan riwayat audit stok.
   - Ekspor dan impor katalog produk via CSV.
   - Soft-delete untuk menjaga keutuhan riwayat transaksi lama.

8. **Pengaturan Format Struk & Printer Thermal**:
   - Dukungan ukuran kertas: 58mm, 80mm, dan 102mm.
   - Pratinjau struk visual monospace sebelum dicetak.
   - Integrasi Bluetooth Thermal Printer ESC/POS (Classic SPP).
   - Uji cetak printer (Test Print).
   - Opsi pencetakan QR Code promosi / website di bagian footer struk.

9. **Laporan Penjualan & Ekspor PDF Offline**:
   - Filter periode: Hari ini, Kemarin, 7 Hari Terakhir, Bulan Ini.
   - Pemisahan jelas antara pembayaran Tunai dan QRIS.
   - Daftar produk terlaris berdasarkan kuantitas dan nilai pendapatan.
   - Penjualan berdasarkan kasir bertugas.
   - Ekspor laporan ke dokumen PDF profesional secara **100% OFFLINE** menggunakan Android Graphics PDF.

10. **Cadangan & Pemulihan (Backup & Restore Offline)**:
    - Ekspor seluruh basis data ke berkas JSON terenkapsulasi.
    - Impor berkas cadangan dengan validasi struktur dan konfirmasi keamanan.

11. **Dukungan Bahasa (Bilingual)**:
    - Bahasa Indonesia (Default).
    - English.

---

## Panduan Membuka, Membangun, dan Menguji Proyek

### Persyaratan Lingkungan
- **Android Studio**: Jellyfish (2023.3.1) / Koala / Ladybug atau yang lebih baru.
- **JDK**: JDK 17 atau JDK 21.
- **Android SDK**: Compile SDK 36, Minimum SDK 24 (Android 7.0 Nougat ke atas).

### Cara Membangun Proyek (Gradle)

1. **Kompilasi dan Periksa Kode**:
   ```bash
   gradle compile_applet
   ```

2. **Jalankan Pengujian Unit & Robolectric**:
   ```bash
   gradle :app:testDebugUnitTest
   ```

3. **Membangun APK Debug**:
   ```bash
   gradle :app:assembleDebug
   ```
   Berkas APK hasil build akan berada di direktori:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Kontak Pengembang

- **Nama**: Logis Denis Prabowo
- **Email**: kasirdunia.com@gmail.com
- **WhatsApp**: 0823-7947-4173
