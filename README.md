# AyamQ Kiosk – Android Point of Sale & Order Management App

## Documentation

[Google Drive - Makalah AyamQ Kiosk](https://drive.google.com/file/d/1HvUkbe-NDhbo_xyCZAnNlLHgkFGxqqxD/view?usp=sharing)

**AyamQ Kiosk** adalah aplikasi kasir dan manajemen pesanan berbasis Android yang dirancang khusus untuk kebutuhan usaha makanan skala kecil hingga menengah (UKM). Aplikasi ini menyediakan solusi digital untuk menggantikan pencatatan manual dalam pengelolaan menu, pemrosesan pesanan, dan pelacakan transaksi, serta dilengkapi sistem backup data yang andal. Dibangun dengan arsitektur modern Android, aplikasi ini menjamin konsistensi data, pengalaman pengguna yang optimal, dan performa yang stabil dalam kondisi online maupun offline.

Aplikasi ini dikembangkan sebagai proyek akhir mata kuliah Pemrograman Bergerak dengan mengimplementasikan berbagai komponen dan pola desain Android terkini.

---

## Fitur Utama

### Manajemen Menu & Kategori  
- CRUD (Create, Read, Update, Delete) menu dengan kategori: Paket, Ala Carte, Sauce, Drink.  
- Bulk import data menu dari file JSON menggunakan Gson (from the Default Backup Directory).

### Sistem Pemesanan & Antrian  
- Input pesanan pelanggan dengan antrian berdasarkan nomor antrean (queue number).  
- Monitoring real-time pesanan pending dan completed.  
- Penyelesaian pesanan dengan update status secara instan.

### Backup & Restore Data  
- Backup manual data menu dan pesanan ke file JSON melalui menu admin.  
- Backup otomatis terjadwal menggunakan JobService & JobScheduler.  
- Upload otomatis file backup ke server hosting (cPanel) saat koneksi tersedia.  
- Penyimpanan timestamp backup terakhir menggunakan SharedPreferences.

### ⚙️ Manajemen Aplikasi  
- Konfirmasi dialog untuk aksi destruktif (seperti hapus semua data).  
- Error handling untuk berbagai skenario: jaringan, penyimpanan, dan validasi data.  
- Tampilan antarmuka yang responsif dan intuitif dengan prinsip Material Design.

---

## Teknologi yang Digunakan

### Bahasa & Platform  
- **Kotlin** – Bahasa pemrograman utama yang aman dan ekspresif.  
- **Android SDK** – Platform pengembangan aplikasi Android.

### Arsitektur & Pola Desain  
- **MVVM (Model-View-ViewModel)** – Pemisahan logika UI dan bisnis.  
- **Repository Pattern** – Abstraksi pengelolaan sumber data lokal dan jaringan.  
- **LiveData** – Komponen lifecycle-aware untuk observasi data real-time.  
- **ViewModel** – Pengelolaan data UI yang bertahan pada perubahan konfigurasi.

### Penyimpanan & Data  
- **Room Database (SQLite)** – Penyimpanan data lokal terstruktur dengan dukungan kompilasi waktu.  
- **SharedPreferences** – Penyimpanan key-value untuk preferensi dan konfigurasi sederhana.  
- **JSON (Gson)** – Serialisasi dan deserialisasi data untuk proses backup/restore.

### Asynchronous & Background Processing  
- **Coroutines** – Operasi asynchronous dan background task yang efisien.  
- **JobService & JobScheduler** – Penjadwalan tugas background dengan optimasi baterai.  
- **Broadcast Receiver** – Respons terhadap event sistem (misal perubahan koneksi jaringan).

### UI/UX  
- **Material Design** – Sistem desain untuk tampilan yang konsisten, intuitif, dan modern.

---

## Konsep Pemrograman Bergerak yang Diimplementasikan

- **Background Task Management** – Menggunakan Coroutines menggantikan AsyncTask untuk operasi non-blocking.  
- **Background Task dengan Ketergantungan Jaringan** – Upload backup hanya saat koneksi internet tersedia.  
- **Broadcast Receiver** – Mendeteksi perubahan status jaringan untuk trigger sinkronisasi.  
- **Service & Job Scheduling** – Implementasi JobService untuk backup otomatis terjadwal.  
- **Alarms & Schedulers** – Penjadwalan tugas menggunakan JobScheduler dengan batasan sistem.  
- **Efficient Data Transfer** – Kompresi dan transfer data batch dalam format JSON.  
- **Shared Preferences & Settings** – Penyimpanan pengaturan aplikasi untuk Admin Menu.  
- **Local Data Persistence** – Penyimpanan data transaksi menggunakan Room (SQLite).  
- **Reactive UI dengan Room + LiveData** – Update UI otomatis saat data berubah.  
- **Lifecycle-aware Components dengan ViewModel** – Pengelolaan data UI yang survive pada rotasi layar.

---

## Cara Menjalankan Aplikasi

1. Clone repository ini ke dalam Android Studio.  
2. Sync project dan pastikan semua dependency terunduh dengan benar.  
3. Jalankan aplikasi pada emulator Android Tablet atau perangkat Android Tablet fisik (min. API level 21).  
4. Akses Menu Admin untuk:  
   - Mengelola data menu (tambah, edit, hapus).  
   - Import data menu dari file JSON.  
   - Melihat dan menyelesaikan pesanan.  
   - Melakukan backup data secara manual.  
5. Backup otomatis akan berjalan sesuai jadwal yang telah ditentukan via JobScheduler.

---

## Demo Video

[![Demo Aplikasi AyamQ](https://img.youtube.com/vi/dY4P1IYFA0M/maxresdefault.jpg)](https://www.youtube.com/watch?v=dY4P1IYFA0M)

_Klik gambar di atas > menonton demo video aplikasi AyamQ Kiosk di YouTube untuk melihat fitur dan cara kerja aplikasi secara langsung._

---

## Studi Kasus: E-Business (Retail / Kiosk Makanan)

Aplikasi ini dikembangkan berdasarkan studi kasus nyata pada sektor UKM makanan dengan fokus pada:  
- Digitalisasi proses bisnis dari pencatatan manual ke sistem digital.  
- Pengelolaan data transaksi secara lokal yang andal dan terstruktur.  
- Pemrosesan pesanan secara real-time untuk meningkatkan efisiensi pelayanan.  
- Otomatisasi backup data untuk mencegah kehilangan informasi penting.  
- Membangun aplikasi yang dapat beroperasi penuh dalam kondisi offline maupun online.

---

## Catatan Penting

- Aplikasi dirancang untuk berfungsi tanpa koneksi internet (fully offline-capable).  
- Upload backup ke server hanya dilakukan ketika perangkat terhubung ke jaringan.  
- Data tetap aman tersimpan secara lokal meskipun proses upload mengalami kegagalan.  
- Proyek ini murni dikembangkan untuk tujuan akademis tanpa menggunakan web scraping atau data dari pihak ketiga.

---

## License

Proyek ini dibuat untuk tujuan akademis dan pembelajaran mata kuliah Pemrograman Bergerak.

---

## Credits

**Nama:** Muhammad Alif Rido  
**NIM:** 230401010006  
**Kampus:** Universitas Siber Asia, Jakarta  
**Mata Kuliah:** Pemrograman Bergerak  
**Dosen Pengampu:** Ir. Ahmad Chusyairi, M.Kom., CDS., IPM., ASEAN Eng
