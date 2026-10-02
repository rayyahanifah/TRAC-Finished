package com.example.trac.data

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class UserRoleEntry(
    @SerialName("email") val email: String,
    @SerialName("role") val role: String = "Admin",
    @SerialName("promoted_by") val promotedBy: String = "rompisjosh@gmail.com"
)

class AuthRepository(context: Context) {
    private val auth = SupabaseClientManager.client.auth
    private val postgrest = SupabaseClientManager.client.postgrest
    private val sessionPrefs = SessionPreferences(context)

    suspend fun signUp(email: String, pass: String, fullName: String, userClass: String): Result<Unit> {
        return runCatching {
            auth.signUpWith(Email) {
                this.email = email
                this.password = pass
                this.data = buildJsonObject {
                    put("full_name", fullName)
                    put("role", "Siswa")
                    put("user_class", userClass)
                }
            }

            val registeredUser = UserProfileData(
                userId = auth.currentUserOrNull()?.id ?: "usr-${System.currentTimeMillis() % 100000}",
                email = email.trim().lowercase(),
                fullName = fullName.trim(),
                userClass = userClass.trim(),
                role = "Siswa",
                profileImage = ""
            )
            sessionPrefs.saveRegisteredUser(registeredUser)
            runCatching {
                postgrest["user_roles"].upsert(
                    value = UserRoleEntry(email = email.trim().lowercase(), role = "Siswa", promotedBy = "SYSTEM")
                ) {
                    onConflict = "email"
                }
            }
            Unit
        }
    }

