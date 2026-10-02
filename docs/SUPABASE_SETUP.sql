-- ==============================================================================
-- TRAC (Tracking & Report Activity Center) - Supabase Database Schema Migration
-- ==============================================================================
-- Disesuaikan secara presisi dengan 5 tabel yang sudah ada di Supabase Anda:
-- 1. reports
-- 2. user_roles
-- 3. staff_members
-- 4. facility_locations
-- 5. admin_notifications
--
-- Skrip ini sepenuhnya IDEMPOTEN (aman dijalankan kapan pun tanpa error).
-- ==============================================================================

-- ==============================================================================
-- 1. TABEL USER_ROLES (Hak Akses Admin & Direktori Pengguna)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.user_roles (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL DEFAULT 'Siswa',
    promoted_by TEXT DEFAULT 'rompisjosh@gmail.com',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT user_roles_pkey PRIMARY KEY (id)
);

-- Seed default Super Admin
INSERT INTO public.user_roles (email, role, promoted_by)
VALUES ('rompisjosh@gmail.com', 'Super Admin', 'system')
ON CONFLICT (email) DO UPDATE SET role = 'Super Admin';

-- RLS untuk user_roles
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read user_roles" ON public.user_roles;
CREATE POLICY "Allow public read user_roles"
ON public.user_roles FOR SELECT
TO anon, authenticated
USING (true);

DROP POLICY IF EXISTS "Allow write user_roles" ON public.user_roles;
CREATE POLICY "Allow write user_roles"
ON public.user_roles FOR ALL
TO anon, authenticated
USING (true)
WITH CHECK (true);


-- ==============================================================================
-- 2. TABEL STAFF_MEMBERS (Teknisi Fasilitas & Petugas)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.staff_members (
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL,
    phone TEXT NOT NULL,
    active_tasks INT DEFAULT 0,
    is_available BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT staff_members_pkey PRIMARY KEY (id)
);

-- Seed daftar staf teknisi sekolah
INSERT INTO public.staff_members (id, name, role, phone, active_tasks, is_available)
VALUES 
    ('STF-01', 'Pak Joko Widodo', 'Teknisi Kelistrikan & Lampu', '0812-3456-7890', 2, true),
    ('STF-02', 'Pak Bambang Pamungkas', 'Teknisi AC & Pendingin Ruangan', '0813-8877-6655', 1, true),
    ('STF-03', 'Ibu Siti Khadijah', 'Koordinator Fasilitas & Sanitasi', '0819-2233-4455', 1, true),
    ('STF-04', 'Mas Fajar Pratama', 'Teknisi IT, Lab & Jaringan', '0857-1122-3344', 3, true),
    ('STF-05', 'Pak Rudi Hartono', 'Staff Sarpras & Perabot Sipil', '0821-9988-7766', 0, true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    phone = EXCLUDED.phone,
    active_tasks = EXCLUDED.active_tasks,
    is_available = EXCLUDED.is_available;

-- RLS untuk staff_members
ALTER TABLE public.staff_members ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read staff_members" ON public.staff_members;
CREATE POLICY "Allow public read staff_members"
ON public.staff_members FOR SELECT
TO anon, authenticated
USING (true);

DROP POLICY IF EXISTS "Allow write staff_members" ON public.staff_members;
CREATE POLICY "Allow write staff_members"
ON public.staff_members FOR ALL
TO anon, authenticated
USING (true)
WITH CHECK (true);


-- ==============================================================================
-- 3. TABEL FACILITY_LOCATIONS (4 Lantai & 113 Ruangan Sekolah)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.facility_locations (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    floor_name TEXT NOT NULL,
    floor_desc TEXT,
    room_name TEXT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    CONSTRAINT facility_locations_pkey PRIMARY KEY (id)
);

-- Tambahkan Unique Constraint (floor_name, room_name) jika belum ada
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'facility_locations_floor_room_key'
    ) THEN
        ALTER TABLE public.facility_locations 
        ADD CONSTRAINT facility_locations_floor_room_key UNIQUE (floor_name, room_name);
    END IF;
END $$;

