package com.example.trac.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trac.data.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(
        val message: String,
        val isLoginSuccess: Boolean = false,
        val isProfileUpdate: Boolean = false
    ) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)
    private var authJob: Job? = null

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun isUserLoggedIn(): Boolean = repository.isUserLoggedIn()

    fun getLoggedInUserId(): String = repository.getLoggedInUserId()

    fun getLoggedInUserName(): String = repository.getLoggedInUserName()

    fun getLoggedInUserClass(): String = repository.getLoggedInUserClass()
 
    fun getLoggedInUserRole(): String = repository.getLoggedInUserRole()

    fun isUserAdmin(): Boolean = repository.isUserAdmin()

    fun isSuperAdmin(): Boolean = repository.isSuperAdmin()

    private val _adminEmailsState = MutableStateFlow<Set<String>>(repository.getAdminEmails())
    val adminEmailsState: StateFlow<Set<String>> = _adminEmailsState.asStateFlow()

    private val _registeredUsersState = MutableStateFlow<List<com.example.trac.data.UserProfileData>>(emptyList())
    val registeredUsersState: StateFlow<List<com.example.trac.data.UserProfileData>> = _registeredUsersState.asStateFlow()

    fun refreshAdminEmails() {
        viewModelScope.launch {
            repository.syncRemoteAdminEmails()
            _adminEmailsState.value = repository.getAdminEmails()
        }
    }

    fun syncCurrentUserRole(onRoleUpdated: (String, Boolean) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val (role, isAdmin) = repository.syncCurrentUserRole()
            _adminEmailsState.value = repository.getAdminEmails()
            onRoleUpdated(role, isAdmin)
        }
    }

    fun loadRegisteredUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val users = repository.getRegisteredUsers()
            withContext(Dispatchers.Main) {
                _registeredUsersState.value = users
            }
        }
    }

    fun promoteUserToAdmin(email: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.promoteUserToAdmin(email)
            val updatedAdmins = repository.getAdminEmails()
            val updatedUsers = repository.getRegisteredUsers()
            withContext(Dispatchers.Main) {
                _adminEmailsState.value = updatedAdmins
                _registeredUsersState.value = updatedUsers
                onComplete()
            }
        }
    }

    fun demoteAdminToUser(email: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.demoteAdminToUser(email)
            val updatedAdmins = repository.getAdminEmails()
            val updatedUsers = repository.getRegisteredUsers()
            withContext(Dispatchers.Main) {
                _adminEmailsState.value = updatedAdmins
                _registeredUsersState.value = updatedUsers
                onComplete()
            }
        }
    }

    fun getAdminEmails(): Set<String> = repository.getAdminEmails()

    fun setUserRole(role: String) {
        repository.setLoggedInUserRole(role)
    }

    fun getLoggedInUserProfileImage(): String = repository.getLoggedInUserProfileImage()

    fun getLoggedInUserEmail(): String = repository.getLoggedInUserEmail()

    fun updateProfile(
        fullName: String,
        userClass: String,
        profileImage: String? = null,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.updateProfileSupabase(fullName, userClass, profileImage)
            result.onSuccess {
                _uiState.value = AuthUiState.Success(
                    message = "Identitas berhasil diperbarui!",
                    isLoginSuccess = false,
                    isProfileUpdate = true
                )
                onResult(result)
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(
                    error.localizedMessage ?: "Gagal memperbarui identitas di server."
                )
                onResult(result)
            }
        }
    }

    fun login(email: String, pass: String) {
        var cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            _uiState.value = AuthUiState.Error("Email/Student ID dan Password wajib diisi.")
            return
        }

        // Auto-append @gmail.com if user typed username without domain (e.g. "rayya" -> "rayya@gmail.com")
        if (!cleanEmail.contains("@")) {
            cleanEmail = "$cleanEmail@gmail.com"
        }

        authJob?.cancel()
        authJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AuthUiState.Loading
            repository.signIn(cleanEmail, cleanPass)
                .onSuccess {
                    _uiState.value = AuthUiState.Success("Login berhasil! Selamat datang di TRAC.", isLoginSuccess = true)
                }
                .onFailure { error ->
                    val rawMsg = error.message ?: ""
                    val friendlyMsg = when {
                        rawMsg.contains("Invalid login credentials", ignoreCase = true) ->
                            "Email/Username atau Password salah. Pastikan akun sudah terdaftar dan password benar."
                        rawMsg.contains("Email not confirmed", ignoreCase = true) ->
                            "Email Anda belum dikonfirmasi di Supabase Dashboard."
                        else -> error.localizedMessage ?: "Gagal login. Periksa koneksi internet Anda."
                    }
                    _uiState.value = AuthUiState.Error(friendlyMsg)
                }
        }
    }

    fun register(fullName: String, email: String, userClass: String, pass: String, confirmPass: String) {
        val cleanName = fullName.trim()
        var cleanEmail = email.trim()
        val cleanClass = userClass.trim()
        val cleanPass = pass.trim()
        val cleanConfirmPass = confirmPass.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanClass.isBlank() || cleanPass.isBlank() || cleanConfirmPass.isBlank()) {
            _uiState.value = AuthUiState.Error("Harap lengkapi semua kolom pendaftaran.")
            return
        }

        if (!cleanEmail.contains("@")) {
            cleanEmail = "$cleanEmail@gmail.com"
        }

        if (cleanPass.length < 6) {
            _uiState.value = AuthUiState.Error("Password minimal harus 6 karakter.")
            return
        }

        if (cleanPass != cleanConfirmPass) {
            _uiState.value = AuthUiState.Error("Konfirmasi Password tidak cocok dengan Password yang dimasukkan.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.signUp(cleanEmail, cleanPass, cleanName, cleanClass)
                .onSuccess {
                    _uiState.value = AuthUiState.Success(
                        message = "Pendaftaran berhasil! Silakan lakukan Login.",
                        isLoginSuccess = false
                    )
                }
                .onFailure { error ->
                    val rawMsg = error.message ?: ""
                    val friendlyMsg = when {
                        rawMsg.contains("already registered", ignoreCase = true) ||
                        rawMsg.contains("already exists", ignoreCase = true) ||
                        rawMsg.contains("User already registered", ignoreCase = true) ->
                            "Email/Student ID ini sudah terdaftar! Silakan gunakan email lain atau langsung Login."
                        rawMsg.contains("Password should be at least", ignoreCase = true) ->
                            "Password terlalu pendek. Gunakan minimal 6 karakter."
                        else -> error.localizedMessage ?: "Gagal mendaftar. Terjadi kesalahan pada server."
                    }
                    _uiState.value = AuthUiState.Error(friendlyMsg)
                }
        }
    }

    fun logout() {
        authJob?.cancel()
        _uiState.value = AuthUiState.Idle
        authJob = viewModelScope.launch(Dispatchers.IO) {
            repository.clearSessionLocal()
            withTimeoutOrNull(2000) {
                repository.signOutRemote()
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
