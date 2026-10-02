package com.example.trac.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FacilityLocationRow(
    val id: String? = null,
    @SerialName("floor_name") val floorName: String,
    @SerialName("floor_desc") val floorDesc: String? = null,
    @SerialName("room_name") val roomName: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class FacilityLocation(
    val floorName: String,
    val floorDesc: String,
    val roomCount: Int,
    val activeReportsCount: Int,
    val rooms: List<String>
)

object SchoolFacilityMasterData {
    val defaultLocations: List<FacilityLocation> = listOf(
        FacilityLocation(
            floorName = "LANTAI 1",
            floorDesc = "Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum",
            roomCount = 48,
            activeReportsCount = 0,
            rooms = listOf(
                "Business Centre", "Ruang Tunggu OTM", "Tangga Ki Hajar", "XII RPL",
                "Ruang Guru PKN", "XII BR 1", "XI ULW", "Lab UPW", "Lab RPL",
                "Toilet BK", "Tangga UKS", "UKS", "Pos Satpam", "Parkiran Mobil",
                "Parkiran Motor", "Ruang Ki Hajar Dewantara", "Ruang Tata Usaha",
                "Ruang Seni Sastra", "Ruang BK", "Ruang Guru ULW", "Ruang Wakil Kepala Sekolah",
                "Ruang Kepala Sekolah", "Area Lobby Sekolah", "Area Resepsionis", "Gudang",
                "Kolam Ikan", "Taman", "Ruang Rapat Kecil", "Lab BD", "Ruang Seni",
                "Ruang OSIS", "Ruang Audio", "Area Lapangan Utama", "Gazebo", "Lab BDP",
                "Toilet Guru", "Toilet Perempuan", "Ruang Guru Pemasaran", "Tangga GYM",
                "Gym", "Ruang Koperasi", "Lab PAI", "Mushola Nurul Iman", "Lapangan Badminton",
                "Area Selasar Lantai 1", "Kantin", "Toilet Kantin", "Sanggar Pramuka"
            )
        ),
        FacilityLocation(
            floorName = "LANTAI 2",
            floorDesc = "Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa",
            roomCount = 24,
            activeReportsCount = 0,
            rooms = listOf(
                "XII AK 1", "XII AK 2", "XII AK 3", "Tangga Ki Hajar", "Ruang Guru AKL (A)",
                "Lab Akuntansi", "Ruang Guru AKL(B)", "XI AK 1", "XI AK 2", "Toilet Laki-Laki",
                "Tangga UKS", "Lab Bahasa", "Perpustakaan", "Ruang Guru Bahasa Indonesia",
                "Tangga GYM", "XI AK 3", "Koperasi Lantai (Lama)", "XII BD", "Aula",
                "Teras Aula", "Toilet Laki-Laki (Area Aula)", "Ruang Kedap Suara",
                "Toilet Perempuan (Area Aula)", "Area Selasar Lantai 2"
            )
        ),
        FacilityLocation(
            floorName = "LANTAI 3",
            floorDesc = "Kelas Manajemen Perkantoran, Logistik & Bisnis Daring",
            roomCount = 23,
            activeReportsCount = 0,
            rooms = listOf(
                "XII MP 1", "XII MP 2", "Tangga Ki Hajar", "XI MP", "Mini Office", "Lab MP",
                "XI Manlog", "XII BR 2", "Toilet. Guru", "Toilet Siswa Laki-Laki", "Toilet Perempuan",
                "Tangga UKS", "XII ULW", "Lab Manlog", "Ruang Guru MTK", "XI BD", "Tangga GYM",
                "XI BR 1", "XI BR 2", "Ruang Guru B. Inggris", "Ruang Kewirausahaan PKK",
                "Teras PKK", "Area Selasar Lantai 3"
            )
        ),
        FacilityLocation(
            floorName = "LANTAI 4",
            floorDesc = "Kelas X Semua Jurusan & Ruang Keagamaan",
            roomCount = 17,
            activeReportsCount = 0,
            rooms = listOf(
                "X AKL 1", "X AKL 2", "Tangga Ki Hajar", "X AKL 3", "Ruang Rokhris", "X MP",
                "X Manlog", "Ruang Guru IPAS", "Toilet Guru", "Toilet Siswa Cewe", "Tangga UKS",
                "X ULW", "Ruang Simulator", "X BD", "Gudang Rokhris", "X RPL", "XI RPL",
                "Area Selasar Lantai 4"
            )
        )
    )

    val defaultCategories = listOf(
        "Electronics" to "Proyektor, PC Lab, Monitor, Speaker & Sound System",
        "Furniture" to "Meja Siswa, Kursi Guru, Papan Tulis, Lemari, Pintu",
        "Plumbing" to "Keran Air, Wastafel, Saluran Pembuangan Air, Pipa",
        "Building Facility" to "Plafon Bocor, Lantai Keramik, Dinding, Kaca Jendela",
        "AC & Air System" to "AC Bocor, AC Tidak Dingin, Remote AC Rusak, Kipas",
        "Lighting" to "Lampu Kelas Mati, Lampu Selasar, Saklar & Stopkontak",
        "Sanitary / Toilet" to "Kloset Mampet, Gayung, Kunci Pintu Toilet, Bau",
        "Lainnya (Kustom)" to "Fasilitas dan sarana prasarana lainnya"
    )
}
