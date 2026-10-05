# TRAC (Tradevis Track and Care) 🛠️🏫

> **Sistem Informasi & Aplikasi Mobile Pelaporan Kerusakan Sarana Prasarana Sekolah Berbasis Android & Cloud BaaS (Supabase)**

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Compile%20SDK-37-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Backend](https://img.shields.io/badge/BaaS-Supabase%20(PostgreSQL)-emerald.svg?style=flat&logo=supabase)](https://supabase.com)
[![Engine](https://img.shields.io/badge/Engine-Ktor%20Android%203.0-orange.svg?style=flat&logo=ktor)](https://ktor.io)

---

## 📌 Daftar Isi
1. [Tentang Proyek](#-tentang-proyek)
2. [Peran Pengguna (User Roles & RBAC)](#-peran-pengguna-user-roles--rbac)
3. [Fitur Utama](#-fitur-utama)
4. [Arsitektur Aplikasi (Software Architecture)](#-arsitektur-aplikasi-software-architecture)
5. [Alur Komunikasi Data (Data Flow)](#-alur-komunikasi-data-data-flow)
6. [Tech Stack & Pustaka](#-tech-stack--pustaka)
7. [Skema & Arsitektur Database](#-skema--arsitektur-database)
8. [Struktur Direktori Proyek](#-struktur-direktori-proyek)
9. [Instalasi & Menjalankan Proyek](#-instalasi--menjalankan-proyek)

---

## 📖 Tentang Proyek

**TRAC (*Tradevis Track and Care*)** adalah platform mobile berbasis Android yang dikembangkan untuk mendigitalisasi, menyederhanakan, dan meningkatkan akuntabilitas proses pelaporan serta pemeliharaan fasilitas di lingkungan sekolah.

### Permasalahan yang Diselesaikan:
- **Pelaporan Manual Tercecer**: Menggantikan pelaporan konvensional (lisan/kertas/grup chat) yang lambat dan sering terabaikan.
- **Lokasi Tidak Spesifik**: Mengintegrasikan basis data denah sekolah nyata (4 lantai dan 113 ruangan) agar teknisi langsung mengetahui titik presisi kerusakan.
- **Minimnya Transparansi**: Memberikan akses pelacakan progres secara *real-time* kepada pelapor (*Pending* $\rightarrow$ *In Progress* $\rightarrow$ *Completed*).
- **Akuntabilitas Teknisi**: Menyediakan pencatatan bukti fisik foto sebelum (*before*) dan setelah (*after*) perbaikan selesai.

---

## 👥 Peran Pengguna (User Roles & RBAC)

Aplikasi TRAC membagi hak akses pengguna berdasarkan peran (*Role-Based Access Control*):

| Peran | Deskripsi | Hak Akses Utama |
|---|---|---|
| **Siswa / Pelapor** (*Student*) | Pengguna umum / peserta didik di lingkungan sekolah. | • Mendaftar & login akun siswa.<br>• Membuat laporan kerusakan fasilitas (kategori, lokasi, foto, urgensi).<br>• Memberikan *upvote* pada laporan fasilitas publik yang rusak.<br>• Memantau status laporan secara berkala.<br>• Melihat bukti foto & catatan penyelesaian teknisi. |
| **Admin Sarpras** (*Facility Admin*) | Pengelola fasilitas, sarana, dan prasarana sekolah. | • Mengakses **Admin Dashboard** khusus.<br>• Memantau statistik laporan (Total, Aktif, Selesai, Mendesak).<br>• Menugaskan teknisi/staf lapangan sesuai spesialisasi.<br>• Memperbarui status pengerjaan (*In Progress* / *Completed*).<br>• Mengunggah foto bukti fisik hasil perbaikan (*proof of completion*). |
| **Teknisi / Staf Lapangan** (*Staff Member*) | Petugas teknis perawatan (Kelistrikan, AC, IT/Jaringan, Perabot, Sanitasi). | • Terdaftar di direktori master staf sekolah.<br>• Menerima disposisi penugasan perbaikan.<br>• Memiliki metrik beban tugas aktif (*active tasks*) & status ketersediaan (*availability*). |

---

## ⚡ Fitur Utama

### 1. Sisi Siswa / Pelapor (*Student Features*)
- **Smart Report Form**:
  - Pengambilan foto langsung dari Kamera atau Galeri.
  - Selektor lokasi bertingkat: Memilih Lantai (Lantai 1–4) lalu memilih dari **113 ruangan resmi**.
  - Pilihan kategori kerusakan (Kelistrikan, Air, Perabot Meja/Kursi, AC/Ventilasi, Lab, dll.).
  - Indikator tingkat prioritas (*Rendah, Sedang, Tinggi, Darurat*).
- **Community Upvote**: Siswa dapat meng-*upvote* laporan kerusakan di area publik untuk menandakan tingginya dampak kerusakan bagi kegiatan belajar.
- **Feed & Filter Laporan**: Pencarian instan dan filter status (*All, Pending, In Progress, Completed*).
- **Detail Laporan & Timeline**: Menampilkan detail deskripsi, foto sebelum perbaikan, dan foto serta catatan setelah diperbaiki.
- **Pengaturan & Personalisasi**:
  - Dukungan **Dual Language** (Bahasa Indonesia & English).
  - Dukungan **Dark Mode & Light Mode**.
  - Manajemen identitas & kelas (28 kelas resmi SMK).

### 2. Sisi Admin Sarpras (*Admin Features*)
- **Live Metrics Dashboard**: Visualisasi ringkasan jumlah laporan aktif, laporan terselesaikan, laporan mendesak, dan beban kerja teknisi.
- **Staff Assignment Dispatcher**: Menugaskan teknisi spesifik ke laporan tertentu secara fleksibel.
- **Verifikasi & Bukti Penyelesaian**: Formulir perubahan status yang mewajibkan input catatan pengerjaan dan bukti foto setelah perbaikan.

---

## 🏛️ Arsitektur Aplikasi (Software Architecture)

Aplikasi dibangun menggunakan pola arsitektur **MVVM (Model-View-ViewModel)** dengan prinsip **Unidirectional Data Flow (UDF)**:

```
┌────────────────────────────────────────────────────────┐
│                   UI LAYER (View)                      │
│   • Jetpack Compose Material 3                         │
│   • Single-Activity Architecture (MainActivity.kt)     │
│   • Custom Canvas 2D Vector Drawing (Icons/Graphics)   │
└───────────────────────────▲────────────────────────────┘
                            │ (Observes StateFlow / Dispatches Events)
┌───────────────────────────▼────────────────────────────┐
│                  VIEWMODEL LAYER                       │
│   • AuthViewModel & ReportViewModel                    │
│   • MutableStateFlow & StateFlow (Reactive UI State)   │
│   • AndroidViewModel & viewModelScope (Coroutines)     │
└───────────────────────────▲────────────────────────────┘
                            │ (Calls Repository Functions)
┌───────────────────────────▼────────────────────────────┐
│                    DATA LAYER                          │
│   • AuthRepository (Supabase GoTrue Auth)              │
│   • ReportRepository (Supabase PostgREST CRUD)         │
│   • SessionPreferences (Local Session & App Settings)  │
│   • SupabaseClientManager (Singleton Client Instance)  │
└───────────────────────────▲────────────────────────────┘
                            │ (HTTPS REST API Calls via Ktor)
┌───────────────────────────▼────────────────────────────┐
│                 BACKEND CLOUD (BaaS)                   │
│   • Supabase Cloud Platform (PostgreSQL)               │
│   • Row Level Security (RLS) Policy Engine             │
└────────────────────────────────────────────────────────┘
```

### Penjelasan Lapisan (Layers):
1. **UI Layer (`com.example.trac`)**:
   - Dibangun murni menggunakan **Jetpack Compose** deklaratif tanpa XML layout legacy.
   - Menggunakan *Single-Activity Pattern* di mana seluruh transisi layar dikelola melalui enum `Screen` dan animasi `AnimatedContent`.
   - Komponen visual, ilustrasi avatar, dan ikon digambar secara mandiri menggunakan Compose 2D Canvas.
2. **ViewModel Layer (`com.example.trac.viewmodel`)**:
   - Menjembatani UI dan Data Layer.
   - Mengelola state asynchronous UI (`AuthUiState`, `ReportUiState`) melalui `StateFlow`.
   - Mengisolasi pemrosesan berat pada *background dispatcher* (`Dispatchers.IO`).
3. **Data Layer (`com.example.trac.data`)**:
   - Mengimplementasikan **Repository Pattern** untuk memisahkan sumber data cloud dan lokal.
   - `ReportRepository`: Mengatur query PostgREST tabel laporan, master staf, notifikasi, dan lokasi.
   - `AuthRepository`: Mengatur siklus autentikasi akun dan token pengguna.
   - `SessionPreferences`: Menyimpan sesi login, preferensi tema, dan bahasa secara offline.

---

## 🔄 Alur Komunikasi Data (Data Flow)

Alur penanganan data dari aksi pengguna hingga tersimpan di cloud:

```
[1. User Action]
     │
     ▼
[2. Composable Screen] ──(Triggers Callback Lambda)──► [MainActivity]
                                                             │
                                                             ▼
[5. UI Recomposition] ◄──(Collects StateFlow)── [3. ViewModel (viewModelScope)]
                                                             │
                                                             ▼
                                                [4. Repository (Dispatchers.IO)]
                                                             │
                                                             ▼
                                                [Ktor Engine / PostgREST Client]
                                                             │
                                                             ▼
                                                [Supabase PostgreSQL Cloud DB]
```

1. **Aksi Pengguna**: Siswa mengisi formulir atau mengklik aksi (misal: *Kirim Laporan* atau *Upvote*).
2. **Event Dispatch**: Composable memanggil fungsi pada `ReportViewModel` / `AuthViewModel`.
3. **Background Execution**: ViewModel membuka coroutine di `viewModelScope` dan mendelegasikan pemrosesan ke `Dispatchers.IO`.
4. **Network Request**: Repository menjalankan request REST API terenkripsi ke endpoint Supabase PostgREST via Ktor Engine.
5. **State Update & Recomposition**: Database merespons dengan hasil terbaru, StateFlow di-update, dan Compose UI secara otomatis me-*recompose* layar dengan tampilan termutakhir.

---

## 💻 Tech Stack & Pustaka

| Komponen | Teknologi | Keterangan & Versi |
|---|---|---|
| **Bahasa Pemrograman** | Kotlin | Versi `2.0.21` (K2 Compose Compiler) |
| **Android SDK** | Android 14/15 | `compileSdk = 37`, `targetSdk = 37`, `minSdk = 24` |
| **UI Toolkit** | Jetpack Compose | BOM `2024.09.00`, Material 3 |
| **BaaS / Database** | Supabase | PostgreSQL Cloud Database & GoTrue Auth |
| **Supabase SDK** | Supabase Kotlin SDK | `3.0.0` (`postgrest-kt`, `auth-kt`) |
| **HTTP Network Engine** | Ktor Client Android | `3.0.0` |
| **JSON Serialization** | Kotlinx Serialization | `1.7.3` |
| **Concurrency & Async** | Kotlin Coroutines | `StateFlow`, `MutableStateFlow`, `viewModelScope` |
| **Penyimpanan Lokal** | SharedPreferences | Pengelolaan sesi login, dark mode, & bahasa |

---

## 🗄️ Skema & Arsitektur Database

Database di-host di cloud **Supabase (PostgreSQL)** dengan 5 tabel relasional inti yang dilindungi oleh **Row Level Security (RLS)**:

```
 ┌──────────────────────┐             ┌──────────────────────┐
 │     user_roles       │             │    staff_members     │
 ├──────────────────────┤             ├──────────────────────┤
 │ id (UUID, PK)        │             │ id (TEXT, PK)        │
 │ email (TEXT, UNIQUE) │             │ name (TEXT)          │
 │ role (TEXT)          │             │ role (TEXT)          │
 │ promoted_by (TEXT)   │             │ phone (TEXT)         │
 └──────────────────────┘             │ active_tasks (INT)   │
                                      │ is_available (BOOL)  │
 ┌──────────────────────────┐         └──────────────────────┘
 │    facility_locations    │
 ├──────────────────────────┤         ┌──────────────────────┐
 │ id (UUID, PK)            │         │ admin_notifications  │
 │ floor_name (TEXT)        │         ├──────────────────────┤
 │ floor_desc (TEXT)        │         │ id (UUID, PK)        │
 │ room_name (TEXT)         │         │ report_id (TEXT)     │
 │ is_active (BOOL)         │         │ title (TEXT)         │
 └──────────────────────────┘         │ description (TEXT)   │
                                      │ is_urgent (BOOL)     │
 ┌──────────────────────────────────┐ │ is_read (BOOL)       │
 │             reports              │ └──────────────────────┘
 ├──────────────────────────────────┤
 │ id (UUID/TEXT, PK)               │
 │ created_at (TIMESTAMPTZ)         │
 │ user_id (TEXT)                   │
 │ user_name (TEXT)                 │
 │ title (TEXT)                     │
 │ location (TEXT)                  │
 │ category (TEXT)                  │
 │ description (TEXT)               │
 │ status (TEXT)                    │ ──► 'Pending' | 'In Progress' | 'Completed'
 │ priority (TEXT)                  │ ──► 'Rendah' | 'Sedang' | 'Tinggi' | 'Darurat'
 │ image_url (TEXT)                 │ ──► Foto bukti awal pelapor
 │ completion_image_url (TEXT)      │ ──► Foto bukti setelah pengerjaan teknisi
 │ completion_notes (TEXT)          │ ──► Catatan teknis hasil perbaikan
 │ upvote_count (INT)               │ ──► Jumlah dukungan siswa lain
 └──────────────────────────────────┘
```

> Skrip migrasi DDL dan data seed tersedia lengkap di [`docs/SUPABASE_SETUP.sql`](file:///c:/Users/USER/AndroidStudioProjects/TRAC-Finished/docs/SUPABASE_SETUP.sql).

---

## 📁 Struktur Direktori Proyek

```text
TRAC-Finished/
├── app/
│   ├── build.gradle.kts                      # Konfigurasi dependensi modul app
│   └── src/main/
│       ├── AndroidManifest.xml               # Izin internet & konfigurasi Activity
│       └── java/com/example/trac/
│           ├── MainActivity.kt               # Single Activity, Navigasi Enum Screen, Top-level State
│           ├── SplashTracScreen.kt           # Onboarding & splash screen
│           ├── LoginTracScreen.kt            # Formulir login pengguna
│           ├── RegisterTracScreen.kt         # Formulir registrasi siswa & pilihan kelas
│           ├── HomeTracScreen.kt             # Dashboard utama siswa & ringkasan laporan
│           ├── CreateReportTracScreen.kt     # Formulir pelaporan, seleksi 113 ruangan & kamera
│           ├── ReportListTracScreen.kt       # Daftar semua laporan dengan fitur filter & pencarian
│           ├── ReportDetailTracScreen.kt     # Detail laporan, bukti before-after & timeline
│           ├── AdminDashboardTracScreen.kt   # Dashboard khusus admin sarpras & penugasan staf
│           ├── NotificationsTracScreen.kt    # Layar notifikasi sistem & broadcast
│           ├── EditIdentityTracScreen.kt     # Layar ubah identitas & profil siswa
│           ├── SettingsTracScreen.kt         # Pengaturan tema gelap & pilihan bahasa
│           ├── TermsTracScreen.kt            # Syarat & ketentuan penggunaan
│           ├── components/
│           │   ├── InAppBanner.kt            # Komponen banner notifikasi animated floating
│           │   ├── ProfileSidebarDrawer.kt  # Sidebar drawer profil pengguna
│           │   └── TracBottomNavBar.kt       # Bottom navigation bar stasioner 5-menu
│           ├── data/
│           │   ├── AuthRepository.kt         # Abstraksi autentikasi Supabase & session cache
│           │   ├── ReportData.kt             # Model DTO serializable untuk tabel reports
│           │   ├── ReportRepository.kt       # Operasi CRUD PostgREST (Reports, Staff, Locations)
│           │   ├── SchoolFacilityMasterData.kt# Master data 113 ruangan di 4 lantai
│           │   ├── SessionPreferences.kt     # Penyimpanan sesi & preferensi SharedPreferences
│           │   ├── StaffMember.kt            # Model data teknisi & petugas sarpras
│           │   ├── SupabaseClient.kt         # Singleton konfigurasi koneksi Supabase
│           │   └── UserProfileData.kt        # Data class profil & role pengguna
│           ├── util/
│           │   └── ImageUtils.kt             # Utility encoding/decoding Bitmap & Base64
│           └── viewmodel/
│               ├── AuthViewModel.kt          # ViewModel autentikasi, registrasi & sesi
│               └── ReportViewModel.kt        # ViewModel laporan, filter, status & upvote
├── docs/
│   ├── ANTIGRAVITY_CONTEXT.md                # Dokumentasi arsitektur teknis internal
│   ├── AI_DEVELOPMENT_RULES.md               # Konvensi pengembangan & aturan konsistensi
│   └── SUPABASE_SETUP.sql                    # Skrip SQL DDL & DML skema database
├── gradle/
│   └── libs.versions.toml                    # Version catalog dependensi Gradle
├── local.properties.example                  # Template konfigurasi URL & API Key Supabase
└── README.md                                 # Dokumentasi utama proyek
```

---

## 🚀 Instalasi & Menjalankan Proyek

### Prasyarat
1. **Android Studio** (Versi Ladybug / Koala atau lebih baru).
2. **JDK 11 atau lebih baru** (Disarankan Android Studio JBR).
3. Akun dan project aktif di [Supabase](https://supabase.com).

### Langkah-langkah Setup:

1. **Clone Repositori**:
   ```bash
   git clone https://github.com/username/TRAC-Finished.git
   cd TRAC-Finished
   ```

2. **Konfigurasi Database Supabase**:
   - Buka SQL Editor di Dashboard Supabase project Anda.
   - Buka file [`docs/SUPABASE_SETUP.sql`](file:///c:/Users/USER/AndroidStudioProjects/TRAC-Finished/docs/SUPABASE_SETUP.sql), salin seluruh isinya, lalu jalankan (*Run*) di Supabase SQL Editor untuk membuat 5 tabel dan data awal.

3. **Konfigurasi `local.properties`**:
   - Buat atau edit file `local.properties` di root direktori project:
     ```properties
     SUPABASE_URL=https://your-project-id.supabase.co
     SUPABASE_ANON_KEY=your-supabase-anon-key-here
     ```

4. **Build & Run**:
   - Buka project di Android Studio.
   - Lakukan **Sync Project with Gradle Files**.
   - Jalankan aplikasi di Emulator Android (API 24+) atau perangkat fisik.

---

## 📄 Lisensi & Hak Cipta
Dikembangkan untuk sistem manajemen dan pemeliharaan fasilitas sarana prasarana sekolah. Seluruh hak cipta dilindungi undang-undang.
