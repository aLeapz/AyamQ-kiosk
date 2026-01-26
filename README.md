# AyamQ Kiosk – Android Point of Sale & Order Management App

AyamQ Kiosk adalah aplikasi kasir dan manajemen pesanan berbasis Android yang dirancang untuk kebutuhan usaha makanan (kiosk/retail).
Aplikasi ini mendukung pengelolaan menu, pemrosesan pesanan, monitoring order secara real-time, serta sistem backup data otomatis dan manual.

Aplikasi ini dikembangkan menggunakan **Android Studio** sebagai project tugas mata kuliah **Pemrograman Bergerak**.

---

## Fitur Utama

- Manajemen menu (tambah, edit, hapus, dan bulk import dari JSON)
- Kategori menu (Ala Carte, Paket, Minuman, Saus, dll)
- Sistem pemesanan dan antrian (queue number)
- Monitoring pesanan (pending & completed orders)
- Penyelesaian pesanan secara real-time
- Backup data menu & pesanan ke file JSON
- Upload backup ke server (cPanel hosting)
- Backup otomatis menggunakan JobScheduler
- Backup manual melalui menu admin
- Penyimpanan waktu backup terakhir
- Konfirmasi dialog untuk aksi destruktif (delete all)
- Error handling untuk kondisi jaringan dan data

---

## Teknologi yang Digunakan

- **Kotlin**
- **Android SDK**
- **Room (SQLite)**
- **LiveData**
- **ViewModel**
- **Repository Pattern**
- **Coroutines**
- **JobService & JobScheduler**
- **Broadcast Receiver**
- **SharedPreferences**
- **JSON (Gson)**
- **Material Design**

---

## Konsep yang Diimplementasikan (Sesuai Soal)

Aplikasi ini memenuhi dan mengimplementasikan konsep-konsep berikut:

- Background Task (Coroutine sebagai pengganti AsyncTask)
- Background Task – Internet Connection
- Broadcast Receiver
- Service (JobService)
- Alarms & Schedulers (JobScheduler)
- Efficient Data Transfer (batch JSON upload)
- Shared Preferences & Settings
- Penyimpanan data menggunakan Room (SQLite)
- Room + LiveData
- ViewModel

---

## Cara Menjalankan Aplikasi

1. Clone repository ini ke Android Studio
2. Sync Gradle dan pastikan dependency terunduh
3. Jalankan aplikasi di emulator atau perangkat fisik
4. Gunakan menu admin untuk:
   - Menambah menu
   - Import menu dari file JSON
   - Melihat dan mengelola pesanan
   - Melakukan backup manual
5. Backup otomatis akan berjalan sesuai jadwal menggunakan JobScheduler

---

## Studi Kasus

Aplikasi ini menggunakan studi kasus **E-Business (Retail / Kiosk Makanan)** dengan fokus pada:

- Pengelolaan data transaksi secara lokal
- Pemrosesan pesanan secara real-time
- Otomatisasi backup data
- Keandalan aplikasi dalam kondisi offline dan online

---

## Catatan

- Aplikasi dapat berjalan **tanpa koneksi internet**
- Upload backup ke server hanya dilakukan jika jaringan tersedia
- Data tetap aman tersimpan secara lokal meskipun upload gagal
- Proyek ini tidak menggunakan scraping atau data eksternal pihak ketiga

---

## License

This project was created for academic purposes only.

---

## Credits

**Nama**: Muhammad Alif Rido  
**NIM**: 230401010006  
**Kampus**: Universitas Siber Asia  
**Mata Kuliah**: Pemrograman Bergerak  
**Dosen Pengampu**:  
Ir. Ahmad Chusyairi, M.Kom., CDS., IPM., ASEAN Eng