-- Seed 113 Ruangan Resmi Gedung Sekolah (Lantai 1 s/d Lantai 4)
INSERT INTO public.facility_locations (floor_name, floor_desc, room_name, is_active)
VALUES
    -- LANTAI 1 (48 Ruangan)
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Business Centre', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Tunggu OTM', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Tangga Ki Hajar', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'XII RPL', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Guru PKN', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'XII BR 1', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'XI ULW', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Lab UPW', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Lab RPL', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Toilet BK', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Tangga UKS', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'UKS', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Pos Satpam', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Parkiran Mobil', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Parkiran Motor', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Ki Hajar Dewantara', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Tata Usaha', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Seni Sastra', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang BK', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Guru ULW', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Wakil Kepala Sekolah', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Kepala Sekolah', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Area Lobby Sekolah', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Area Resepsionis', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Gudang', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Kolam Ikan', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Taman', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Rapat Kecil', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Lab BD', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Seni', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang OSIS', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Audio', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Area Lapangan Utama', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Gazebo', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Lab BDP', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Toilet Guru', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Toilet Perempuan', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Guru Pemasaran', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Tangga GYM', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Gym', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Ruang Koperasi', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Lab PAI', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Mushola Nurul Iman', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Lapangan Badminton', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Area Selasar Lantai 1', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Kantin', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Toilet Kantin', true),
    ('LANTAI 1', 'Lobi, Administrasi, Lab Kejuruan & Fasilitas Umum', 'Sanggar Pramuka', true),

    -- LANTAI 2 (24 Ruangan)
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'XII AK 1', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'XII AK 2', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'XII AK 3', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Tangga Ki Hajar', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Ruang Guru AKL (A)', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Lab Akuntansi', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Ruang Guru AKL(B)', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'XI AK 1', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'XI AK 2', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Toilet Laki-Laki', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Tangga UKS', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Lab Bahasa', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Perpustakaan', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Ruang Guru Bahasa Indonesia', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Tangga GYM', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'XI AK 3', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Koperasi Lantai (Lama)', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'XII BD', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Aula', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Teras Aula', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Toilet Laki-Laki (Area Aula)', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Ruang Kedap Suara', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Toilet Perempuan (Area Aula)', true),
    ('LANTAI 2', 'Ruang Guru AKL, Perpustakaan, Aula & Lab Bahasa', 'Area Selasar Lantai 2', true),

    -- LANTAI 3 (23 Ruangan)
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XII MP 1', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XII MP 2', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Tangga Ki Hajar', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XI MP', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Mini Office', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Lab MP', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XI Manlog', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XII BR 2', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Toilet. Guru', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Toilet Siswa Laki-Laki', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Toilet Perempuan', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Tangga UKS', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XII ULW', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Lab Manlog', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Ruang Guru MTK', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XI BD', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Tangga GYM', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XI BR 1', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'XI BR 2', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Ruang Guru B. Inggris', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Ruang Kewirausahaan PKK', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Teras PKK', true),
    ('LANTAI 3', 'Kelas Manajemen Perkantoran, Logistik & Bisnis Daring', 'Area Selasar Lantai 3', true),

    -- LANTAI 4 (18 Ruangan)
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X AKL 1', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X AKL 2', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Tangga Ki Hajar', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X AKL 3', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Ruang Rokhris', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X MP', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X Manlog', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Ruang Guru IPAS', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Toilet Guru', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Toilet Siswa Cewe', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Tangga UKS', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X ULW', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Ruang Simulator', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X BD', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Gudang Rokhris', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'X RPL', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'XI RPL', true),
    ('LANTAI 4', 'Kelas X Semua Jurusan & Ruang Keagamaan', 'Area Selasar Lantai 4', true)
ON CONFLICT (floor_name, room_name) DO UPDATE SET
    floor_desc = EXCLUDED.floor_desc,
    is_active = EXCLUDED.is_active;

-- RLS untuk facility_locations
ALTER TABLE public.facility_locations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read facility_locations" ON public.facility_locations;
CREATE POLICY "Allow public read facility_locations"
ON public.facility_locations FOR SELECT
TO anon, authenticated
USING (true);

DROP POLICY IF EXISTS "Allow write facility_locations" ON public.facility_locations;
CREATE POLICY "Allow write facility_locations"
ON public.facility_locations FOR ALL
TO anon, authenticated
USING (true)
WITH CHECK (true);


-- ==============================================================================
-- 4. TABEL ADMIN_NOTIFICATIONS (Notifikasi Admin)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.admin_notifications (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    report_id TEXT,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    is_urgent BOOLEAN DEFAULT false,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT admin_notifications_pkey PRIMARY KEY (id)
);

-- RLS untuk admin_notifications
ALTER TABLE public.admin_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read admin_notifications" ON public.admin_notifications;
CREATE POLICY "Allow public read admin_notifications"
ON public.admin_notifications FOR SELECT
TO anon, authenticated
USING (true);

DROP POLICY IF EXISTS "Allow write admin_notifications" ON public.admin_notifications;
CREATE POLICY "Allow write admin_notifications"
ON public.admin_notifications FOR ALL
TO anon, authenticated
USING (true)
WITH CHECK (true);


-- ==============================================================================
-- 5. TABEL REPORTS (Laporan Kerusakan Fasilitas)
-- ==============================================================================
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read reports" ON public.reports;
CREATE POLICY "Allow public read reports"
ON public.reports FOR SELECT
TO anon, authenticated
USING (true);

DROP POLICY IF EXISTS "Allow write reports" ON public.reports;
CREATE POLICY "Allow write reports"
ON public.reports FOR ALL
TO anon, authenticated
USING (true)
WITH CHECK (true);
