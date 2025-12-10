# 💰 CashFlow Report App

CashFlow Report App adalah aplikasi Android berbasis **Kotlin** yang dirancang untuk membantu pengguna melacak arus kas (pemasukan dan pengeluaran), mengelola akun keuangan, dan melihat analisis laporan keuangan secara visual.

Project ini dibangun menggunakan arsitektur **MVVM (Model-View-ViewModel)** untuk memastikan kode yang bersih, mudah diuji, dan *scalable*.

## ✨ Fitur Utama

* **Autentikasi Pengguna:** Login, Register, dan Lupa Password (Firebase Auth).
* **Manajemen Transaksi:** Tambah, Edit, dan Lihat riwayat transaksi pemasukan/pengeluaran.
* **Multi-Akun:** Kelola berbagai sumber dana (Dompet, Bank, Tabungan).
* **Laporan & Analitik:**
    * **Analytics:** Visualisasi data pengeluaran.
    * **Recap:** Ringkasan keuangan periode tertentu.
* **Integrasi Cloud:**
    * Upload bukti transaksi/gambar menggunakan **Cloudinary**.
    * Notifikasi real-time (Notification Helper).
* **Profil Pengguna:** Halaman profil yang dapat disesuaikan.

## 🛠️ Teknologi & Library

* **Bahasa:** [Kotlin](https://kotlinlang.org/)
* **UI:** XML Layouts & Material Design
* **Arsitektur:** MVVM (Model-View-ViewModel)
* **Navigasi:** Android Navigation Component
* **Backend / Cloud:**
    * [Firebase](https://firebase.google.com/) (Auth, Analytics, Crashlytics)
    * [Cloudinary](https://cloudinary.com/) (Manajemen Gambar)
* **Network & Async:** Coroutines & Retrofit (jika ada API call)


## 🚀 Cara Menjalankan Project

Untuk menjalankan aplikasi ini di lokal komputer Anda, ikuti langkah berikut:

### Prasyarat
* Android Studio (versi terbaru disarankan).
* JDK 17 atau lebih baru.

### Instalasi

1.  **Clone Repository**
    ```bash
    git clone [https://github.com/Jexxy1517/CashFlowReportApp.git](https://github.com/Jexxy1517/CashFlowReportApp.git)
    ```

2.  **Buka di Android Studio**
    * Buka Android Studio -> File -> Open -> Pilih folder `CashFlowReportApp`.
    * Tunggu hingga proses *Gradle Sync* selesai.

3.  **Konfigurasi Firebase**
    * Pastikan file `google-services.json` sudah ada di dalam folder `app/`.
    * Jika belum, download dari Firebase Console project Anda.

4.  **Konfigurasi Cloudinary**
    * Buka `com/example/cashflowreportapp/Cloudinaryconfig.kt`.
    * Pastikan API Key dan Cloud Name sudah sesuai dengan akun Cloudinary Anda.

5.  **Run App**
    * Hubungkan device Android atau gunakan Emulator.
    * Klik tombol **Run (▶)**.
