package com.example.trac.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("trac_user_session", Context.MODE_PRIVATE)

    fun saveSession(
        email: String,
        fullName: String,
        userId: String,
        userClass: String = "XI RPL",
        profileImage: String = "",
        role: String = "Siswa"
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_CLASS, userClass)
            .putString(KEY_PROFILE_IMAGE, profileImage)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun getFullName(): String {
        val name = prefs.getString(KEY_FULL_NAME, "") ?: ""
        return if (name.isNotBlank()) {
            name
        } else {
            getEmail().substringBefore("@").replaceFirstChar { it.uppercase() }
        }
    }

    fun getUserClass(): String {
        val cls = prefs.getString(KEY_USER_CLASS, "") ?: ""
        return if (cls.isNotBlank()) cls else "XI RPL"
    }

    fun getRole(): String {
        val email = getEmail().trim().lowercase()
        if (getAdminEmails().contains(email)) return "Admin"
        val role = prefs.getString(KEY_USER_ROLE, "") ?: ""
        return if (role.isNotBlank()) role else "Siswa"
    }

    fun saveRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun getAdminEmails(): Set<String> {
        val defaultAdmins = setOf(SUPER_ADMIN_EMAIL)
        val stored = prefs.getStringSet(KEY_ADMIN_EMAILS, emptySet()) ?: emptySet()
        return defaultAdmins + stored.map { it.lowercase() }
    }

    fun addAdminEmail(email: String) {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return
        val current = (prefs.getStringSet(KEY_ADMIN_EMAILS, emptySet()) ?: emptySet()).toMutableSet()
        current.add(clean)
        prefs.edit().putStringSet(KEY_ADMIN_EMAILS, current).apply()
    }

    fun removeAdminEmail(email: String) {
        val clean = email.trim().lowercase()
        if (clean == SUPER_ADMIN_EMAIL) return // Super Admin cannot be removed
        val current = (prefs.getStringSet(KEY_ADMIN_EMAILS, emptySet()) ?: emptySet()).toMutableSet()
        current.remove(clean)
        prefs.edit().putStringSet(KEY_ADMIN_EMAILS, current).apply()
    }

    fun setAdminEmails(emails: Set<String>) {
        val clean = emails.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        prefs.edit().putStringSet(KEY_ADMIN_EMAILS, clean).apply()
    }
    

    fun getProfileImage(): String = prefs.getString(KEY_PROFILE_IMAGE, "") ?: ""

    fun updateUserProfile(fullName: String, userClass: String, profileImage: String? = null) {
        val editor = prefs.edit()
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_USER_CLASS, userClass)

        if (!profileImage.isNullOrBlank()) {
            editor.putString(KEY_PROFILE_IMAGE, profileImage)
        }
        editor.apply()
    }

    fun isIndonesian(): Boolean = prefs.getBoolean(KEY_IS_INDONESIAN, false)

    fun saveLanguage(isIndonesian: Boolean) {
        prefs.edit().putBoolean(KEY_IS_INDONESIAN, isIndonesian).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_IS_DARK_MODE, false)

    fun saveDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DARK_MODE, isDark).apply()
    }

    fun getStaffList(): List<StaffMember> {
        val isCustomized = prefs.getBoolean(KEY_STAFF_CUSTOMIZED, false)
        val raw = prefs.getString(KEY_STAFF_MEMBERS_JSON, null)
        if (raw != null) {
            runCatching {
                return Json.decodeFromString<List<StaffMember>>(raw)
            }
        }
        return if (isCustomized) emptyList() else defaultStaffList
    }

    fun saveStaffList(list: List<StaffMember>) {
        runCatching {
            val json = Json.encodeToString(list)
            prefs.edit()
                .putString(KEY_STAFF_MEMBERS_JSON, json)
                .putBoolean(KEY_STAFF_CUSTOMIZED, true)
                .apply()
        }
    }

    fun getFacilityLocations(): List<FacilityLocation> {
        val raw = prefs.getString(KEY_FACILITY_LOCATIONS_JSON, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                return Json.decodeFromString<List<FacilityLocation>>(raw)
            }
        }
        return SchoolFacilityMasterData.defaultLocations
    }

    fun saveFacilityLocations(list: List<FacilityLocation>) {
        runCatching {
            val json = Json.encodeToString(list)
            prefs.edit()
                .putString(KEY_FACILITY_LOCATIONS_JSON, json)
                .putBoolean(KEY_FACILITY_LOCATIONS_CUSTOMIZED, true)
                .apply()
        }
    }

    fun getRegisteredUsers(): List<UserProfileData> {
        val raw = prefs.getString(KEY_REGISTERED_USERS_JSON, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                return Json.decodeFromString<List<UserProfileData>>(raw)
            }
        }
        return emptyList()
    }

    fun saveRegisteredUser(profile: UserProfileData) {
        runCatching {
            val current = getRegisteredUsers().toMutableList()
            val index = current.indexOfFirst { it.email.equals(profile.email, ignoreCase = true) }
            if (index >= 0) {
                current[index] = profile
            } else {
                current.add(profile)
            }
            prefs.edit().putString(KEY_REGISTERED_USERS_JSON, Json.encodeToString(current)).apply()
        }
    }

    fun getReadNotificationIds(): Set<String> {
        return prefs.getStringSet(KEY_READ_NOTIFICATION_IDS, emptySet()) ?: emptySet()
    }

    fun markNotificationAsRead(id: String) {
        val current = HashSet(getReadNotificationIds())
        current.add(id)
        prefs.edit().putStringSet(KEY_READ_NOTIFICATION_IDS, current).apply()
    }

    fun markAllNotificationsAsRead(ids: Collection<String>) {
        val current = HashSet(getReadNotificationIds())
        current.addAll(ids)
        prefs.edit().putStringSet(KEY_READ_NOTIFICATION_IDS, current).apply()
    }

    fun isNotificationRead(id: String): Boolean {
        return getReadNotificationIds().contains(id)
    }

    fun getReadAdminNotificationIds(): Set<String> {
        return prefs.getStringSet(KEY_READ_ADMIN_NOTIF_IDS, emptySet()) ?: emptySet()
    }

    fun markAdminNotificationAsRead(id: String) {
        val current = HashSet(getReadAdminNotificationIds())
        current.add(id)
        prefs.edit().putStringSet(KEY_READ_ADMIN_NOTIF_IDS, current).apply()
    }

    fun markAllAdminNotificationsAsRead(ids: Collection<String>) {
        val current = HashSet(getReadAdminNotificationIds())
        current.addAll(ids)
        prefs.edit().putStringSet(KEY_READ_ADMIN_NOTIF_IDS, current).apply()
    }

    fun getStatusOverrides(): Map<String, String> {
        val raw = prefs.getString(KEY_STATUS_OVERRIDES, null) ?: return emptyMap()
        return runCatching { Json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveStatusOverride(reportId: String, status: String) {
        val current = getStatusOverrides().toMutableMap()
        current[reportId] = status
        prefs.edit().putString(KEY_STATUS_OVERRIDES, Json.encodeToString(current)).apply()
    }

    fun getCompletionNotesOverrides(): Map<String, String> {
        val raw = prefs.getString(KEY_COMPLETION_NOTES_OVERRIDES, null) ?: return emptyMap()
        return runCatching { Json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveCompletionNotesOverride(reportId: String, notes: String?) {
        val current = getCompletionNotesOverrides().toMutableMap()
        if (notes != null) {
            current[reportId] = notes
        } else {
            current.remove(reportId)
        }
        prefs.edit().putString(KEY_COMPLETION_NOTES_OVERRIDES, Json.encodeToString(current)).apply()
    }

    fun getCompletionImageOverrides(): Map<String, String> {
        val raw = prefs.getString(KEY_COMPLETION_IMAGE_OVERRIDES, null) ?: return emptyMap()
        return runCatching { Json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveCompletionImageOverride(reportId: String, image: String?) {
        val current = getCompletionImageOverrides().toMutableMap()
        if (image != null) {
            current[reportId] = image
        } else {
            current.remove(reportId)
        }
        prefs.edit().putString(KEY_COMPLETION_IMAGE_OVERRIDES, Json.encodeToString(current)).apply()
    }

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""

    fun clearSession() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_EMAIL)
            .remove(KEY_FULL_NAME)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_CLASS)
            .remove(KEY_USER_ROLE)
            .remove(KEY_PROFILE_IMAGE)
            .apply()
    }

    companion object {
        const val SUPER_ADMIN_EMAIL = "rompisjosh@gmail.com"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_FULL_NAME = "key_full_name"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_CLASS = "key_user_class"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_ADMIN_EMAILS = "key_admin_emails"
        private const val KEY_STAFF_MEMBERS_JSON = "key_staff_members_json"
        private const val KEY_STAFF_CUSTOMIZED = "key_staff_customized"
        private const val KEY_FACILITY_LOCATIONS_JSON = "key_facility_locations_json"
        private const val KEY_FACILITY_LOCATIONS_CUSTOMIZED = "key_facility_locations_customized"
        private const val KEY_REGISTERED_USERS_JSON = "key_registered_users_json"
        private const val KEY_READ_NOTIFICATION_IDS = "key_read_notification_ids"
        private const val KEY_READ_ADMIN_NOTIF_IDS = "key_read_admin_notif_ids"
        private const val KEY_PROFILE_IMAGE = "key_profile_image"
        private const val KEY_IS_INDONESIAN = "key_is_indonesian"
        private const val KEY_IS_DARK_MODE = "key_is_dark_mode"
        private const val KEY_STATUS_OVERRIDES = "key_status_overrides"
        private const val KEY_COMPLETION_NOTES_OVERRIDES = "key_completion_notes_overrides"
        private const val KEY_COMPLETION_IMAGE_OVERRIDES = "key_completion_image_overrides"

        val defaultStaffList = listOf(
            StaffMember("STF-01", "Pak Joko Widodo", "Teknisi Kelistrikan & Lampu", "0812-3456-7890", 2, true),
            StaffMember("STF-02", "Pak Bambang Pamungkas", "Teknisi AC & Pendingin Ruangan", "0813-8877-6655", 1, true),
            StaffMember("STF-03", "Ibu Siti Khadijah", "Koordinator Fasilitas & Sanitasi", "0819-2233-4455", 1, true),
            StaffMember("STF-04", "Mas Fajar Pratama", "Teknisi IT, Lab & Jaringan", "0857-1122-3344", 3, true),
            StaffMember("STF-05", "Pak Rudi Hartono", "Staff Sarpras & Perabot Sipil", "0821-9988-7766", 0, true)
        )
    }
}