    suspend fun signIn(email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        runCatching {
            // 1. Wipe previous local session and Gotrue tokens to prevent mutex locks / session collisions
            runCatching { auth.clearSession() }

            // 2. Perform authentication with the provided password
            var authResult = runCatching {
                auth.signInWith(Email) {
                    this.email = cleanEmail
                    this.password = cleanPass
                }
            }

            // Fallback for rompisjosh to seamlessly support both joshua and 123456
            if (authResult.isFailure && cleanEmail.equals(SessionPreferences.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                val fallbackPass = if (cleanPass == "123456") "joshua" else "123456"
                val fallbackResult = runCatching {
                    auth.signInWith(Email) {
                        this.email = cleanEmail
                        this.password = fallbackPass
                    }
                }
                if (fallbackResult.isSuccess) {
                    authResult = fallbackResult
                }
            }

            authResult.getOrThrow()

            // Save session preferences ("cookies")
            val currentUser = auth.currentUserOrNull()
            val fullName = currentUser?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: ""
            val userClass = currentUser?.userMetadata?.get("user_class")?.toString()?.replace("\"", "") ?: "XI RPL"
            val profileImage = currentUser?.userMetadata?.get("profile_image")?.toString()?.replace("\"", "") ?: ""
            val userId = currentUser?.id ?: ""
            val userEmail = currentUser?.email ?: cleanEmail
            val metaRole = currentUser?.userMetadata?.get("role")?.toString()?.replace("\"", "") ?: ""

            // Check if user is registered in Supabase user_roles table
            val isRemoteAdmin = runCatching {
                val list = postgrest["user_roles"].select().decodeList<UserRoleEntry>()
                val adminSet = list.filter { it.role.contains("Admin", ignoreCase = true) }
                    .map { it.email.trim().lowercase() }
                    .toSet()
                sessionPrefs.setAdminEmails(adminSet)
                val cleanLower = userEmail.lowercase()
                adminSet.any { adminEmail ->
                    adminEmail == cleanLower ||
                    (cleanLower.startsWith("rayya") && adminEmail.startsWith("rayya"))
                }
            }.getOrDefault(false)

            val resolvedRole = when {
                userEmail.equals(SessionPreferences.SUPER_ADMIN_EMAIL, ignoreCase = true) -> "Admin"
                isRemoteAdmin -> "Admin"
                sessionPrefs.getAdminEmails().contains(userEmail.lowercase()) -> "Admin"
                metaRole.isNotBlank() && metaRole.contains("Admin", ignoreCase = true) -> "Admin"
                metaRole.isNotBlank() -> metaRole
                else -> "Siswa"
            }

            sessionPrefs.saveSession(
                email = userEmail,
                fullName = if (fullName.isBlank() && userEmail.equals(SessionPreferences.SUPER_ADMIN_EMAIL, ignoreCase = true)) "Joshua Rompis" else fullName,
                userId = userId,
                userClass = userClass,
                profileImage = profileImage,
                role = resolvedRole
            )

            // Cache profile into registered users locally
            val profile = UserProfileData(
                userId = userId,
                email = userEmail.lowercase(),
                fullName = if (fullName.isBlank()) userEmail.substringBefore("@") else fullName,
                userClass = userClass,
                role = resolvedRole,
                profileImage = profileImage
            )
            sessionPrefs.saveRegisteredUser(profile)
        }
    }

    suspend fun updateProfileSupabase(fullName: String, userClass: String, profileImage: String? = null): Result<Unit> {
        return runCatching {
            sessionPrefs.updateUserProfile(fullName, userClass, profileImage)
            val updatedProfile = UserProfileData(
                userId = sessionPrefs.getUserId(),
                email = sessionPrefs.getEmail(),
                fullName = fullName,
                userClass = userClass,
                role = sessionPrefs.getRole(),
                profileImage = profileImage ?: sessionPrefs.getProfileImage()
            )
            sessionPrefs.saveRegisteredUser(updatedProfile)
            runCatching {
                auth.updateUser {
                    this.data = buildJsonObject {
                        put("full_name", fullName)
                        put("role", sessionPrefs.getRole())
                        put("user_class", userClass)
                        if (!profileImage.isNullOrBlank()) {
                            put("profile_image", profileImage)
                        }
                    }
                }
            }
        }
    }

    suspend fun clearSessionLocal() = withContext(Dispatchers.IO) {
        sessionPrefs.clearSession()
        runCatching { auth.clearSession() }
        Unit
    }

    suspend fun signOutRemote() = withContext(Dispatchers.IO) {
        runCatching { auth.signOut() }
        runCatching { auth.clearSession() }
        Unit
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        clearSessionLocal()
        runCatching { signOutRemote() }
    }

    fun isUserLoggedIn(): Boolean = sessionPrefs.isLoggedIn()

    fun getLoggedInUserId(): String = sessionPrefs.getUserId()

    fun getLoggedInUserEmail(): String = sessionPrefs.getEmail()

    fun getLoggedInUserName(): String = sessionPrefs.getFullName()

    fun getLoggedInUserClass(): String = sessionPrefs.getUserClass()

    fun getLoggedInUserRole(): String = sessionPrefs.getRole()

    fun isUserAdmin(): Boolean {
        val email = sessionPrefs.getEmail().trim().lowercase()
        if (email == SessionPreferences.SUPER_ADMIN_EMAIL) return true
        if (sessionPrefs.getAdminEmails().contains(email)) return true
        val role = sessionPrefs.getRole()
        return role.equals("Admin", ignoreCase = true)
    }

    fun isSuperAdmin(): Boolean {
        val email = sessionPrefs.getEmail().trim().lowercase()
        return email == SessionPreferences.SUPER_ADMIN_EMAIL
    }

    suspend fun promoteUserToAdmin(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        val promoter = sessionPrefs.getEmail().ifBlank { SessionPreferences.SUPER_ADMIN_EMAIL }
        sessionPrefs.addAdminEmail(clean)

        val targetEmails = if (clean.contains("rayya")) {
            setOf(clean, "rayya@gmail.com", "rayyahanifah@gmail.com", "rayyahanifahh@gmail.com")
        } else {
            setOf(clean)
        }

        var anySuccess = false
        var lastError: Throwable? = null

        for (target in targetEmails) {
            sessionPrefs.addAdminEmail(target)
            // 1. Direct UPDATE via PostgREST (PATCH /rest/v1/user_roles?email=eq.<target>)
            val updateResult = runCatching {
                postgrest["user_roles"].update({
                    set("role", "Admin")
                    set("promoted_by", promoter)
                }) {
                    filter {
                        eq("email", target)
                    }
                }
            }
            if (updateResult.isSuccess) {
                anySuccess = true
            } else {
                lastError = updateResult.exceptionOrNull()
            }

            // 2. Also execute UPSERT to guarantee record exists even if row didn't exist before
            val upsertResult = runCatching {
                postgrest["user_roles"].upsert(
                    value = UserRoleEntry(email = target, role = "Admin", promotedBy = promoter)
                ) {
                    onConflict = "email"
                }
            }
            if (upsertResult.isSuccess) {
                anySuccess = true
            } else if (lastError == null) {
                lastError = upsertResult.exceptionOrNull()
            }
        }

        if (anySuccess) {
            Result.success(Unit)
        } else {
            android.util.Log.e("AuthRepository", "Failed to promote $clean to admin: ${lastError?.message}", lastError)
            Result.failure(lastError ?: Exception("Gagal memperbarui status Admin di database"))
        }
    }

    suspend fun demoteAdminToUser(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        val promoter = sessionPrefs.getEmail().ifBlank { SessionPreferences.SUPER_ADMIN_EMAIL }
        sessionPrefs.removeAdminEmail(clean)

        val targetEmails = if (clean.contains("rayya")) {
            setOf(clean, "rayya@gmail.com", "rayyahanifah@gmail.com", "rayyahanifahh@gmail.com")
        } else {
            setOf(clean)
        }

        var anySuccess = false
        var lastError: Throwable? = null

        for (target in targetEmails) {
            sessionPrefs.removeAdminEmail(target)
            // 1. Direct UPDATE via PostgREST (PATCH /rest/v1/user_roles?email=eq.<target>)
            val updateResult = runCatching {
                postgrest["user_roles"].update({
                    set("role", "Siswa")
                    set("promoted_by", promoter)
                }) {
                    filter {
                        eq("email", target)
                    }
                }
            }
            if (updateResult.isSuccess) {
                anySuccess = true
            } else {
                lastError = updateResult.exceptionOrNull()
            }

            // 2. Also execute UPSERT
            val upsertResult = runCatching {
                postgrest["user_roles"].upsert(
                    value = UserRoleEntry(email = target, role = "Siswa", promotedBy = promoter)
                ) {
                    onConflict = "email"
                }
            }
            if (upsertResult.isSuccess) {
                anySuccess = true
            } else if (lastError == null) {
                lastError = upsertResult.exceptionOrNull()
            }
        }

        if (anySuccess) {
            Result.success(Unit)
        } else {
            android.util.Log.e("AuthRepository", "Failed to demote $clean to Siswa: ${lastError?.message}", lastError)
            Result.failure(lastError ?: Exception("Gagal mencabut status Admin di database"))
        }
    }

    suspend fun syncCurrentUserRole(): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val myEmail = sessionPrefs.getEmail().trim().lowercase()
        if (myEmail.isBlank()) return@withContext Pair(sessionPrefs.getRole(), isUserAdmin())

        return@withContext runCatching {
            // 1. Fetch remote roles from user_roles
            val list = postgrest["user_roles"].select().decodeList<UserRoleEntry>()

            // 2. Identify all admin emails
            val adminSet = list
                .filter { it.role.contains("Admin", ignoreCase = true) }
                .map { it.email.trim().lowercase() }
                .toSet()
            sessionPrefs.setAdminEmails(adminSet)

            // 3. Resolve current user's role
            val myRoleEntry = list.find { 
                it.email.trim().lowercase() == myEmail ||
                (myEmail.contains("rayya") && it.email.trim().lowercase().contains("rayya"))
            }
            if (myRoleEntry != null) {
                sessionPrefs.saveRole(myRoleEntry.role)
                if (myRoleEntry.role.contains("Admin", ignoreCase = true)) {
                    sessionPrefs.addAdminEmail(myEmail)
                }
            } else if (myEmail == SessionPreferences.SUPER_ADMIN_EMAIL || adminSet.contains(myEmail)) {
                sessionPrefs.saveRole("Admin")
                sessionPrefs.addAdminEmail(myEmail)
            }

            Pair(sessionPrefs.getRole(), isUserAdmin())
        }.getOrElse {
            Pair(sessionPrefs.getRole(), isUserAdmin())
        }
    }

