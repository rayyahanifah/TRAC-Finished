package com.example.trac

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trac.data.ReportData
import com.example.trac.data.ReportRepository
import com.example.trac.data.SessionPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NotificationItemData(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val badgeText: String? = null,
    val badgeBg: Color? = null,
    val badgeColor: Color? = null,
    val iconType: NotificationIconType,
    var isUnread: Boolean = true,
    val category: String = "Updates",
    val reportId: String? = null
)

enum class NotificationIconType {
    REPAIR,
    COMPLETED,
    COMMENT,
    ANNOUNCEMENT
}

@Composable
fun NotificationsTracScreen(
    reportsList: List<ReportData> = emptyList(),
    isIndonesian: Boolean = false,
    isDarkMode: Boolean = false,
    onNotificationItemClick: (NotificationItemData) -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val sessionPrefs = remember { SessionPreferences(context) }
    val reportRepo = remember { ReportRepository() }
    val coroutineScope = rememberCoroutineScope()
    var readNotificationIds by remember { mutableStateOf(HashSet(sessionPrefs.getReadNotificationIds())) }

    LaunchedEffect(Unit) {
        reportRepo.getReadNotificationIds().onSuccess { remoteReadIds ->
            if (remoteReadIds.isNotEmpty()) {
                sessionPrefs.markAllNotificationsAsRead(remoteReadIds)
                readNotificationIds = HashSet(sessionPrefs.getReadNotificationIds())
            }
        }
    }

    // Dynamic Theme Colors
    val pageBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    val filterOptions = if (isIndonesian) listOf("Semua", "Belum Dibaca", "Pembaruan", "Pengumuman") else listOf("All", "Unread", "Updates", "Announcements")

    // Dynamic list of notifications synced with user reports & announcements
    val notifications = remember(reportsList, readNotificationIds, isIndonesian) {
        val list = mutableListOf<NotificationItemData>()

        // 1. Dynamic notifications from live reports (In Progress or Completed)
        reportsList.filter {
            it.status.equals("In Progress", ignoreCase = true) || it.status.equals("Completed", ignoreCase = true)
        }.forEach { report ->
            val isCompleted = report.status.equals("Completed", ignoreCase = true)
            val notifId = if (isCompleted) "notif_done_${report.id ?: report.title.hashCode()}" else "notif_prog_${report.id ?: report.title.hashCode()}"
            val isUnread = !readNotificationIds.contains(notifId)

            list.add(
                NotificationItemData(
                    id = notifId,
                    title = if (isCompleted)
                        (if (isIndonesian) "Laporan selesai diperbaiki" else "Report completed")
                    else
                        (if (isIndonesian) "Laporan sedang diproses" else "Your report was updated"),
                    description = if (isCompleted)
                        (if (isIndonesian) "${report.title} di ${report.location} telah selesai diperbaiki dan ditutup." else "${report.title} in ${report.location} has been repaired and closed.")
                    else
                        (if (isIndonesian) "${report.title} di ${report.location} sedang ditangani oleh teknisi perbaikan." else "${report.title} in ${report.location} is currently being handled by technicians."),
                    timeAgo = report.createdAt?.take(10) ?: (if (isIndonesian) "Hari ini" else "Today"),
                    badgeText = if (isCompleted) (if (isIndonesian) "Selesai" else "Done") else (if (isIndonesian) "Proses" else "In Repair"),
                    badgeBg = if (isCompleted) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                    badgeColor = if (isCompleted) Color(0xFF059669) else Color(0xFFD97706),
                    iconType = if (isCompleted) NotificationIconType.COMPLETED else NotificationIconType.REPAIR,
                    isUnread = isUnread,
                    category = "Updates",
                    reportId = report.id
                )
            )
        }

        // 2. Default initial notifications if list is sparse, plus announcements
        val d1 = "notif_def_1"
        list.add(
            NotificationItemData(
                id = d1,
                title = if (isIndonesian) "Pusat Layanan Fasilitas TRAC" else "TRAC Facility Hub",
                description = if (isIndonesian) "Laporan fasilitas sekolah akan langsung diteruskan ke tim teknisi." else "Facility issue reports are directly forwarded to the technician team.",
                timeAgo = if (isIndonesian) "Hari ini" else "Today",
                badgeText = "Info",
                badgeBg = Color(0xFFEFF6FF),
                badgeColor = Color(0xFF2563EB),
                iconType = NotificationIconType.COMMENT,
                isUnread = !readNotificationIds.contains(d1),
                category = "Updates",
                reportId = null
            )
        )

        // 3. School Announcement
        val a1 = "notif_ann_1"
        list.add(
            NotificationItemData(
                id = a1,
                title = if (isIndonesian) "Pengumuman Pemeliharaan Sekolah" else "Scheduled School Maintenance",
                description = if (isIndonesian) "Jadwal pemeliharaan dan inspeksi rutin sarana prasarana sekolah setiap Sabtu 07:00-12:00." else "Scheduled school facility maintenance this Saturday, 07:00-12:00.",
                timeAgo = if (isIndonesian) "Kemarin" else "Yesterday",
                badgeText = "Info",
                badgeBg = Color(0xFFF3E8FF),
                badgeColor = Color(0xFF7E22CE),
                iconType = NotificationIconType.ANNOUNCEMENT,
                isUnread = !readNotificationIds.contains(a1),
                category = "Announcements",
                reportId = null
            )
        )

        list
    }

    // Filter Notifications Real Time
    val filteredNotifications = notifications.filter { item ->
        when (selectedFilter) {
            "Unread", "Belum Dibaca" -> item.isUnread
            "Updates", "Pembaruan" -> item.category == "Updates"
            "Announcements", "Pengumuman" -> item.category == "Announcements"
            else -> true
        }
    }

    // Entrance animation states
    val headerAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val headerOffsetY = remember { Animatable(if (isPreview) 0f else -30f) }

    val contentAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val contentOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }

    LaunchedEffect(isPreview) {
        if (!isPreview) {
            launch {
                headerAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
            }
            launch {
                headerOffsetY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            delay(100)
            launch {
                contentAlpha.animateTo(1f, animationSpec = tween(450))
            }
            launch {
                contentOffsetY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Top Header Bar: Title "Notifications" + "Mark all as read"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = headerAlpha.value
                        translationY = headerOffsetY.value.dp.toPx()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isIndonesian) "Notifikasi" else "Notifications",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                Text(
                    text = if (isIndonesian) "Tandai dibaca" else "Mark all as read",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.clickable {
                        val allIds = notifications.map { it.id }
                        sessionPrefs.markAllNotificationsAsRead(allIds)
                        readNotificationIds = HashSet(sessionPrefs.getReadNotificationIds())
                        coroutineScope.launch {
                            reportRepo.markAllNotificationsAsReadInDb(allIds)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Main Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffsetY.value.dp.toPx()
                    }
            ) {
                // 2. Filter Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = filter == selectedFilter || (selectedFilter == "All" && filter == "Semua")
                        val chipBg = if (isSelected) Color(0xFF2563EB) else cardBg
                        val chipTextColor = if (isSelected) Color.White else textSecondary

                        Surface(
                            modifier = Modifier
                                .clickable { selectedFilter = filter }
                                .shadow(
                                    elevation = if (isSelected) 6.dp else 2.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    spotColor = if (isSelected) Color(0x352563EB) else Color(0x0A000000)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = chipBg,
                            border = if (isSelected) null else BorderStroke(1.dp, borderCol)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipTextColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Notifications List Items
                if (filteredNotifications.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(18.dp),
                                spotColor = Color(0x0D000000)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isIndonesian) "Tidak ada notifikasi saat ini." else "No notifications right now.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        }
                    }
                } else {
                    filteredNotifications.forEach { item ->
                        NotificationCardItem(
                            notification = item,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onClick = {
                                sessionPrefs.markNotificationAsRead(item.id)
                                readNotificationIds = HashSet(sessionPrefs.getReadNotificationIds())
                                coroutineScope.launch {
                                    reportRepo.markNotificationAsReadInDb(item.id, item.title, item.description)
                                }
                                onNotificationItemClick(item)
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun NotificationCardItem(
    notification: NotificationItemData,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (notification.isUnread) Color(0x1F2563EB) else Color(0x0D000000),
                ambientColor = Color(0x05000000)
            ),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (notification.isUnread) Color(0xFF2563EB) else borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            val iconBg = when (notification.iconType) {
                NotificationIconType.REPAIR -> Color(0xFFFEF3C7)
                NotificationIconType.COMPLETED -> Color(0xFFD1FAE5)
                NotificationIconType.COMMENT -> Color(0xFFEFF6FF)
                NotificationIconType.ANNOUNCEMENT -> Color(0xFFF3E8FF)
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBg, shape = RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                NotificationIcon(notification.iconType)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = notification.title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (notification.isUnread) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFF2563EB), shape = CircleShape)
                            )
                        }
                    }

                    if (notification.badgeText != null && notification.badgeBg != null && notification.badgeColor != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(notification.badgeBg, shape = RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = notification.badgeText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = notification.badgeColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.description,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = notification.timeAgo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun NotificationIcon(iconType: NotificationIconType) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        when (iconType) {
            NotificationIconType.REPAIR -> {
                val iconColor = Color(0xFFD97706)
                drawCircle(color = iconColor, radius = w * 0.42f, style = Stroke(width = 2.dp.toPx()))
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.5f, h * 0.28f),
                    end = Offset(w * 0.5f, h * 0.58f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = iconColor, radius = 1.4.dp.toPx(), center = Offset(w * 0.5f, h * 0.74f))
            }
            NotificationIconType.COMPLETED -> {
                val iconColor = Color(0xFF059669)
                drawCircle(color = iconColor, radius = w * 0.42f, style = Stroke(width = 2.dp.toPx()))
                val checkPath = Path().apply {
                    moveTo(w * 0.32f, h * 0.52f)
                    lineTo(w * 0.46f, h * 0.66f)
                    lineTo(w * 0.68f, h * 0.38f)
                }
                drawPath(
                    path = checkPath,
                    color = iconColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            NotificationIconType.COMMENT -> {
                val iconColor = Color(0xFF2563EB)
                val path = Path().apply {
                    moveTo(w * 0.18f, h * 0.2f)
                    lineTo(w * 0.82f, h * 0.2f)
                    quadraticTo(w * 0.92f, h * 0.2f, w * 0.92f, h * 0.32f)
                    lineTo(w * 0.92f, h * 0.68f)
                    quadraticTo(w * 0.92f, h * 0.8f, w * 0.82f, h * 0.8f)
                    lineTo(w * 0.45f, h * 0.8f)
                    lineTo(w * 0.22f, h * 0.95f)
                    lineTo(w * 0.22f, h * 0.8f)
                    lineTo(w * 0.18f, h * 0.8f)
                    quadraticTo(w * 0.08f, h * 0.8f, w * 0.08f, h * 0.68f)
                    lineTo(w * 0.08f, h * 0.32f)
                    quadraticTo(w * 0.08f, h * 0.2f, w * 0.18f, h * 0.2f)
                    close()
                }
                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            NotificationIconType.ANNOUNCEMENT -> {
                val iconColor = Color(0xFF9333EA)
                val hornPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.4f)
                    lineTo(w * 0.45f, h * 0.28f)
                    lineTo(w * 0.45f, h * 0.72f)
                    lineTo(w * 0.2f, h * 0.6f)
                    close()
                }
                drawPath(
                    path = hornPath,
                    color = iconColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                val wavePath = Path().apply {
                    moveTo(w * 0.62f, h * 0.3f)
                    quadraticTo(w * 0.78f, h * 0.5f, w * 0.62f, h * 0.7f)
                }
                drawPath(
                    path = wavePath,
                    color = iconColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationsTracScreenPreview() {
    NotificationsTracScreen()
}
