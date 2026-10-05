package com.example.trac

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.trac.components.ZoomableImageViewerDialog
import com.example.trac.data.FacilityLocation
import com.example.trac.data.ReportData
import com.example.trac.data.ReportRepository
import com.example.trac.data.SchoolFacilityMasterData
import com.example.trac.data.SessionPreferences
import com.example.trac.data.StaffMember
import com.example.trac.data.UserProfileData
import com.example.trac.util.ImageUtils
import kotlinx.coroutines.launch

enum class AdminSubTab {
    DASHBOARD,
    REPORTS,
    REPORT_DETAIL,
    ASSIGN_STAFF,
    USERS_PELAPOR,
    CATEGORIES_LOCATIONS,
    ADMIN_NOTIF
}

data class StudentReporter(
    val id: String,
    val name: String,
    val className: String,
    val email: String,
    val totalReports: Int,
    val resolvedReports: Int
)

data class AdminNotifItem(
    val id: String,
    val title: String,
    val desc: String,
    val timeAgo: String,
    val isUrgent: Boolean,
    var isRead: Boolean = false,
    val reportId: String? = null
)

@Composable
fun AdminDashboardTracScreen(
    adminName: String = "Admin TRAC",
    adminEmail: String = "rompisjosh@gmail.com",
    isSuperAdmin: Boolean = true,
    adminEmails: Set<String> = setOf("rompisjosh@gmail.com"),
    isIndonesian: Boolean = true,
    isDarkMode: Boolean = false,
    reportsList: List<ReportData> = emptyList(),
    registeredUsers: List<UserProfileData> = emptyList(),
    selectedReportInitial: ReportData? = null,
    onBackToUserModeClick: () -> Unit = {},
    onUpdateReportStatus: (reportId: String, newStatus: String, completionImageUrl: String?, completionNotes: String?) -> Unit = { _, _, _, _ -> },
    onToggleUserAdminRole: (email: String, shouldBeAdmin: Boolean) -> Unit = { _, _ -> }
) {
    // Dynamic Theme Tokens matching standard TRAC visual system
    val pageBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

    var currentTab by rememberSaveable { mutableStateOf(AdminSubTab.DASHBOARD) }
    var selectedReport by remember {
        mutableStateOf(selectedReportInitial ?: reportsList.firstOrNull())
    }

    if (selectedReport == null && reportsList.isNotEmpty()) {
        selectedReport = reportsList.first()
    }
    val currentSelectedReport = reportsList.find { it.id == selectedReport?.id } ?: selectedReport

    val context = LocalContext.current
    val sessionPrefs = remember { SessionPreferences(context) }
    val reportRepo = remember { ReportRepository() }
    val coroutineScope = rememberCoroutineScope()
    var staffList by remember { mutableStateOf(sessionPrefs.getStaffList()) }

    // Real school facility locations across all 4 floors loaded from persistent storage
    var facilityLocations by remember {
        mutableStateOf(sessionPrefs.getFacilityLocations())
    }

    var currentAdminEmails by remember(adminEmails) {
        mutableStateOf(HashSet(adminEmails))
    }

    // Dynamic Admin Notifications synced with live reports & persistent read tracking
    var readAdminNotifIds by remember { mutableStateOf(HashSet(sessionPrefs.getReadAdminNotificationIds())) }

    // Remote sync with Supabase on screen load
    LaunchedEffect(Unit) {
        reportRepo.getStaffList().onSuccess { remoteStaff ->
            if (remoteStaff.isNotEmpty()) {
                staffList = remoteStaff
                sessionPrefs.saveStaffList(remoteStaff)
            }
        }
        reportRepo.getFacilityLocations().onSuccess { remoteLocs ->
            if (remoteLocs.isNotEmpty()) {
                facilityLocations = remoteLocs
                sessionPrefs.saveFacilityLocations(remoteLocs)
            }
        }
        reportRepo.getReadNotificationIds().onSuccess { remoteReadIds ->
            if (remoteReadIds.isNotEmpty()) {
                sessionPrefs.markAllAdminNotificationsAsRead(remoteReadIds)
                readAdminNotifIds = HashSet(sessionPrefs.getReadAdminNotificationIds())
            }
        }
    }

    // Dynamic Student Reporters List aggregated from registered users and submitted reports
    val studentReporters = remember(registeredUsers, reportsList) {
        val userMap = mutableMapOf<String, StudentReporter>()

        // 1. Seed base default accounts
        val defaultSeed = listOf(
            StudentReporter("USR-101", "rompis", "XI RPL", "rompisjosh@gmail.com", 0, 0),
            StudentReporter("USR-102", "Rayya Hanifah", "XI RPL", "rayyahanifah@gmail.com", 0, 0),
            StudentReporter("USR-103", "Joshua Benjamin", "XI RPL", "joshua@gmail.com", 0, 0),
            StudentReporter("USR-104", "Kevin Sanjaya", "XI TKJ", "kevin@gmail.com", 0, 0)
        )
        defaultSeed.forEach { userMap[it.email.trim().lowercase()] = it }

        // 2. Add local cached registered users
        sessionPrefs.getRegisteredUsers().forEach { u ->
            val cleanEmail = u.email.trim().lowercase()
            userMap[cleanEmail] = StudentReporter(
                id = if (u.userId.isNotBlank()) u.userId else "USR-${cleanEmail.replace("@", "_").replace(".", "_")}",
                name = u.fullName,
                className = u.userClass,
                email = u.email,
                totalReports = 0,
                resolvedReports = 0
            )
        }

        // 3. Add remote registered users
        registeredUsers.forEach { u ->
            val cleanEmail = u.email.trim().lowercase()
            userMap[cleanEmail] = StudentReporter(
                id = if (u.userId.isNotBlank()) u.userId else "USR-${cleanEmail.replace("@", "_").replace(".", "_")}",
                name = u.fullName,
                className = u.userClass,
                email = u.email,
                totalReports = 0,
                resolvedReports = 0
            )
        }

        // 4. Discover reporters who submitted reports
        reportsList.forEach { r ->
            val author = r.userName?.trim()
            if (!author.isNullOrBlank()) {
                val candidateEmail = if (author.contains("@")) author.lowercase() else "${author.lowercase().replace(" ", "")}@gmail.com"
                if (!userMap.containsKey(candidateEmail)) {
                    userMap[candidateEmail] = StudentReporter(
                        id = if (!r.userId.isNullOrBlank()) r.userId else "USR-${candidateEmail.replace("@", "_").replace(".", "_")}",
                        name = author,
                        className = "XI RPL",
                        email = candidateEmail,
                        totalReports = 0,
                        resolvedReports = 0
                    )
                }
            }
        }

        // 5. Calculate live report statistics
        userMap.values.map { student ->
            val total = reportsList.count { r ->
                r.userName.equals(student.name, ignoreCase = true) ||
                (r.userId != null && r.userId == student.id)
            }
            val resolved = reportsList.count { r ->
                (r.userName.equals(student.name, ignoreCase = true) ||
                 (r.userId != null && r.userId == student.id)) &&
                r.status.equals("Completed", ignoreCase = true)
            }
            student.copy(totalReports = total, resolvedReports = resolved)
        }.sortedWith(
            compareByDescending<StudentReporter> { it.email.equals(SessionPreferences.SUPER_ADMIN_EMAIL, ignoreCase = true) }
                .thenByDescending { it.totalReports }
        )
    }

    val adminNotifications = remember(reportsList, readAdminNotifIds, isIndonesian) {
        val list = mutableListOf<AdminNotifItem>()

        // 1. Generate real notifications from all actual reports submitted by users/students
        reportsList.forEach { report ->
            val isUrgent = report.priority.equals("Darurat", ignoreCase = true) || report.priority.equals("Emergency", ignoreCase = true)
            val isCompleted = report.status.equals("Completed", ignoreCase = true)
            val idSuffix = report.id?.takeLast(6) ?: report.title.hashCode().toString()

            // A. Incoming / Active Report Alert
            val newNotifId = "admin_notif_new_$idSuffix"
            val newTitle = if (isUrgent) {
                if (isIndonesian) "🚨 Laporan Darurat: ${report.title}" else "🚨 Emergency Report: ${report.title}"
            } else {
                if (isIndonesian) "📥 Laporan Masuk: ${report.title}" else "📥 Incoming Report: ${report.title}"
            }
            val newDesc = if (isIndonesian) {
                "${report.location} • Dilaporkan oleh ${report.userName ?: "Pelapor"}. Kategori ${report.category}."
            } else {
                "${report.location} • Reported by ${report.userName ?: "Reporter"}. Category: ${report.category}."
            }

            list.add(
                AdminNotifItem(
                    id = newNotifId,
                    title = newTitle,
                    desc = newDesc,
                    timeAgo = report.createdAt?.take(10) ?: if (isIndonesian) "Hari ini" else "Today",
                    isUrgent = isUrgent,
                    isRead = readAdminNotifIds.contains(newNotifId),
                    reportId = report.id
                )
            )

            // B. If completed, show verified completion notice
            if (isCompleted) {
                val doneNotifId = "admin_notif_done_$idSuffix"
                val doneTitle = if (isIndonesian) {
                    "✅ Fasilitas Selesai: ${report.title}"
                } else {
                    "✅ Facility Resolved: ${report.title}"
                }
                val doneDesc = if (isIndonesian) {
                    "${report.location} • Perbaikan telah selesai. ${if (!report.completionNotes.isNullOrBlank()) "Catatan: " + report.completionNotes else "Bukti penanganan terlampir."}"
                } else {
                    "${report.location} • Repair marked complete. ${if (!report.completionNotes.isNullOrBlank()) "Notes: " + report.completionNotes else "Proof verified."}"
                }
                list.add(
                    AdminNotifItem(
                        id = doneNotifId,
                        title = doneTitle,
                        desc = doneDesc,
                        timeAgo = report.createdAt?.take(10) ?: if (isIndonesian) "Hari ini" else "Today",
                        isUrgent = false,
                        isRead = readAdminNotifIds.contains(doneNotifId),
                        reportId = report.id
                    )
                )
            }

            // C. If report has multiple student upvotes
            if (report.upvoteCount >= 2) {
                val upvoteNotifId = "admin_notif_up_$idSuffix"
                val upTitle = if (isIndonesian) {
                    "🔥 +${report.upvoteCount} Siswa Terdampak: ${report.title}"
                } else {
                    "🔥 +${report.upvoteCount} Students Affected: ${report.title}"
                }
                val upDesc = if (isIndonesian) {
                    "${report.location} • Banyak siswa menandai fasilitas ini mendesak diperbaiki."
                } else {
                    "${report.location} • Multiple students confirmed this issue urgently needs repair."
                }
                list.add(
                    AdminNotifItem(
                        id = upvoteNotifId,
                        title = upTitle,
                        desc = upDesc,
                        timeAgo = report.createdAt?.take(10) ?: if (isIndonesian) "Hari ini" else "Today",
                        isUrgent = true,
                        isRead = readAdminNotifIds.contains(upvoteNotifId),
                        reportId = report.id
                    )
                )
            }
        }

        // 2. System Monitoring Welcome Card
        val sysNotifId = "admin_notif_sys_hub"
        list.add(
            AdminNotifItem(
                id = sysNotifId,
                title = if (isIndonesian) "ℹ️ Sistem Monitoring Fasilitas TRAC" else "ℹ️ TRAC Facility Monitoring System",
                desc = if (isIndonesian)
                    "Notifikasi ini memantau laporan fasilitas sekolah dari siswa secara real-time. Ketuk notifikasi untuk membuka detail penugasan."
                else
                    "Real-time notifications for school facility reports submitted by students. Tap any notification to open report details.",
                timeAgo = if (isIndonesian) "Hari ini" else "Today",
                isUrgent = false,
                isRead = readAdminNotifIds.contains(sysNotifId),
                reportId = null
            )
        )

        // Sort: Unread first, then urgent first
        list.sortedWith(
            compareBy<AdminNotifItem> { it.isRead }
                .thenByDescending { it.isUrgent }
        )
    }

    val unreadAdminNotifCount = adminNotifications.count { !it.isRead }

    // Metrics counters
    val totalCount = reportsList.size
    val pendingCount = reportsList.count { it.status.equals("Pending", ignoreCase = true) }
    val inProgressCount = reportsList.count { it.status.equals("In Progress", ignoreCase = true) }
    val completedCount = reportsList.count { it.status.equals("Completed", ignoreCase = true) }
    val completionRate = if (totalCount > 0) (completedCount * 100) / totalCount else 100

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = pageBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Admin Hub Bar
            AdminTopBar(
                adminName = adminName,
                isSuperAdmin = isSuperAdmin,
                isIndonesian = isIndonesian,
                isDarkMode = isDarkMode,
                cardBg = cardBg,
                borderCol = borderCol,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onBackToUserMode = onBackToUserModeClick
            )

            // 2. Horizontal Sub-Tab Navigation Bar (7 Tabs)
            AdminSubTabRow(
                currentTab = currentTab,
                isIndonesian = isIndonesian,
                isDarkMode = isDarkMode,
                unreadNotifCount = unreadAdminNotifCount,
                onTabSelect = { currentTab = it }
            )

            // 3. Tab Content View with smooth animated transition
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(180)))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "AdminSubTabTransition"
            ) { tab ->
                when (tab) {
                    AdminSubTab.DASHBOARD -> {
                        AdminDashboardView(
                            isIndonesian = isIndonesian,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            totalCount = totalCount,
                            pendingCount = pendingCount,
                            inProgressCount = inProgressCount,
                            completedCount = completedCount,
                            completionRate = completionRate,
                            staffCount = staffList.size,
                            reportsList = reportsList,
                            onNavigateToTab = { currentTab = it },
                            onOpenReportDetail = { report ->
                                selectedReport = report
                                currentTab = AdminSubTab.REPORT_DETAIL
                            }
                        )
                    }

                    AdminSubTab.REPORTS -> {
                        AdminReportsListView(
                            reportsList = reportsList,
                            isIndonesian = isIndonesian,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onUpdateStatus = { reportId, newStatus, completionImg, completionNote ->
                                onUpdateReportStatus(reportId, newStatus, completionImg, completionNote)
                            },
                            onOpenDetail = { report ->
                                selectedReport = report
                                currentTab = AdminSubTab.REPORT_DETAIL
                            }
                        )
                    }

                    AdminSubTab.REPORT_DETAIL -> {
                        AdminReportDetailView(
                            report = currentSelectedReport,
                            staffList = staffList,
                            isIndonesian = isIndonesian,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onUpdateStatus = { reportId, newStatus, completionImg, completionNote ->
                                onUpdateReportStatus(reportId, newStatus, completionImg, completionNote)
                                selectedReport = selectedReport?.copy(
                                    status = newStatus,
                                    completionImageUrl = completionImg ?: selectedReport?.completionImageUrl,
                                    completionNotes = completionNote ?: selectedReport?.completionNotes
                                )
                            },
                            onBackToReports = { currentTab = AdminSubTab.REPORTS }
                        )
                    }

                    AdminSubTab.ASSIGN_STAFF -> {
                        AdminAssignStaffView(
                            staffList = staffList,
                            reportsList = reportsList,
                            isIndonesian = isIndonesian,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onAssignStaff = { staffId, reportId ->
                                var newTasks = 0
                                staffList = staffList.map {
                                    if (it.id == staffId) {
                                        newTasks = it.activeTasks + 1
                                        it.copy(activeTasks = newTasks)
                                    } else it
                                }
                                sessionPrefs.saveStaffList(staffList)
                                coroutineScope.launch {
                                    reportRepo.updateStaffActiveTasks(staffId, newTasks)
                                }
                            },
                            onAddStaff = { newStaff ->
                                staffList = staffList + newStaff
                                sessionPrefs.saveStaffList(staffList)
                                coroutineScope.launch {
                                    reportRepo.addStaff(newStaff)
                                }
                            },
                            onDeleteStaff = { staffId ->
                                staffList = staffList.filterNot { it.id == staffId }
                                sessionPrefs.saveStaffList(staffList)
                                coroutineScope.launch {
                                    reportRepo.deleteStaff(staffId)
                                }
                            },
                            onToggleStaffAvailability = { staffId ->
                                var newAvail = true
                                staffList = staffList.map {
                                    if (it.id == staffId) {
                                        newAvail = !it.isAvailable
                                        it.copy(isAvailable = newAvail)
                                    } else it
                                }
                                sessionPrefs.saveStaffList(staffList)
                                coroutineScope.launch {
                                    reportRepo.updateStaffAvailability(staffId, newAvail)
                                }
                            }
                        )
                    }

                    AdminSubTab.USERS_PELAPOR -> {
                        AdminUsersPelaporView(
                            students = studentReporters,
                            isSuperAdmin = isSuperAdmin,
                            adminEmails = currentAdminEmails,
                            isIndonesian = isIndonesian,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onToggleAdmin = { email, makeAdmin ->
                                val clean = email.trim().lowercase()
                                val affectedEmails = if (clean.contains("rayya")) {
                                    setOf(clean, "rayya@gmail.com", "rayyahanifah@gmail.com", "rayyahanifahh@gmail.com")
                                } else {
                                    setOf(clean)
                                }
                                currentAdminEmails = if (makeAdmin) {
                                    HashSet(currentAdminEmails + affectedEmails)
                                } else {
                                    HashSet(currentAdminEmails - affectedEmails)
                                }
                                onToggleUserAdminRole(email, makeAdmin)
                            }
                        )
                    }

                    AdminSubTab.CATEGORIES_LOCATIONS -> {
                        AdminCategoriesLocationsView(
                            locations = facilityLocations,
                            reportsList = reportsList,
                            isIndonesian = isIndonesian,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onAddRoomToFloor = { floorName, roomName ->
                                val updated = facilityLocations.map { loc ->
                                    if (loc.floorName.equals(floorName, ignoreCase = true) && !loc.rooms.contains(roomName)) {
                                        loc.copy(
                                            rooms = loc.rooms + roomName,
                                            roomCount = loc.rooms.size + 1
                                        )
                                    } else loc
                                }
                                facilityLocations = updated
                                sessionPrefs.saveFacilityLocations(updated)
                                val desc = facilityLocations.find { it.floorName.equals(floorName, ignoreCase = true) }?.floorDesc ?: ""
                                coroutineScope.launch {
                                    reportRepo.addRoomToFloor(floorName, roomName, desc)
                                }
                            },
                            onDeleteRoomFromFloor = { floorName, roomName ->
                                val updated = facilityLocations.map { loc ->
                                    if (loc.floorName.equals(floorName, ignoreCase = true)) {
                                        loc.copy(
                                            rooms = loc.rooms.filterNot { it.equals(roomName, ignoreCase = true) },
                                            roomCount = maxOf(0, loc.rooms.size - 1)
                                        )
                                    } else loc
                                }
                                facilityLocations = updated
                                sessionPrefs.saveFacilityLocations(updated)
                                coroutineScope.launch {
                                    reportRepo.deleteRoomFromFloor(floorName, roomName)
                                }
                            }
                        )
                    }

                    AdminSubTab.ADMIN_NOTIF -> {
                        AdminNotifView(
                            notifications = adminNotifications,
                            isIndonesian = isIndonesian,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onMarkAllRead = {
                                val notifIds = adminNotifications.map { it.id }
                                sessionPrefs.markAllAdminNotificationsAsRead(notifIds)
                                readAdminNotifIds = HashSet(sessionPrefs.getReadAdminNotificationIds())
                                coroutineScope.launch {
                                    reportRepo.markAllNotificationsAsReadInDb(notifIds)
                                }
                            },
                            onNotificationClick = { notif ->
                                sessionPrefs.markAdminNotificationAsRead(notif.id)
                                readAdminNotifIds = HashSet(sessionPrefs.getReadAdminNotificationIds())
                                coroutineScope.launch {
                                    reportRepo.markNotificationAsReadInDb(notif.id, notif.title, notif.desc)
                                }
                                val matchedReport = reportsList.find { it.id == notif.reportId }
                                if (matchedReport != null) {
                                    selectedReport = matchedReport
                                    currentTab = AdminSubTab.REPORT_DETAIL
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. TOP BAR
// -------------------------------------------------------------
@Composable
private fun AdminTopBar(
    adminName: String,
    isSuperAdmin: Boolean,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onBackToUserMode: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, spotColor = Color(0x12000000)),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TRAC Brand Shield Icon Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AdminShieldCrownIcon(color = Color.White, size = 20.dp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Main Info Column with weight(1f) to prevent squeezing the right button
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isIndonesian) "Panel Admin" else "Admin Hub",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Text(
                            text = if (isIndonesian) {
                                if (isSuperAdmin) "ADMIN UTAMA" else "ADMIN"
                            } else {
                                if (isSuperAdmin) "SUPER ADMIN" else "ADMIN"
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isIndonesian) "Fasilitas TRAC • $adminName" else "School Facilities • $adminName",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Button: Back to Reporter Mode
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBackToUserMode() },
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "←",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isIndonesian) "Mode Pelapor" else "Reporter Mode",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. HORIZONTAL SUB-TAB SELECTOR (7 TABS)
// -------------------------------------------------------------
@Composable
private fun AdminSubTabRow(
    currentTab: AdminSubTab,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    unreadNotifCount: Int = 0,
    onTabSelect: (AdminSubTab) -> Unit
) {
    val tabs = listOf(
        AdminSubTab.DASHBOARD to (if (isIndonesian) "Dasbor" else "Dashboard"),
        AdminSubTab.REPORTS to (if (isIndonesian) "Laporan" else "Reports"),
        AdminSubTab.REPORT_DETAIL to (if (isIndonesian) "Detail Laporan" else "Report Detail"),
        AdminSubTab.ASSIGN_STAFF to (if (isIndonesian) "Tugaskan Staf" else "Assign Staff"),
        AdminSubTab.USERS_PELAPOR to (if (isIndonesian) "Daftar Pelapor" else "Reporters"),
        AdminSubTab.CATEGORIES_LOCATIONS to (if (isIndonesian) "Kategori & Lokasi" else "Categories & Rooms"),
        AdminSubTab.ADMIN_NOTIF to (if (isIndonesian) "Notifikasi" else "Notifications")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDarkMode) Color(0xFF131D2F) else Color(0xFFF1F5F9)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { (tab, label) ->
                val isSelected = currentTab == tab
                val bg = if (isSelected) Color(0xFF2563EB) else if (isDarkMode) Color(0xFF1E293B) else Color.White
                val textColor = if (isSelected) Color.White else if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                val border = if (isSelected) Color(0xFF1D4ED8) else if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTabSelect(tab) },
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    border = BorderStroke(1.dp, border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdminTabIcon(tab = tab, color = textColor, size = 15.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor
                        )
                        if (tab == AdminSubTab.ADMIN_NOTIF && unreadNotifCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White else Color(0xFFEF4444)
                            ) {
                                Text(
                                    text = "$unreadNotifCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color(0xFF2563EB) else Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. TAB 1: DASHBOARD
// -------------------------------------------------------------
@Composable
private fun AdminDashboardView(
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    totalCount: Int,
    pendingCount: Int,
    inProgressCount: Int,
    completedCount: Int,
    completionRate: Int,
    staffCount: Int,
    reportsList: List<ReportData>,
    onNavigateToTab: (AdminSubTab) -> Unit,
    onOpenReportDetail: (ReportData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Banner in TRAC Blue
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2563EB).copy(alpha = 0.4f) else Color(0xFFBFDBFE))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = if (isIndonesian) "Ringkasan Operasional Fasilitas" else "Facility Operational Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF1E40AF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isIndonesian)
                        "Pantau seluruh laporan sarana prasarana sekolah, tugaskan teknisi, dan ubah status pengerjaan secara real-time."
                    else
                        "Monitor school facility reports, assign technical staff, and update resolution progress in real time.",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Metrics Grid
        Text(
            text = if (isIndonesian) "Statistik Utama" else "Key Metrics",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = if (isIndonesian) "Total Laporan" else "Total Reports",
                value = "$totalCount",
                subtitle = if (isIndonesian) "Laporan tercatat" else "Recorded",
                cardColor = cardBg,
                accentColor = Color(0xFF2563EB),
                borderColor = borderCol,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            MetricCard(
                title = if (isIndonesian) "Menunggu (Pending)" else "Pending",
                value = "$pendingCount",
                subtitle = if (isIndonesian) "Perlu ditindak" else "Action needed",
                cardColor = cardBg,
                accentColor = Color(0xFFD97706),
                borderColor = borderCol,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = if (isIndonesian) "Sedang Diproses" else "In Progress",
                value = "$inProgressCount",
                subtitle = if (isIndonesian) "Oleh teknisi" else "Under repair",
                cardColor = cardBg,
                accentColor = Color(0xFF3B82F6),
                borderColor = borderCol,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            MetricCard(
                title = if (isIndonesian) "Selesai (Resolved)" else "Resolved",
                value = "$completedCount",
                subtitle = "$completionRate% " + if (isIndonesian) "terselesaikan" else "resolved",
                cardColor = cardBg,
                accentColor = Color(0xFF10B981),
                borderColor = borderCol,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Row
        Text(
            text = if (isIndonesian) "Aksi Cepat Admin" else "Quick Actions",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminActionButton(
                label = if (isIndonesian) "Kelola Laporan" else "Manage Reports",
                color = Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(AdminSubTab.REPORTS) }
            )

            AdminActionButton(
                label = if (isIndonesian) "Tugaskan Staf" else "Assign Staff",
                color = Color(0xFF0284C7),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(AdminSubTab.ASSIGN_STAFF) }
            )

            AdminActionButton(
                label = if (isIndonesian) "Data Lokasi" else "Locations",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(AdminSubTab.CATEGORIES_LOCATIONS) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Incoming Reports Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isIndonesian) "Laporan Masuk Terbaru" else "Recent Incoming Reports",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Text(
                text = if (isIndonesian) "Lihat Semua →" else "View All →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB),
                modifier = Modifier.clickable { onNavigateToTab(AdminSubTab.REPORTS) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (reportsList.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isIndonesian) "Belum ada laporan masuk dari pelapor." else "No reports submitted yet.",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }
            }
        } else {
            reportsList.take(4).forEach { report ->
                AdminCompactReportCard(
                    report = report,
                    isIndonesian = isIndonesian,
                    cardBg = cardBg,
                    borderCol = borderCol,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { onOpenReportDetail(report) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 4. TAB 2: LAPORAN (REPORTS LIST WITH STATUS CONTROL)
// -------------------------------------------------------------
@Composable
private fun AdminReportsListView(
    reportsList: List<ReportData>,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onUpdateStatus: (reportId: String, newStatus: String, completionImageUrl: String?, completionNotes: String?) -> Unit,
    onOpenDetail: (ReportData) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    var reportToResolve by remember { mutableStateOf<ReportData?>(null) }
    var completionNotesInput by remember { mutableStateOf("") }
    var completionImageBase64 by remember { mutableStateOf<String?>(null) }
    var zoomedImageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var zoomedImageTitle by remember { mutableStateOf("Pratinjau Foto") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val base64 = ImageUtils.uriToBase64(context, uri)
                completionImageBase64 = base64
            }
        }
    }

    val filteredList = remember(reportsList, searchQuery, selectedFilter) {
        reportsList.filter { report ->
            val matchesQuery = searchQuery.isBlank() ||
                    report.title.contains(searchQuery, ignoreCase = true) ||
                    report.location.contains(searchQuery, ignoreCase = true) ||
                    (report.userName ?: "").contains(searchQuery, ignoreCase = true) ||
                    report.category.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Pending" -> report.status.equals("Pending", ignoreCase = true)
                "In Progress" -> report.status.equals("In Progress", ignoreCase = true)
                "Completed" -> report.status.equals("Completed", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchVectorIcon(color = textSecondary, size = 18.dp)
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        fontSize = 13.5.sp,
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isIndonesian) "Cari judul, lokasi, atau pelapor..." else "Search title, room, or reporter...",
                                fontSize = 13.5.sp,
                                color = textSecondary
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "✕",
                        fontSize = 14.sp,
                        color = textSecondary,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        val filters = listOf(
            "All" to (if (isIndonesian) "Semua (${reportsList.size})" else "All (${reportsList.size})"),
            "Pending" to (if (isIndonesian) "Menunggu (${reportsList.count { it.status.equals("Pending", ignoreCase = true) }})" else "Pending (${reportsList.count { it.status.equals("Pending", ignoreCase = true) }})"),
            "In Progress" to (if (isIndonesian) "Diproses (${reportsList.count { it.status.equals("In Progress", ignoreCase = true) }})" else "In Progress (${reportsList.count { it.status.equals("In Progress", ignoreCase = true) }})"),
            "Completed" to (if (isIndonesian) "Selesai (${reportsList.count { it.status.equals("Completed", ignoreCase = true) }})" else "Resolved (${reportsList.count { it.status.equals("Completed", ignoreCase = true) }})")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (key, label) ->
                val isSelected = selectedFilter == key
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedFilter = key },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF2563EB) else if (isDarkMode) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF1D4ED8) else borderCol)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Lazy Reports List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isIndonesian) "Tidak ada laporan yang sesuai dengan filter." else "No reports match the filter.",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredList, key = { it.id ?: it.title }) { report ->
                    AdminReportManageCard(
                        report = report,
                        isIndonesian = isIndonesian,
                        isDarkMode = isDarkMode,
                        cardBg = cardBg,
                        borderCol = borderCol,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onUpdateStatus = { newStatus ->
                            report.id?.let { onUpdateStatus(it, newStatus, null, null) }
                        },
                        onOpenResolveDialog = {
                            reportToResolve = report
                            completionNotesInput = report.completionNotes ?: ""
                            completionImageBase64 = report.completionImageUrl
                        },
                        onOpenDetail = { onOpenDetail(report) },
                        onPreviewPhoto = { bmp, title ->
                            zoomedImageBitmap = bmp
                            zoomedImageTitle = title
                        }
                    )
                }
            }
        }
    }

    // Resolution & Proof Dialog for Reports List (ensuring proof prompt is NEVER bypassed)
    if (reportToResolve != null) {
        val target = reportToResolve!!
        AlertDialog(
            onDismissRequest = { reportToResolve = null },
            containerColor = cardBg,
            title = {
                Text(
                    text = if (isIndonesian) "Selesaikan Laporan & Bukti Perbaikan" else "Resolve Report & Add Proof",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isIndonesian)
                            "Selesaikan #${target.id?.takeLast(5) ?: "TRAC"}: '${target.title}'. Masukkan catatan perbaikan dan foto bukti setelah penanganan:"
                        else
                            "Resolve #${target.id?.takeLast(5) ?: "TRAC"}: '${target.title}'. Add technician notes and completion proof photo:",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    OutlinedTextField(
                        value = completionNotesInput,
                        onValueChange = { completionNotesInput = it },
                        label = { Text(if (isIndonesian) "Catatan Perbaikan" else "Resolution Notes", fontSize = 12.sp) },
                        placeholder = { Text(if (isIndonesian) "Contoh: Sudah diperbaiki dan dites berfungsi normal." else "E.g. Repaired and tested working.", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        )
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            },
                        color = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        val compImg = completionImageBase64
                        if (compImg != null) {
                            val bmp = remember(compImg) {
                                ImageUtils.base64ToBitmap(compImg)
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Completion Proof",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📷", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isIndonesian) "Pilih Foto Bukti Selesai (Opsional)" else "Pick After Photo (Optional)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        target.id?.let {
                            onUpdateStatus(it, "Completed", completionImageBase64, completionNotesInput.trim())
                        }
                        reportToResolve = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(if (isIndonesian) "Simpan & Selesai" else "Mark Resolved", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToResolve = null }) {
                    Text(if (isIndonesian) "Batal" else "Cancel", color = textSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    if (zoomedImageBitmap != null) {
        ZoomableImageViewerDialog(
            imageBitmap = zoomedImageBitmap,
            title = zoomedImageTitle,
            subtitle = null,
            onDismiss = { zoomedImageBitmap = null }
        )
    }
}

// -------------------------------------------------------------
// 5. TAB 3: REPORT DETAIL (ADMIN VIEW)
// -------------------------------------------------------------
@Composable
private fun AdminReportDetailView(
    report: ReportData?,
    staffList: List<StaffMember>,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onUpdateStatus: (reportId: String, newStatus: String, completionImageUrl: String?, completionNotes: String?) -> Unit,
    onBackToReports: () -> Unit
) {
    if (report == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isIndonesian) "Pilih laporan dari tab 'Laporan' untuk melihat detail admin." else "Select a report from 'Reports' tab to view details.",
                    fontSize = 13.5.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBackToReports) {
                    Text(if (isIndonesian) "Buka Daftar Laporan" else "Open Reports List")
                }
            }
        }
        return
    }

    var currentStatus by remember(report.status) { mutableStateOf(report.status) }
    var selectedStaffName by remember { mutableStateOf(staffList.firstOrNull()?.name ?: "Tim Fasilitas") }

    var showCompletionDialog by remember { mutableStateOf(false) }
    var completionNotesInput by remember(report.id) { mutableStateOf(report.completionNotes ?: "") }
    var completionImageBase64 by remember(report.id) { mutableStateOf<String?>(report.completionImageUrl) }
    var zoomedImageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var zoomedImageTitle by remember { mutableStateOf("Pratinjau Foto") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val base64 = ImageUtils.uriToBase64(context, uri)
                completionImageBase64 = base64
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBackToReports() },
                shape = RoundedCornerShape(10.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "←", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIndonesian) "Kembali" else "Back",
                        fontSize = 12.sp,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (isIndonesian) "Kelola Laporan ID #${report.id?.takeLast(5) ?: "TRAC"}" else "Manage Report #${report.id?.takeLast(5) ?: "TRAC"}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Current Active Status Hero Banner
        val isCompletedStatus = currentStatus.equals("Completed", ignoreCase = true)
        val isInProgressStatus = currentStatus.equals("In Progress", ignoreCase = true)
        
        val heroBg = when {
            isCompletedStatus -> if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFECFDF5)
            isInProgressStatus -> if (isDarkMode) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFFEFF6FF)
            else -> if (isDarkMode) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFFFBEB)
        }
        val heroBorder = when {
            isCompletedStatus -> Color(0xFF10B981)
            isInProgressStatus -> Color(0xFF2563EB)
            else -> Color(0xFFF59E0B)
        }
        val heroIcon = when {
            isCompletedStatus -> "✅"
            isInProgressStatus -> "⚙️"
            else -> "⏳"
        }
        val heroTitle = when {
            isCompletedStatus -> if (isIndonesian) "STATUS: SELESAI DIPERBAIKI" else "STATUS: RESOLVED & VERIFIED"
            isInProgressStatus -> if (isIndonesian) "STATUS: SEDANG DIPROSES" else "STATUS: UNDER REPAIR / IN PROGRESS"
            else -> if (isIndonesian) "STATUS: MENUNGGU PENANGANAN" else "STATUS: PENDING ACTION"
        }
        val heroSubtitle = when {
            isCompletedStatus -> if (isIndonesian) "Perbaikan fasilitas telah rampung. Bukti penyelesaian tersimpan di bawah." else "Facility repair complete. Proof and resolution notes attached below."
            isInProgressStatus -> if (isIndonesian) "Teknisi sedang melakukan perbaikan di lokasi fasilitas." else "Technicians are currently on-site working on this facility."
            else -> if (isIndonesian) "Laporan tercatat dan sedang menunggu penugasan teknisi." else "Report recorded and awaiting technician dispatch."
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = heroBg,
            border = BorderStroke(1.5.dp, heroBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = heroIcon, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heroTitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = heroSubtitle,
                        fontSize = 11.5.sp,
                        color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Interactive Status Control Selector (3 Clear Option Cards)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isIndonesian) "Ganti Status Laporan (Pilih salah satu):" else "Change Progress Status (Select one):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusOptions = listOf(
                        Triple("Pending", if (isIndonesian) "Menunggu" else "Pending", Color(0xFFD97706)),
                        Triple("In Progress", if (isIndonesian) "Diproses" else "In Progress", Color(0xFF2563EB)),
                        Triple("Completed", if (isIndonesian) "Selesai" else "Resolved", Color(0xFF10B981))
                    )

                    statusOptions.forEach { (stKey, stLabel, accentCol) ->
                        val isCurrent = currentStatus.equals(stKey, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (stKey == "Completed") {
                                        completionNotesInput = report.completionNotes ?: ""
                                        completionImageBase64 = report.completionImageUrl
                                        showCompletionDialog = true
                                    } else {
                                        currentStatus = stKey
                                        report.id?.let { onUpdateStatus(it, stKey, null, null) }
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) accentCol else (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC)),
                            border = BorderStroke(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) accentCol else borderCol
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (stKey) {
                                        "Pending" -> "⏳"
                                        "In Progress" -> "⚙️"
                                        else -> "✅"
                                    },
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else textPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isCurrent) Color.White.copy(alpha = 0.25f) else Color.Transparent
                                ) {
                                    Text(
                                        text = if (isCurrent) (if (isIndonesian) "AKTIF ✓" else "ACTIVE ✓") else (if (isIndonesian) "Pilih" else "Tap"),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCurrent) Color.White else textSecondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Report Information Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = report.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryPill(category = report.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    LocationPill(location = report.location)
                    Spacer(modifier = Modifier.width(8.dp))
                    AdminPriorityBadge(priority = report.priority, isIndonesian = isIndonesian)
                    if (report.upvoteCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👍 +${report.upvoteCount}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = borderCol)

                Spacer(modifier = Modifier.height(14.dp))

                DetailItemRow(
                    label = if (isIndonesian) "Nama Pelapor" else "Reporter",
                    value = report.userName ?: if (isIndonesian) "Pelapor TRAC" else "TRAC Reporter",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                DetailItemRow(
                    label = if (isIndonesian) "Prioritas / Urgensi" else "Priority Level",
                    value = when (report.priority.lowercase()) {
                        "darurat", "emergency" -> if (isIndonesian) "DARURAT" else "EMERGENCY"
                        "rendah", "low" -> if (isIndonesian) "RENDAH" else "LOW"
                        "tinggi", "high" -> if (isIndonesian) "TINGGI" else "HIGH"
                        else -> if (isIndonesian) "SEDANG" else "MEDIUM"
                    },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                DetailItemRow(
                    label = if (isIndonesian) "Waktu Laporan" else "Reported At",
                    value = report.createdAt ?: if (isIndonesian) "Hari ini" else "Today",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                DetailItemRow(
                    label = if (isIndonesian) "Petugas Ditugaskan" else "Assigned Staff",
                    value = if (selectedStaffName == "Tim Fasilitas") {
                        if (isIndonesian) "Tim Fasilitas" else "School Facility Team"
                    } else selectedStaffName,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isIndonesian) "Deskripsi Kerusakan:" else "Issue Description:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = report.description,
                    fontSize = 13.5.sp,
                    color = textPrimary,
                    lineHeight = 20.sp
                )

                // Render Image if available
                if (!report.imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isIndonesian) "Foto Bukti Fasilitas (Sebelum):" else "Initial Photo (Before):",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val imageBitmap = remember(report.imageUrl) {
                        ImageUtils.base64ToBitmap(report.imageUrl)
                    }
                    if (imageBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    zoomedImageBitmap = imageBitmap.asImageBitmap()
                                    zoomedImageTitle = if (isIndonesian) "Foto Bukti Fasilitas (Sebelum)" else "Initial Evidence (Before)"
                                }
                        ) {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = "Report Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xCC0F172A)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🔍", fontSize = 11.sp)
                                    Text(
                                        text = if (isIndonesian) "Ketuk untuk perbesar" else "Tap to zoom",
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Render Before & After Proof section if completed
                if (currentStatus.equals("Completed", ignoreCase = true) && (!report.completionImageUrl.isNullOrBlank() || !report.completionNotes.isNullOrBlank())) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = borderCol)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isIndonesian) "✨ Bukti Selesai Diperbaiki (After):" else "✨ Completion Proof (After):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )

                    if (!report.completionImageUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val completionBmp = remember(report.completionImageUrl) {
                            ImageUtils.base64ToBitmap(report.completionImageUrl)
                        }
                        if (completionBmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        zoomedImageBitmap = completionBmp.asImageBitmap()
                                        zoomedImageTitle = if (isIndonesian) "✨ Bukti Selesai Diperbaiki (After)" else "✨ Completion Proof (After)"
                                    }
                            ) {
                                Image(
                                    bitmap = completionBmp.asImageBitmap(),
                                    contentDescription = "Completion Proof",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xCC0F172A)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("🔍", fontSize = 11.sp)
                                        Text(
                                            text = if (isIndonesian) "Ketuk untuk perbesar" else "Tap to zoom",
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!report.completionNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Text(
                                text = "📝 ${report.completionNotes}",
                                fontSize = 12.5.sp,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Completion Dialog
        if (showCompletionDialog) {
            AlertDialog(
                onDismissRequest = { showCompletionDialog = false },
                containerColor = cardBg,
                title = {
                    Text(
                        text = if (isIndonesian) "Selesaikan Laporan & Bukti Perbaikan" else "Resolve Report & Add Proof",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isIndonesian) "Tambahkan catatan penanganan teknisi serta foto bukti setelah selesai diperbaiki:" else "Add technician handling notes and optional completion photo:",
                            fontSize = 12.sp,
                            color = textSecondary
                        )

                        OutlinedTextField(
                            value = completionNotesInput,
                            onValueChange = { completionNotesInput = it },
                            label = { Text(if (isIndonesian) "Catatan Perbaikan" else "Resolution Notes", fontSize = 12.sp) },
                            placeholder = { Text(if (isIndonesian) "Contoh: Komponen rusak telah diganti dan dites normal." else "E.g. Repaired and tested working.", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            )
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    imagePickerLauncher.launch("image/*")
                                },
                            color = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, borderCol)
                        ) {
                            val compImg = completionImageBase64
                            if (compImg != null) {
                                val bmp = remember(compImg) {
                                    ImageUtils.base64ToBitmap(compImg)
                                }
                                if (bmp != null) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Completion Proof",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .clickable {
                                                    zoomedImageBitmap = bmp.asImageBitmap()
                                                    zoomedImageTitle = if (isIndonesian) "Foto Bukti Selesai" else "Completion Proof Photo"
                                                },
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xCC0F172A)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text("🔍", fontSize = 10.sp)
                                                Text(
                                                    text = if (isIndonesian) "Perbesar" else "Zoom",
                                                    color = Color.White,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "📷", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isIndonesian) "Pilih Foto Bukti Selesai (Opsional)" else "Pick After Photo (Optional)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            currentStatus = "Completed"
                            report.id?.let {
                                onUpdateStatus(it, "Completed", completionImageBase64, completionNotesInput.trim())
                            }
                            showCompletionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(if (isIndonesian) "Simpan & Selesai" else "Mark Resolved", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCompletionDialog = false }) {
                        Text(if (isIndonesian) "Batal" else "Cancel", color = textSecondary, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Staff Assignment Quick Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isIndonesian) "Tugaskan Teknisi Spesialis:" else "Assign Specialist Technician:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                staffList.forEach { staff ->
                    val isAssigned = selectedStaffName == staff.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedStaffName = staff.name }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = staff.name,
                                fontSize = 13.sp,
                                fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAssigned) Color(0xFF2563EB) else textPrimary
                            )
                            Text(
                                text = staff.role,
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAssigned) Color(0xFF2563EB) else if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = if (isAssigned) (if (isIndonesian) "Ditugaskan ✓" else "Assigned ✓") else (if (isIndonesian) "Pilih" else "Select"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAssigned) Color.White else textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (zoomedImageBitmap != null) {
        ZoomableImageViewerDialog(
            imageBitmap = zoomedImageBitmap,
            title = zoomedImageTitle,
            subtitle = report.title,
            onDismiss = { zoomedImageBitmap = null }
        )
    }
}

// -------------------------------------------------------------
// 6. TAB 4: ASSIGN STAFF
// -------------------------------------------------------------
@Composable
private fun AdminAssignStaffView(
    staffList: List<StaffMember>,
    reportsList: List<ReportData>,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onAssignStaff: (staffId: String, reportId: String) -> Unit,
    onAddStaff: (StaffMember) -> Unit = {},
    onDeleteStaff: (staffId: String) -> Unit = {},
    onToggleStaffAvailability: (staffId: String) -> Unit = {}
) {
    val context = LocalContext.current
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<StaffMember?>(null) }

    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }

    val roleSuggestions = listOf(
        "Teknisi Kelistrikan & Lampu",
        "Teknisi AC & Pendingin",
        "Staff Sarpras & Perabot Sipil",
        "Teknisi IT & Jaringan",
        "Koordinator Fasilitas & Sanitasi"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header with Add Staff button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isIndonesian) "Manajemen Staf & Teknisi" else "Facility Staff Management",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isIndonesian)
                        "${staffList.size} staf teknisi terdaftar di sekolah"
                    else
                        "${staffList.size} school technicians registered",
                    fontSize = 11.5.sp,
                    color = textSecondary
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        newName = ""
                        newRole = ""
                        newPhone = ""
                        showAddStaffDialog = true
                    },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF2563EB)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlusVectorIcon(color = Color.White, size = 13.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIndonesian) "+ Tambah Staf" else "+ Add Staff",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (staffList.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isIndonesian) "Belum ada staf terdaftar" else "No staff members registered",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isIndonesian) "Klik tombol '+ Tambah Staf' di atas untuk menambahkan teknisi sekolah Anda." else "Tap '+ Add Staff' above to register your school technicians.",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                }
            }
        }

        staffList.forEach { staff ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = if (staff.isAvailable) Color(0xFFEFF6FF) else Color(0xFFFEF3C7),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StaffWrenchIcon(
                                color = if (staff.isAvailable) Color(0xFF2563EB) else Color(0xFFD97706),
                                size = 20.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = staff.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = staff.role,
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onToggleStaffAvailability(staff.id) },
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (staff.isAvailable) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = if (staff.isAvailable) (if (isIndonesian) "Siap Bertugas" else "Available") else (if (isIndonesian) "Sedang Bertugas" else "Busy"),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (staff.isAvailable) Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${staff.activeTasks} " + if (isIndonesian) "tugas" else "tasks",
                                    fontSize = 10.5.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    // Contact / WhatsApp & Delete actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val cleanPhone = staff.phone.replace("-", "").replace(" ", "").replace("+", "")
                                    val waNumber = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNumber"))
                                    runCatching { context.startActivity(intent) }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2563EB)
                        ) {
                            Text(
                                text = "WhatsApp",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Delete button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { staffToDelete = staff },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Box(
                                modifier = Modifier.padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TrashVectorIcon(color = Color(0xFFEF4444), size = 15.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Tambah Staf Baru
    if (showAddStaffDialog) {
        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = {
                Text(
                    text = if (isIndonesian) "Tambah Staf / Teknisi Baru" else "Add New Staff Member",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = if (isIndonesian) "Nama Lengkap Staf:" else "Staff Full Name:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Contoh: Pak Joko Widodo", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isIndonesian) "Peran / Bidang Tanggung Jawab:" else "Role / Responsibility:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newRole,
                        onValueChange = { newRole = it },
                        placeholder = { Text("Contoh: Teknisi Kelistrikan & Lampu", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isIndonesian) "Pilihan Cepat Bidang:" else "Quick Suggestions:",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roleSuggestions.forEach { role ->
                            val isSel = newRole == role
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { newRole = role },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Color(0xFF2563EB) else Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = role.substringBefore(" &"),
                                    fontSize = 10.5.sp,
                                    color = if (isSel) Color.White else Color(0xFF2563EB),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isIndonesian) "Nomor WhatsApp / Telepon:" else "WhatsApp / Phone Number:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        placeholder = { Text("0812-3456-7890", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val newStaffId = "STF-${System.currentTimeMillis() % 1000}"
                            val created = StaffMember(
                                id = newStaffId,
                                name = newName.trim(),
                                role = if (newRole.isBlank()) "Teknisi Fasilitas Umum" else newRole.trim(),
                                phone = if (newPhone.isBlank()) "0812-0000-0000" else newPhone.trim(),
                                activeTasks = 0,
                                isAvailable = true
                            )
                            onAddStaff(created)
                            showAddStaffDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text(if (isIndonesian) "Simpan Staf" else "Save Staff", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) {
                    Text(if (isIndonesian) "Batal" else "Cancel", color = textSecondary)
                }
            }
        )
    }

    // Dialog: Konfirmasi Hapus Staf
    if (staffToDelete != null) {
        val target = staffToDelete!!
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = {
                Text(
                    text = if (isIndonesian) "Hapus Staf?" else "Remove Staff?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isIndonesian)
                        "Apakah Anda yakin ingin menghapus '${target.name}' (${target.role}) dari daftar teknisi sekolah?"
                    else
                        "Are you sure you want to remove '${target.name}' from the staff list?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStaff(target.id)
                        staffToDelete = null
                    }
                ) {
                    Text(if (isIndonesian) "Hapus" else "Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) {
                    Text(if (isIndonesian) "Batal" else "Cancel", color = textSecondary)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 7. TAB 5: USERS PELAPOR (STUDENT REPORTERS & ADMIN MANAGER)
// -------------------------------------------------------------
@Composable
private fun AdminUsersPelaporView(
    students: List<StudentReporter>,
    isSuperAdmin: Boolean,
    adminEmails: Set<String>,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onToggleAdmin: (email: String, shouldBeAdmin: Boolean) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredStudents = remember(students, query) {
        if (query.isBlank()) students
        else students.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.className.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isIndonesian) "Direktori Pengguna & Hak Akses Admin" else "Users Directory & Admin Permissions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isIndonesian)
                "Daftar akun terdaftar. Admin Utama (rompisjosh@gmail.com) memiliki wewenang mengangkat atau mencabut hak admin."
            else
                "List of registered users. Super Admin (rompisjosh@gmail.com) has authority to grant or revoke admin privileges.",
            fontSize = 12.sp,
            color = textSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Super Admin Authority Notice Banner
        if (isSuperAdmin) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF2563EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AdminShieldCrownIcon(color = Color.White, size = 16.dp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isIndonesian) "Wewenang Admin Utama Aktif" else "Super Admin Authority Active",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = if (isIndonesian)
                                "Anda dapat menekan tombol '+ Jadikan Admin' atau 'Cabut Admin' pada tiap pengguna."
                            else
                                "You can tap '+ Grant Admin' or 'Revoke Admin' for any user.",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchVectorIcon(color = textSecondary, size = 18.dp)
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(fontSize = 13.5.sp, color = textPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = if (isIndonesian) "Cari pelapor atau email..." else "Search reporter or email...",
                                fontSize = 13.5.sp,
                                color = textSecondary
                            )
                        }
                        inner()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredStudents, key = { it.email.trim().lowercase() }) { student ->
                val cleanEmail = student.email.trim().lowercase()
                val isUserSuperAdmin = cleanEmail == SessionPreferences.SUPER_ADMIN_EMAIL
                val isUserAdmin = isUserSuperAdmin || adminEmails.contains(cleanEmail)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, if (isUserAdmin) Color(0xFF2563EB).copy(alpha = 0.4f) else borderCol)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            if (isUserAdmin) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = student.name.take(2).uppercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUserAdmin) Color(0xFF2563EB) else textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = student.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when {
                                                isUserSuperAdmin -> Color(0xFF2563EB)
                                                isUserAdmin -> Color(0xFF3B82F6)
                                                else -> Color(0xFF64748B)
                                            }
                                        ) {
                                            Text(
                                                text = when {
                                                    isUserSuperAdmin -> if (isIndonesian) "ADMIN UTAMA" else "SUPER ADMIN"
                                                    isUserAdmin -> "ADMIN"
                                                    else -> if (isIndonesian) "PELAPOR" else "REPORTER"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${student.className} • ${student.email}",
                                        fontSize = 11.5.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "${student.totalReports} " + if (isIndonesian) "Laporan" else "Reports",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Super Admin Promotion / Demotion Action Buttons
                        if (isSuperAdmin && !isUserSuperAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = borderCol)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (isUserAdmin) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onToggleAdmin(student.email, false) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Text(
                                            text = if (isIndonesian) "Cabut Hak Admin" else "Revoke Admin",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onToggleAdmin(student.email, true) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2563EB)
                                    ) {
                                        Text(
                                            text = if (isIndonesian) "+ Jadikan Admin" else "+ Grant Admin",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. TAB 6: CATEGORIES & LOCATIONS
// -------------------------------------------------------------
@Composable
private fun AdminCategoriesLocationsView(
    locations: List<FacilityLocation>,
    reportsList: List<ReportData>,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onAddRoomToFloor: (floorName: String, roomName: String) -> Unit = { _, _ -> },
    onDeleteRoomFromFloor: (floorName: String, roomName: String) -> Unit = { _, _ -> }
) {
    var roomSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFloorForAdd by remember { mutableStateOf<String?>(null) }
    var newRoomNameInput by remember { mutableStateOf("") }
    var roomToDeletePair by remember { mutableStateOf<Pair<String, String>?>(null) }

    val categories = SchoolFacilityMasterData.defaultCategories

    val totalRoomsCount = remember(locations) { locations.sumOf { it.rooms.size } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isIndonesian) "Master Data Kategori & 4 Lantai Gedung" else "Facility Categories & 4 Building Floors",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isIndonesian)
                "Denah resmi gedung sekolah ($totalRoomsCount ruangan terdaftar) dan 8 kategori sarana prasarana."
            else
                "Official school building floor plan ($totalRoomsCount rooms registered) and facility categories.",
            fontSize = 12.sp,
            color = textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar for Rooms Across All Floors
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchVectorIcon(color = textSecondary, size = 18.dp)
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = roomSearchQuery,
                    onValueChange = { roomSearchQuery = it },
                    textStyle = TextStyle(fontSize = 13.5.sp, color = textPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (roomSearchQuery.isEmpty()) {
                            Text(
                                text = if (isIndonesian) "Cari ruangan di seluruh lantai... (contoh: XII RPL, Lab, Kantin)" else "Search rooms across floors...",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                        inner()
                    }
                )
                if (roomSearchQuery.isNotEmpty()) {
                    Text(
                        text = "✕",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { roomSearchQuery = "" }
                            .padding(4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Categories Overview
        Text(
            text = if (isIndonesian) "8 Kategori Kerusakan Fasilitas" else "8 Facility Damage Categories",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        val categoryColors = listOf(
            Color(0xFF3B82F6), // Electronics
            Color(0xFF8B5CF6), // Furniture
            Color(0xFF06B6D4), // Plumbing
            Color(0xFFF97316), // Building Facility
            Color(0xFF0EA5E9), // AC & Air System
            Color(0xFFEAB308), // Lighting
            Color(0xFF10B981), // Sanitary
            Color(0xFF64748B)  // Lainnya
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEachIndexed { index, (catName, catDesc) ->
                val catColor = categoryColors[index % categoryColors.size]
                val count = reportsList.count { it.category.contains(catName.substringBefore(" "), ignoreCase = true) }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(catColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = catName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$count " + if (isIndonesian) "laporan" else "reports",
                            fontSize = 10.5.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Floor Breakdowns (4 Real Floors)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isIndonesian) "Denah Gedung Sekolah (4 Lantai)" else "School Building Layout (4 Floors)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = if (isIndonesian) "Sesuai Penempatan Asli" else "Official School Layout",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2563EB)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        locations.forEach { loc ->
            val matchingRooms = if (roomSearchQuery.isBlank()) {
                loc.rooms
            } else {
                loc.rooms.filter { it.contains(roomSearchQuery, ignoreCase = true) }
            }

            // Only display floor card if query is blank or has matching rooms
            if (roomSearchQuery.isBlank() || matchingRooms.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LocationBuildingIcon(color = Color(0xFF2563EB), size = 20.dp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = loc.floorName,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = loc.floorDesc,
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "${loc.rooms.size} " + if (isIndonesian) "Ruangan" else "Rooms",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Button: Tambah Ruangan ke Lantai Ini
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            newRoomNameInput = ""
                                            selectedFloorForAdd = loc.floorName
                                        },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlusVectorIcon(color = Color(0xFF2563EB), size = 11.dp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isIndonesian) "Ruang" else "Add",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (matchingRooms.size == loc.rooms.size)
                                (if (isIndonesian) "Daftar Penempatan Ruangan (${matchingRooms.size}):" else "Registered Rooms (${matchingRooms.size}):")
                            else
                                (if (isIndonesian) "Hasil Pencarian Ruangan (${matchingRooms.size} dari ${loc.rooms.size}):" else "Search Results (${matchingRooms.size} of ${loc.rooms.size}):"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Scrollable room chips with tap to delete/inspect
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            matchingRooms.forEach { room ->
                                val isHighlighted = roomSearchQuery.isNotBlank() && room.contains(roomSearchQuery, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            roomToDeletePair = Pair(loc.floorName, room)
                                        },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isHighlighted) Color(0xFFDBEAFE) else (if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)),
                                    border = if (isHighlighted) BorderStroke(1.dp, Color(0xFF2563EB)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = room,
                                            fontSize = 11.sp,
                                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isHighlighted) Color(0xFF1D4ED8) else textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Tambah Ruangan ke Lantai
    if (selectedFloorForAdd != null) {
        val targetFloor = selectedFloorForAdd!!
        AlertDialog(
            onDismissRequest = { selectedFloorForAdd = null },
            title = {
                Text(
                    text = if (isIndonesian) "Tambah Ruangan ke $targetFloor" else "Add Room to $targetFloor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isIndonesian) "Masukkan nama ruangan baru yang ada di $targetFloor:" else "Enter the new room name on $targetFloor:",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newRoomNameInput,
                        onValueChange = { newRoomNameInput = it },
                        placeholder = { Text("Contoh: Lab Jaringan 2, Ruang Konseling", fontSize = 12.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newRoomNameInput.isNotBlank()) {
                            onAddRoomToFloor(targetFloor, newRoomNameInput.trim())
                            selectedFloorForAdd = null
                        }
                    },
                    enabled = newRoomNameInput.isNotBlank()
                ) {
                    Text(if (isIndonesian) "Tambah Ruangan" else "Add Room", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFloorForAdd = null }) {
                    Text(if (isIndonesian) "Batal" else "Cancel", color = textSecondary)
                }
            }
        )
    }

    // Dialog: Konfirmasi Hapus Ruangan
    if (roomToDeletePair != null) {
        val (floorName, roomName) = roomToDeletePair!!
        AlertDialog(
            onDismissRequest = { roomToDeletePair = null },
            title = {
                Text(
                    text = if (isIndonesian) "Hapus Ruangan?" else "Remove Room?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isIndonesian)
                        "Apakah Anda ingin menghapus ruangan '$roomName' dari daftar $floorName?"
                    else
                        "Do you want to remove room '$roomName' from $floorName?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRoomFromFloor(floorName, roomName)
                        roomToDeletePair = null
                    }
                ) {
                    Text(if (isIndonesian) "Hapus" else "Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDeletePair = null }) {
                    Text(if (isIndonesian) "Batal" else "Cancel", color = textSecondary)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 9. TAB 7: ADMIN NOTIFICATIONS
// -------------------------------------------------------------
@Composable
private fun AdminNotifView(
    notifications: List<AdminNotifItem>,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onMarkAllRead: () -> Unit,
    onNotificationClick: (AdminNotifItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isIndonesian) "Pusat Peringatan & Notifikasi Admin" else "Admin Alert & Notification Center",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = if (isIndonesian) "Pemberitahuan darurat dan status pengerjaan fasilitas." else "Urgent reports and repair status alerts.",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMarkAllRead() },
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF)
            ) {
                Text(
                    text = if (isIndonesian) "Tandai Dibaca" else "Mark All Read",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notifications.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AdminBellIcon(color = textSecondary, size = 32.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isIndonesian) "Belum ada notifikasi laporan baru" else "No report notifications yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isIndonesian) "Laporan yang dikirim oleh siswa akan otomatis muncul di sini." else "Incoming reports from students will automatically appear here.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNotificationClick(notif) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (!notif.isRead) (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF)) else cardBg,
                        border = BorderStroke(
                            width = if (!notif.isRead) 1.5.dp else 1.dp,
                            color = if (!notif.isRead) (if (notif.isUrgent) Color(0xFFEF4444) else Color(0xFF2563EB)) else borderCol
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = if (notif.isUrgent) Color(0xFFFEE2E2) else Color(0xFFEFF6FF),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AdminBellIcon(
                                    color = if (notif.isUrgent) Color(0xFFEF4444) else Color(0xFF2563EB),
                                    size = 20.dp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = notif.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!notif.isRead) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (notif.isUrgent) Color(0xFFEF4444) else Color(0xFF2563EB)
                                            ) {
                                                Text(
                                                    text = if (isIndonesian) "BARU" else "NEW",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = notif.timeAgo,
                                            fontSize = 11.sp,
                                            color = textSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = notif.desc,
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    lineHeight = 16.sp
                                )

                                if (notif.reportId != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isIndonesian) "🔍 Ketuk untuk buka detail laporan →" else "🔍 Tap to open report details →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPOSABLES & CARDS
// -------------------------------------------------------------
@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    cardColor: Color,
    accentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun AdminActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun AdminCompactReportCard(
    report: ReportData,
    isIndonesian: Boolean = true,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${report.location} • " + (if (isIndonesian) "Oleh " else "By ") + (report.userName ?: if (isIndonesian) "Pelapor" else "Reporter"),
                    fontSize = 11.5.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            AdminStatusBadge(status = report.status, isIndonesian = isIndonesian)
        }
    }
}

@Composable
private fun AdminReportManageCard(
    report: ReportData,
    isIndonesian: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onUpdateStatus: (String) -> Unit,
    onOpenResolveDialog: () -> Unit,
    onOpenDetail: () -> Unit,
    onPreviewPhoto: ((androidx.compose.ui.graphics.ImageBitmap, String) -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = report.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AdminPriorityBadge(priority = report.priority, isIndonesian = isIndonesian)
                    AdminStatusBadge(status = report.status, isIndonesian = isIndonesian)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${report.location} • " + (if (isIndonesian) "Oleh " else "By ") + (report.userName ?: if (isIndonesian) "Pelapor" else "Reporter"),
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (report.upvoteCount > 0) {
                    Text(
                        text = "👍 +${report.upvoteCount}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = report.description,
                fontSize = 12.5.sp,
                color = textPrimary,
                maxLines = 2,
                lineHeight = 16.sp
            )

            // Attached proof notice if resolved
            if (report.status.equals("Completed", ignoreCase = true) && (!report.completionNotes.isNullOrBlank() || !report.completionImageUrl.isNullOrBlank())) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                ) {
                    Text(
                        text = "✨ " + (if (isIndonesian) "Bukti: " else "Proof: ") + (report.completionNotes ?: if (isIndonesian) "Foto terlampir" else "Photo attached"),
                        fontSize = 11.sp,
                        color = Color(0xFF166534),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Attached Photos Preview Row (Before & After clickable thumbnails)
            if (!report.imageUrl.isNullOrBlank() || !report.completionImageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!report.imageUrl.isNullOrBlank()) {
                        val beforeBmp = remember(report.imageUrl) {
                            ImageUtils.base64ToBitmap(report.imageUrl)
                        }
                        if (beforeBmp != null) {
                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onPreviewPhoto?.invoke(
                                            beforeBmp.asImageBitmap(),
                                            if (isIndonesian) "Foto Bukti (Sebelum)" else "Initial Evidence (Before)"
                                        )
                                    },
                                color = Color(0xFFE2E8F0)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = beforeBmp.asImageBitmap(),
                                        contentDescription = "Before Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xCC000000)
                                    ) {
                                        Text("🔍", fontSize = 8.sp, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (!report.completionImageUrl.isNullOrBlank()) {
                        val afterBmp = remember(report.completionImageUrl) {
                            ImageUtils.base64ToBitmap(report.completionImageUrl)
                        }
                        if (afterBmp != null) {
                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onPreviewPhoto?.invoke(
                                            afterBmp.asImageBitmap(),
                                            if (isIndonesian) "Foto Bukti Selesai (After)" else "Completion Proof (After)"
                                        )
                                    },
                                color = Color(0xFFDCFCE7)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = afterBmp.asImageBitmap(),
                                        contentDescription = "After Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xCC000000)
                                    ) {
                                        Text("🔍", fontSize = 8.sp, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Clean Two-Action Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isCompleted = report.status.equals("Completed", ignoreCase = true)
                val isInProgress = report.status.equals("In Progress", ignoreCase = true)

                when {
                    isCompleted -> {
                        StatusActionButton(
                            label = if (isIndonesian) "↺ Buka Kembali" else "↺ Reopen",
                            color = Color(0xFFD97706),
                            onClick = { onUpdateStatus("In Progress") }
                        )
                    }
                    isInProgress -> {
                        StatusActionButton(
                            label = if (isIndonesian) "✅ Selesaikan (Bukti)" else "✅ Resolve (Proof)",
                            color = Color(0xFF16A34A),
                            onClick = { onOpenResolveDialog() }
                        )
                    }
                    else -> {
                        StatusActionButton(
                            label = if (isIndonesian) "⚙️ Mulai Proses" else "⚙️ Start Repair",
                            color = Color(0xFF2563EB),
                            onClick = { onUpdateStatus("In Progress") }
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenDetail() },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkMode) Color(0xFF334155) else Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = if (isIndonesian) "Kelola Detail →" else "Manage Detail →",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AdminPriorityBadge(priority: String, isIndonesian: Boolean = true) {
    val (bg, textColor) = when (priority.lowercase()) {
        "darurat", "emergency", "high", "tinggi" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        "rendah", "low" -> Color(0xFFD1FAE5) to Color(0xFF15803D)
        else -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    }

    val label = when (priority.lowercase()) {
        "darurat", "emergency" -> if (isIndonesian) "DARURAT" else "EMERGENCY"
        "rendah", "low" -> if (isIndonesian) "RENDAH" else "LOW"
        "tinggi", "high" -> if (isIndonesian) "TINGGI" else "HIGH"
        else -> if (isIndonesian) "SEDANG" else "MEDIUM"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AdminStatusBadge(status: String, isIndonesian: Boolean = true) {
    val (bg, textColor, label) = when {
        status.equals("Pending", ignoreCase = true) || status.equals("Menunggu", ignoreCase = true) ->
            Triple(Color(0xFFFEF3C7), Color(0xFFB45309), if (isIndonesian) "MENUNGGU" else "PENDING")
        status.equals("In Progress", ignoreCase = true) || status.equals("Diproses", ignoreCase = true) ->
            Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), if (isIndonesian) "DIPROSES" else "IN PROGRESS")
        else ->
            Triple(Color(0xFFDCFCE7), Color(0xFF15803D), if (isIndonesian) "SELESAI" else "RESOLVED")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CategoryPill(category: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F5F9)
    ) {
        Text(
            text = category,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun LocationPill(location: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFEFF6FF)
    ) {
        Text(
            text = "📍 $location",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2563EB),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.5.sp, color = textSecondary)
        Text(text = value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
    }
}

// -------------------------------------------------------------
// CANVAS DRAWN ICONS MATCHING TRAC DESIGN SYSTEM
// -------------------------------------------------------------
@Composable
private fun AdminTabIcon(tab: AdminSubTab, color: Color, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        when (tab) {
            AdminSubTab.DASHBOARD -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.1f, h * 0.1f),
                    size = Size(w * 0.35f, h * 0.35f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.55f, h * 0.1f),
                    size = Size(w * 0.35f, h * 0.35f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.1f, h * 0.55f),
                    size = Size(w * 0.35f, h * 0.35f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.55f, h * 0.55f),
                    size = Size(w * 0.35f, h * 0.35f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
            AdminSubTab.REPORTS -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.7f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.35f), end = Offset(w * 0.65f, h * 0.35f), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.5f), end = Offset(w * 0.65f, h * 0.5f), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.65f), end = Offset(w * 0.55f, h * 0.65f), strokeWidth = 1.5.dp.toPx())
            }
            AdminSubTab.REPORT_DETAIL -> {
                drawCircle(color = color, radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.5.dp.toPx()))
                drawLine(color = color, start = Offset(w * 0.5f, h * 0.3f), end = Offset(w * 0.5f, h * 0.55f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(color = color, radius = 1.2.dp.toPx(), center = Offset(w * 0.5f, h * 0.7f))
            }
            AdminSubTab.ASSIGN_STAFF -> {
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.75f)
                    lineTo(w * 0.65f, h * 0.35f)
                }
                drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(w * 0.75f, h * 0.25f), style = Stroke(width = 1.5.dp.toPx()))
            }
            AdminSubTab.USERS_PELAPOR -> {
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(w * 0.35f, h * 0.35f), style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(w * 0.65f, h * 0.35f), style = Stroke(width = 1.5.dp.toPx()))
                drawLine(color = color, start = Offset(w * 0.2f, h * 0.75f), end = Offset(w * 0.5f, h * 0.75f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.5f, h * 0.75f), end = Offset(w * 0.8f, h * 0.75f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }
            AdminSubTab.CATEGORIES_LOCATIONS -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.25f, h * 0.25f),
                    size = Size(w * 0.5f, h * 0.65f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawLine(color = color, start = Offset(w * 0.4f, h * 0.4f), end = Offset(w * 0.4f, h * 0.5f), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(w * 0.6f, h * 0.4f), end = Offset(w * 0.6f, h * 0.5f), strokeWidth = 1.5.dp.toPx())
            }
            AdminSubTab.ADMIN_NOTIF -> {
                drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.2f))
                val bell = Path().apply {
                    moveTo(w * 0.3f, h * 0.7f)
                    lineTo(w * 0.7f, h * 0.7f)
                    lineTo(w * 0.65f, h * 0.35f)
                    lineTo(w * 0.35f, h * 0.35f)
                    close()
                }
                drawPath(bell, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color = color, start = Offset(w * 0.45f, h * 0.82f), end = Offset(w * 0.55f, h * 0.82f), strokeWidth = 1.5.dp.toPx())
            }
        }
    }
}

@Composable
private fun AdminShieldCrownIcon(color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.10f)
            lineTo(w * 0.88f, h * 0.26f)
            lineTo(w * 0.88f, h * 0.58f)
            cubicTo(w * 0.88f, h * 0.82f, w * 0.5f, h * 0.94f, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.5f, h * 0.94f, w * 0.12f, h * 0.82f, w * 0.12f, h * 0.58f)
            lineTo(w * 0.12f, h * 0.26f)
            close()
        }
        drawPath(shieldPath, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.52f))
    }
}

@Composable
private fun StaffWrenchIcon(color: Color, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.8f)
            lineTo(w * 0.65f, h * 0.35f)
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(w * 0.72f, h * 0.28f), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun LocationBuildingIcon(color: Color, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.25f, h * 0.2f),
            size = Size(w * 0.5f, h * 0.7f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawLine(color = color, start = Offset(w * 0.4f, h * 0.38f), end = Offset(w * 0.4f, h * 0.48f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.6f, h * 0.38f), end = Offset(w * 0.6f, h * 0.48f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.4f, h * 0.6f), end = Offset(w * 0.4f, h * 0.7f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.6f, h * 0.6f), end = Offset(w * 0.6f, h * 0.7f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun AdminBellIcon(color: Color, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.18f))
        val bell = Path().apply {
            moveTo(w * 0.28f, h * 0.72f)
            lineTo(w * 0.72f, h * 0.72f)
            lineTo(w * 0.65f, h * 0.34f)
            lineTo(w * 0.35f, h * 0.34f)
            close()
        }
        drawPath(bell, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = color, start = Offset(w * 0.44f, h * 0.84f), end = Offset(w * 0.56f, h * 0.84f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun SearchVectorIcon(color: Color, size: androidx.compose.ui.unit.Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(w * 0.42f, h * 0.42f), style = Stroke(width = 1.8.dp.toPx()))
        drawLine(color = color, start = Offset(w * 0.65f, h * 0.65f), end = Offset(w * 0.88f, h * 0.88f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun PlusVectorIcon(color: Color, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.85f, h * 0.5f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.15f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun TrashVectorIcon(color: Color, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Top lid bar
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.25f),
            end = Offset(w * 0.8f, h * 0.25f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Top handle
        drawLine(
            color = color,
            start = Offset(w * 0.4f, h * 0.15f),
            end = Offset(w * 0.6f, h * 0.15f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Bin body
        val binPath = Path().apply {
            moveTo(w * 0.28f, h * 0.25f)
            lineTo(w * 0.32f, h * 0.85f)
            cubicTo(w * 0.33f, h * 0.9f, w * 0.38f, h * 0.92f, w * 0.45f, h * 0.92f)
            lineTo(w * 0.55f, h * 0.92f)
            cubicTo(w * 0.62f, h * 0.92f, w * 0.67f, h * 0.9f, w * 0.68f, h * 0.85f)
            lineTo(w * 0.72f, h * 0.25f)
        }
        drawPath(binPath, color = color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Inner vertical slats
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.38f),
            end = Offset(w * 0.42f, h * 0.76f),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.38f),
            end = Offset(w * 0.58f, h * 0.76f),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