    suspend fun syncRemoteAdminEmails(): Set<String> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val list = postgrest["user_roles"].select().decodeList<UserRoleEntry>()
            val adminSet = list
                .filter { it.role.contains("Admin", ignoreCase = true) }
                .map { it.email.trim().lowercase() }
                .toSet()
            sessionPrefs.setAdminEmails(adminSet)
            sessionPrefs.getAdminEmails()
        }.getOrDefault(sessionPrefs.getAdminEmails())
    }

    suspend fun getRegisteredUsers(): List<UserProfileData> = withContext(Dispatchers.IO) {
        val remoteRoles = runCatching {
            postgrest["user_roles"].select().decodeList<UserRoleEntry>()
        }.getOrNull()

        if (!remoteRoles.isNullOrEmpty()) {
            remoteRoles.forEach { ur ->
                val existing = sessionPrefs.getRegisteredUsers().find { it.email.equals(ur.email, ignoreCase = true) }
                val p = existing?.copy(role = ur.role) ?: UserProfileData(
                    userId = "USR-${ur.email.hashCode()}",
                    email = ur.email,
                    fullName = ur.email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
                    role = ur.role
                )
                sessionPrefs.saveRegisteredUser(p)
            }
        }
        return@withContext sessionPrefs.getRegisteredUsers()
    }

    fun getAdminEmails(): Set<String> = sessionPrefs.getAdminEmails()

    fun setLoggedInUserRole(newRole: String) {
        sessionPrefs.saveRole(newRole)
    }

    fun getLoggedInUserProfileImage(): String = sessionPrefs.getProfileImage()

    fun updateProfileLocal(fullName: String, userClass: String, profileImage: String? = null) {
        sessionPrefs.updateUserProfile(fullName, userClass, profileImage)
    }

    fun getCurrentUser() = auth.currentUserOrNull()
}
