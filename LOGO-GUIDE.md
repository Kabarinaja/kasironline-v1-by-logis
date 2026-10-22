# PANDUAN PENGGANTIAN LOGO DAN IKON — KASIR DUNIA

Dokumentasi ini menjelaskan langkah-langkah mengganti logo resmi dan ikon launcher aplikasi **KASIR DUNIA** tanpa mengubah logika aplikasi kasir.

---

## 1. Lokasi & Nama File Resource

| Tipe Logo | Lokasi Folder | Nama File | Format yang Disarankan | Ukuran yang Disarankan |
| :--- | :--- | :--- | :--- | :--- |
| **Ikon Launcher (Utama)** | `app/src/main/res/drawable/` | `ic_launcher_foreground.xml` | Vector Drawable (XML) atau PNG Layer List | 108 x 108 dp (Elemen aman 66 x 66 dp) |
| **Warna Latar Ikon** | `app/src/main/res/drawable/` | `ic_launcher_background.xml` | Vector Drawable (XML) | Warna solid `#0D8B96` atau gradien |
| **Logo Dalam Aplikasi** | `app/src/main/res/drawable/` | `logo_kasirdunia.xml` (atau `.png`) | Vector Drawable (XML) atau PNG Transparan | 400 x 120 px (Rasio ~10:3) |
| **Ikon Launcher Raster (Fallback)** | `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/` | `ic_launcher.png` & `ic_launcher_round.png` | PNG 32-bit RGBA | mdpi: 48x48, hdpi: 72x72, xhdpi: 96x96, xxhdpi: 144x144, xxxhdpi: 192x192 |

---

## 2. Cara Mengganti Logo Dalam Aplikasi (`logo_kasirdunia`)

Logo dalam aplikasi ditampilkan pada header onboarding, halaman login PIN, sidebar navigasi tablet, struk belanja (jika dicetak), dan halaman Tentang Aplikasi.

### Langkah-langkah:
1. Siapkan file logo resmi bisnis Anda (format `.png` transparan atau vector `.xml`).
2. Beri nama file tepat: `logo_kasirdunia.png` (jika PNG) atau `logo_kasirdunia.xml` (jika Vector).
3. Simpan file tersebut ke direktori:
   ```
   app/src/main/res/drawable/logo_kasirdunia.png
   ```
4. Jika sebelumnya ada file `logo_kasirdunia.xml` dan Anda memakai `.png`, hapus file `.xml` agar tidak terjadi duplikasi ID resource di Android AAPT2.
5. Bangun ulang aplikasi menggunakan `gradle assembleDebug` atau buka aplikasi di streaming emulator. Logo baru otomatis tampil di semua layar aplikasi.

---

## 3. Cara Mengganti Ikon Launcher Aplikasi (`ic_launcher`)

Aplikasi Android menggunakan arsitektur **Adaptive Icon** yang terdiri dari dua lapisan:
1. **Background Layer (`ic_launcher_background.xml`)**: Latar belakang warna penuh (default: `#0D8B96` Toska).
2. **Foreground Layer (`ic_launcher_foreground.xml`)**: Lambang / simbol utama di tengah.

### Langkah-langkah cepat:
1. Buka folder `app/src/main/res/drawable/`.
2. Edit atau gantikan `ic_launcher_foreground.xml` dengan simbol logo Anda.
3. Untuk memperbarui ikon raster legacy pada folder mipmap (`mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi`), gunakan Android Studio *Asset Studio* (`Right-click res -> New -> Image Asset`) atau skrip ImageMagick:
   ```bash
   SRC="path/to/official_logo.png"
   BASE="app/src/main/res"
   for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
     dir="$BASE/mipmap-${spec%:*}"
     size="${spec#*:}"
     convert "$SRC" -resize "${size}x${size}!" "PNG32:$dir/ic_launcher.png"
     radius=$((size / 2))
     convert "$SRC" -resize "${size}x${size}!" \
       \( -size "${size}x${size}" xc:none -fill white \
          -draw "circle $radius,$radius $radius,0" \) \
       -alpha set -compose DstIn -composite "PNG32:$dir/ic_launcher_round.png"
   done
   ```

---

## 4. Keamanan & Integritas Kode

- Penggantian file logo dan ikon di folder `res/drawable/` dan `res/mipmap-*/` **tidak akan mengubah atau merusak logika transaksi, database, maupun pengaturan kasir**.
- Semua referensi di Jetpack Compose menggunakan resource ID standar `R.drawable.logo_kasirdunia` dan `R.mipmap.ic_launcher`.
