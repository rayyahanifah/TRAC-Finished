package com.example.trac

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trac.components.ProfileSidebarDrawer
import com.example.trac.components.SidebarMenuItem
import com.example.trac.components.TracBottomNavBar
import com.example.trac.data.ReportData
import com.example.trac.data.ReportRepository
import com.example.trac.data.SessionPreferences
import com.example.trac.viewmodel.AuthUiState
import com.example.trac.viewmodel.AuthViewModel
import com.example.trac.viewmodel.ReportUiState
import com.example.trac.viewmodel.ReportViewModel

enum class Screen {
    SPLASH,
    LOGIN,
    REGISTER,
    HOME,
    REPORT_LIST,
    REPORT_DETAIL,
    CREATE_REPORT,
    TERMS,
    NOTIFICATIONS,
    EDIT_IDENTITY,
    SETTINGS,
    ADMIN_DASHBOARD
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        // Force light status bar & navigation bar icons regardless of System Dark Mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFF8FAFD),
                    surface = Color.White
                )
            ) {
                TRACApp()
            }
        }
    }
}

@Composable
fun TRACApp(
    authViewModel: AuthViewModel = viewModel(),
    reportViewModel: ReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionPrefs = remember { SessionPreferences(context) }
    val reportRepo = remember { ReportRepository() }

    // 1. Check persistent local login session ("cookies") on app launch
    val isAlreadyLoggedIn = remember { authViewModel.isUserLoggedIn() }

    var currentScreen by remember {
        mutableStateOf(if (isAlreadyLoggedIn) Screen.HOME else Screen.SPLASH)
    }

    var isSidebarOpen by remember { mutableStateOf(false) }

    var isIndonesianLanguage by remember { mutableStateOf(sessionPrefs.isIndonesian()) }
    var isAppDarkMode by remember { mutableStateOf(sessionPrefs.isDarkMode()) }

    var previousScreen by remember { mutableStateOf(Screen.HOME) }
    var backPressedTime by remember { mutableLongStateOf(0L) }

    var bannerErrorMessage by remember { mutableStateOf<String?>(null) }

    var loggedInUserName by remember {
        mutableStateOf(authViewModel.getLoggedInUserName())
    }

    var loggedInUserClass by remember {
        mutableStateOf(authViewModel.getLoggedInUserClass())
    }

    var loggedInProfileImage by remember {
        mutableStateOf(authViewModel.getLoggedInUserProfileImage())
    }

    var isUserAdmin by remember {
        mutableStateOf(authViewModel.isUserAdmin())
    }

    var loggedInUserRole by remember {
        mutableStateOf(authViewModel.getLoggedInUserRole())
    }

    // Selected Report for Detail View
    var selectedReport by remember { mutableStateOf<ReportData?>(null) }

    val authState by authViewModel.uiState.collectAsState()
    val reportUiState by reportViewModel.uiState.collectAsState()
    val liveReportsList by reportViewModel.reports.collectAsState()
    val adminEmails by authViewModel.adminEmailsState.collectAsState()
    val registeredUsers by authViewModel.registeredUsersState.collectAsState()

    val handleLogout: () -> Unit = {
        isSidebarOpen = false
        bannerErrorMessage = null
        authViewModel.logout()
        loggedInUserName = ""
        loggedInUserClass = ""
        loggedInProfileImage = ""
        loggedInUserRole = "Siswa"
        isUserAdmin = false
        currentScreen = Screen.SPLASH
    }

    // 2. System Back Gesture & Back Button Handling
    BackHandler(enabled = true) {
        if (isSidebarOpen) {
            isSidebarOpen = false
            return@BackHandler
        }

        when (currentScreen) {
            Screen.HOME, Screen.SPLASH -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedTime = currentTime
                    Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                }
            }

            Screen.REPORT_DETAIL -> {
                currentScreen = previousScreen
            }

            Screen.ADMIN_DASHBOARD, Screen.REPORT_LIST, Screen.CREATE_REPORT, Screen.NOTIFICATIONS, Screen.EDIT_IDENTITY, Screen.SETTINGS -> {
                currentScreen = Screen.HOME
            }

            Screen.REGISTER, Screen.LOGIN -> {
                currentScreen = Screen.SPLASH
            }

            Screen.TERMS -> {
                currentScreen = Screen.REGISTER
            }
        }
    }

    // Handle Auth UI States
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthUiState.Success -> {
                bannerErrorMessage = null
                if (state.isLoginSuccess) {
                    loggedInUserName = authViewModel.getLoggedInUserName()
                    loggedInUserClass = authViewModel.getLoggedInUserClass()
                    loggedInProfileImage = authViewModel.getLoggedInUserProfileImage()
                    loggedInUserRole = authViewModel.getLoggedInUserRole()
                    isUserAdmin = authViewModel.isUserAdmin()
                    reportViewModel.fetchReports()
                    currentScreen = Screen.HOME
                } else if (state.isProfileUpdate) {
                    loggedInUserName = authViewModel.getLoggedInUserName()
                    loggedInUserClass = authViewModel.getLoggedInUserClass()
                    loggedInProfileImage = authViewModel.getLoggedInUserProfileImage()
                    loggedInUserRole = authViewModel.getLoggedInUserRole()
                    isUserAdmin = authViewModel.isUserAdmin()
                    currentScreen = Screen.HOME
                } else {
                    currentScreen = Screen.LOGIN
                }
                authViewModel.resetState()
            }

            is AuthUiState.Error -> {
                bannerErrorMessage = state.message
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }

            else -> {}
        }
    }

    // Handle Report UI States
    LaunchedEffect(reportUiState) {
        when (val state = reportUiState) {
            is ReportUiState.Success -> {
                bannerErrorMessage = null
                reportViewModel.fetchReports()
                currentScreen = Screen.REPORT_LIST
                reportViewModel.resetState()
            }

            is ReportUiState.Error -> {
                bannerErrorMessage = state.message
            }

            else -> {}
        }
    }

    // Auto-fetch fresh reports and users from Supabase whenever user opens Home, Report List, or Admin Dashboard
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.HOME || currentScreen == Screen.REPORT_LIST || currentScreen == Screen.NOTIFICATIONS) {
            reportViewModel.fetchReports()
            authViewModel.syncCurrentUserRole { role, isAdmin ->
                loggedInUserRole = role
                isUserAdmin = isAdmin
            }
            reportRepo.getReadNotificationIds().onSuccess { readIds ->
                if (readIds.isNotEmpty()) {
                    sessionPrefs.markAllNotificationsAsRead(readIds)
                    sessionPrefs.markAllAdminNotificationsAsRead(readIds)
                }
            }
            reportRepo.getFacilityLocations().onSuccess { locs ->
                if (locs.isNotEmpty()) sessionPrefs.saveFacilityLocations(locs)
            }
            reportRepo.getStaffList().onSuccess { staff ->
                if (staff.isNotEmpty()) sessionPrefs.saveStaffList(staff)
            }
        }
        if (currentScreen == Screen.ADMIN_DASHBOARD) {
            authViewModel.syncCurrentUserRole { role, isAdmin ->
                loggedInUserRole = role
                isUserAdmin = isAdmin
            }
            authViewModel.loadRegisteredUsers()
            authViewModel.refreshAdminEmails()
            reportViewModel.fetchReports()
            reportRepo.getReadNotificationIds().onSuccess { readIds ->
                if (readIds.isNotEmpty()) {
                    sessionPrefs.markAllNotificationsAsRead(readIds)
                    sessionPrefs.markAllAdminNotificationsAsRead(readIds)
                }
            }
            reportRepo.getFacilityLocations().onSuccess { locs ->
                if (locs.isNotEmpty()) sessionPrefs.saveFacilityLocations(locs)
            }
            reportRepo.getStaffList().onSuccess { staff ->
                if (staff.isNotEmpty()) sessionPrefs.saveStaffList(staff)
            }
        }
    }

    // Dynamic unread notifications count for Bell Badge
    val unreadNotifsCount = remember(liveReportsList, currentScreen) {
        val readIds = sessionPrefs.getReadNotificationIds()
        val dynamicIds = liveReportsList.filter {
            it.status.equals("In Progress", ignoreCase = true) || it.status.equals("Completed", ignoreCase = true)
        }.map { if (it.status.equals("Completed", ignoreCase = true)) "notif_done_${it.id ?: it.title.hashCode()}" else "notif_prog_${it.id ?: it.title.hashCode()}" }
        val allIds = (dynamicIds + listOf("notif_def_1", "notif_ann_1")).distinct()
        allIds.count { !readIds.contains(it) }
    }

    val showBottomBar = currentScreen in listOf(
        Screen.HOME,
        Screen.REPORT_LIST,
        Screen.REPORT_DETAIL,
        Screen.CREATE_REPORT,
        Screen.NOTIFICATIONS
    )

    val pageBgColor = if (isAppDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = pageBgColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val isTabNavigation = (initialState in listOf(Screen.HOME, Screen.REPORT_LIST, Screen.REPORT_DETAIL, Screen.CREATE_REPORT, Screen.NOTIFICATIONS)) &&
                            (targetState in listOf(Screen.HOME, Screen.REPORT_LIST, Screen.REPORT_DETAIL, Screen.CREATE_REPORT, Screen.NOTIFICATIONS))

                    if (isTabNavigation) {
                        (fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.98f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                            )
                    } else {
                        val isForward = targetState.ordinal > initialState.ordinal
                        if (isForward) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width / 3 } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width / 3 } + fadeOut()
                            )
                        }
                    }
                },
                label = "ScreenNavigationTransition"
            ) { screen ->
                when (screen) {
                    Screen.SPLASH -> {
                        SplashTracScreen(
                            onLoginClick = {
                                bannerErrorMessage = null
                                authViewModel.resetState()
                                currentScreen = Screen.LOGIN
                            },
                            onRegisterClick = {
                                bannerErrorMessage = null
                                authViewModel.resetState()
                                currentScreen = Screen.REGISTER
                            }
                        )
                    }

                    Screen.LOGIN -> {
                        LoginTracScreen(
                            isLoading = authState is AuthUiState.Loading,
                            errorMessage = bannerErrorMessage,
                            onLoginClick = { email, pass ->
                                bannerErrorMessage = null
                                authViewModel.login(email, pass)
                            },
                            onRegisterClick = {
                                bannerErrorMessage = null
                                authViewModel.resetState()
                                currentScreen = Screen.REGISTER
                            },
                            onForgotPasswordClick = {
                                bannerErrorMessage = "Fitur lupa kata sandi dapat diatur melalui Supabase Auth Dashboard."
                            }
                        )
                    }

                    Screen.REGISTER -> {
                        RegisterTracScreen(
                            isLoading = authState is AuthUiState.Loading,
                            errorMessage = bannerErrorMessage,
                            onSignUpClick = { name, email, userClass, pass, confirmPass ->
                                bannerErrorMessage = null
                                authViewModel.register(name, email, userClass, pass, confirmPass)
                            },
                            onLoginClick = {
                                bannerErrorMessage = null
                                authViewModel.resetState()
                                currentScreen = Screen.LOGIN
                            },
                            onTermsClick = {
                                bannerErrorMessage = null
                                currentScreen = Screen.TERMS
                            }
                        )
                    }

                    Screen.TERMS -> {
                        TermsTracScreen(
                            onBackClick = {
                                currentScreen = Screen.REGISTER
                            },
                            onAcceptClick = {
                                currentScreen = Screen.REGISTER
                            }
                        )
                    }

                    Screen.HOME -> {
                        HomeTracScreen(
                            userName = loggedInUserName,
                            userProfileImage = loggedInProfileImage,
                            isIndonesian = isIndonesianLanguage,
                            isDarkMode = isAppDarkMode,
                            reportsList = liveReportsList,
                            unreadNotificationCount = unreadNotifsCount,
                            onRefreshReports = {
                                reportViewModel.fetchReports()
                            },
                            onCreateReportClick = {
                                bannerErrorMessage = null
                                currentScreen = Screen.CREATE_REPORT
                            },
                            onViewAllReportsClick = {
                                currentScreen = Screen.REPORT_LIST
                            },
                            onReportsTabClick = {
                                currentScreen = Screen.REPORT_LIST
                            },
                            onReportItemClick = { reportData ->
                                selectedReport = reportData
                                previousScreen = Screen.HOME
                                currentScreen = Screen.REPORT_DETAIL
                            },
                            onNotificationClick = {
                                currentScreen = Screen.NOTIFICATIONS
                            },
                            onLogoutClick = {
                                authViewModel.syncCurrentUserRole { role, isAdmin ->
                                    loggedInUserRole = role
                                    isUserAdmin = isAdmin
                                }
                                isSidebarOpen = true
                            }
                        )
                    }

                    Screen.REPORT_LIST -> {
                        ReportListTracScreen(
                            reportsList = liveReportsList,
                            currentUserId = authViewModel.getLoggedInUserId(),
                            currentUserName = loggedInUserName,
                            isIndonesian = isIndonesianLanguage,
                            isDarkMode = isAppDarkMode,
                            onBackClick = {
                                currentScreen = Screen.HOME
                            },
                            onHomeTabClick = {
                                currentScreen = Screen.HOME
                            },
                            onCreateReportClick = {
                                bannerErrorMessage = null
                                currentScreen = Screen.CREATE_REPORT
                            },
                            onReportItemClick = { reportData ->
                                selectedReport = reportData
                                previousScreen = Screen.REPORT_LIST
                                currentScreen = Screen.REPORT_DETAIL
                            },
                            onLogoutClick = {
                                isSidebarOpen = true
                            }
                        )
                    }

                    Screen.REPORT_DETAIL -> {
                        val activeReport = liveReportsList.find { it.id == selectedReport?.id } ?: selectedReport
                        ReportDetailTracScreen(
                            reportData = activeReport,
                            isIndonesian = isIndonesianLanguage,
                            isDarkMode = isAppDarkMode,
                            onUpvoteClick = { reportId ->
                                reportViewModel.upvoteReport(reportId)
                            },
                            onBackClick = {
                                currentScreen = previousScreen
                            },
                            onHomeTabClick = {
                                currentScreen = Screen.HOME
                            },
                            onReportsTabClick = {
                                currentScreen = Screen.REPORT_LIST
                            },
                            onCreateReportClick = {
                                bannerErrorMessage = null
                                currentScreen = Screen.CREATE_REPORT
                            },
                            onLogoutClick = {
                                isSidebarOpen = true
                            }
                        )
                    }

                    Screen.CREATE_REPORT -> {
                        CreateReportTracScreen(
                            isLoading = reportUiState is ReportUiState.Loading,
                            errorMessage = bannerErrorMessage,
                            isIndonesian = isIndonesianLanguage,
                            isDarkMode = isAppDarkMode,
                            onBackClick = {
                                currentScreen = Screen.HOME
                            },
                            onHomeTabClick = {
                                currentScreen = Screen.HOME
                            },
                            onReportsTabClick = {
                                currentScreen = Screen.REPORT_LIST
                            },
                            onSubmitReportClick = { category, location, title, desc, imageUrl, priority ->
                                bannerErrorMessage = null
                                reportViewModel.createReport(category, location, title, desc, imageUrl, priority)
                            },
                            onLogoutClick = {
                                isSidebarOpen = true
                            }
                        )
                    }

                    Screen.NOTIFICATIONS -> {
                        NotificationsTracScreen(
                            reportsList = liveReportsList,
                            isIndonesian = isIndonesianLanguage,
                            isDarkMode = isAppDarkMode,
                            onNotificationItemClick = { notifItem ->
                                val matchingReport = liveReportsList.find {
                                    it.id == notifItem.reportId
                                } ?: liveReportsList.firstOrNull()

                                if (matchingReport != null) {
                                    selectedReport = matchingReport
                                }
                                previousScreen = Screen.NOTIFICATIONS
                                currentScreen = Screen.REPORT_DETAIL
                            }
                        )
                    }

                    Screen.EDIT_IDENTITY -> {
                        EditIdentityTracScreen(
                            currentName = loggedInUserName,
                            currentClass = loggedInUserClass,
                            currentRole = if (isUserAdmin) {
                                if (isIndonesianLanguage) "Pengurus / Admin" else "Administrator"
                            } else {
                                if (isIndonesianLanguage) "Siswa / Pelapor" else "Student / Reporter"
                            },
                            currentProfileImage = loggedInProfileImage,
                            isLoading = authState is AuthUiState.Loading,
                            isIndonesian = isIndonesianLanguage,
                            isDarkMode = isAppDarkMode,
                            onBackClick = {
                                currentScreen = Screen.HOME
                            },
                            onUpdateIdentityClick = { newName, newClass, newImage ->
                                authViewModel.updateProfile(newName, newClass, newImage) { result ->
                                    result.onSuccess {
                                        loggedInUserName = newName
                                        loggedInUserClass = newClass
                                        if (!newImage.isNullOrBlank()) {
                                            loggedInProfileImage = newImage
                                        }
                                        Toast.makeText(
                                            context,
                                            if (isIndonesianLanguage) "Identitas berhasil diperbarui!" else "Identity updated successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        currentScreen = Screen.HOME
                                    }.onFailure { error ->
                                        Toast.makeText(
                                            context,
                                            error.localizedMessage ?: "Gagal memperbarui identitas",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }

                    Screen.SETTINGS -> {
                        SettingsTracScreen(
                            userName = loggedInUserName,
                            userEmail = authViewModel.getLoggedInUserEmail(),
                            userProfileImage = loggedInProfileImage,
                            userRoleInitial = loggedInUserRole,
                            isIndonesianInitial = isIndonesianLanguage,
                            isDarkModeInitial = isAppDarkMode,
                            onBackClick = {
                                currentScreen = Screen.HOME
                            },
                            onLanguageChange = { isIndo ->
                                isIndonesianLanguage = isIndo
                                sessionPrefs.saveLanguage(isIndo)
                            },
                            onThemeChange = { isDark ->
                                isAppDarkMode = isDark
                                sessionPrefs.saveDarkMode(isDark)
                            },
                            onRoleChange = { newRole ->
                                authViewModel.setUserRole(newRole)
                                loggedInUserRole = newRole
                                isUserAdmin = authViewModel.isUserAdmin()
                                Toast.makeText(
                                    context,
                                    if (isIndonesianLanguage) "Peran diubah ke: $newRole" else "Role updated to: $newRole",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onLogoutClick = handleLogout
                        )
                    }

                    Screen.ADMIN_DASHBOARD -> {
                        AdminDashboardTracScreen(
                            adminName = loggedInUserName,
                            adminEmail = authViewModel.getLoggedInUserEmail(),
                            isSuperAdmin = authViewModel.isSuperAdmin(),
                            adminEmails = adminEmails,
                            isIndonesian = isIndonesianLanguage,
                            isDarkMode = isAppDarkMode,
                            reportsList = liveReportsList,
                            registeredUsers = registeredUsers,
                            selectedReportInitial = selectedReport,
                            onBackToUserModeClick = {
                                currentScreen = Screen.HOME
                            },
                            onUpdateReportStatus = { reportId, newStatus, completionImg, completionNote ->
                                reportViewModel.updateReportStatus(reportId, newStatus, completionImg, completionNote)
                                val statusLabel = when (newStatus.lowercase()) {
                                    "completed", "selesai" -> if (isIndonesianLanguage) "Selesai (Bukti Terlampir)" else "Resolved (Proof Attached)"
                                    "in progress", "diproses" -> if (isIndonesianLanguage) "Sedang Diproses" else "In Progress"
                                    else -> if (isIndonesianLanguage) "Menunggu (Pending)" else "Pending"
                                }
                                Toast.makeText(
                                    context,
                                    if (isIndonesianLanguage) "Status laporan berhasil diubah: $statusLabel" else "Report status updated: $statusLabel",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onToggleUserAdminRole = { targetEmail, makeAdmin ->
                                if (makeAdmin) {
                                    authViewModel.promoteUserToAdmin(targetEmail) {
                                        isUserAdmin = authViewModel.isUserAdmin()
                                        loggedInUserRole = authViewModel.getLoggedInUserRole()
                                        authViewModel.refreshAdminEmails()
                                    }
                                    Toast.makeText(
                                        context,
                                        if (isIndonesianLanguage) "$targetEmail berhasil dijadikan Admin" else "$targetEmail promoted to Admin",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    authViewModel.demoteAdminToUser(targetEmail) {
                                        isUserAdmin = authViewModel.isUserAdmin()
                                        loggedInUserRole = authViewModel.getLoggedInUserRole()
                                        authViewModel.refreshAdminEmails()
                                    }
                                    Toast.makeText(
                                        context,
                                        if (isIndonesianLanguage) "Hak Admin $targetEmail dicabut" else "Admin role revoked from $targetEmail",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }



            // Fixed Stationary Bottom Navigation Bar
            if (showBottomBar) {
                TracBottomNavBar(
                    currentScreen = currentScreen,
                    isIndonesian = isIndonesianLanguage,
                    isDarkMode = isAppDarkMode,
                    onTabSelected = { newScreen ->
                        bannerErrorMessage = null
                        if (newScreen == Screen.HOME || newScreen == Screen.REPORT_LIST) {
                            reportViewModel.fetchReports()
                        }
                        currentScreen = newScreen
                    },
                    onCreateReportClick = {
                        bannerErrorMessage = null
                        currentScreen = Screen.CREATE_REPORT
                    },
                    onProfileClick = {
                        authViewModel.syncCurrentUserRole { role, isAdmin ->
                            loggedInUserRole = role
                            isUserAdmin = isAdmin
                        }
                        isSidebarOpen = true
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Right-Side Profile Sidebar Drawer
            ProfileSidebarDrawer(
                isOpen = isSidebarOpen,
                userName = loggedInUserName,
                userClass = loggedInUserClass,
                userRole = if (isUserAdmin) (if (isIndonesianLanguage) "Pengurus / Admin" else "Administrator") else loggedInUserRole,
                userProfileImage = loggedInProfileImage,
                isAdmin = isUserAdmin,
                isIndonesian = isIndonesianLanguage,
                isDarkMode = isAppDarkMode,
                currentScreen = currentScreen,
                onClose = { isSidebarOpen = false },
                onMenuItemClick = { menuItem ->
                    when (menuItem) {
                        SidebarMenuItem.HOME -> currentScreen = Screen.HOME
                        SidebarMenuItem.CREATE_REPORT -> currentScreen = Screen.CREATE_REPORT
                        SidebarMenuItem.MY_REPORTS, SidebarMenuItem.HISTORY -> currentScreen = Screen.REPORT_LIST
                        SidebarMenuItem.ADMIN_PANEL -> {
                            if (isUserAdmin) {
                                currentScreen = Screen.ADMIN_DASHBOARD
                            } else {
                                Toast.makeText(context, "Akses ditolak: Hanya Admin yang dapat mengakses panel ini.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        SidebarMenuItem.PROFILE -> currentScreen = Screen.EDIT_IDENTITY
                        SidebarMenuItem.SETTINGS -> currentScreen = Screen.SETTINGS
                    }
                },
                onLogoutClick = handleLogout
            )
        }
    }
}
