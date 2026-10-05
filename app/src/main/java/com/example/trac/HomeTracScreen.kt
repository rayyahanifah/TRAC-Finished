package com.example.trac

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import com.example.trac.components.ZoomableImageViewerDialog
import com.example.trac.data.ReportData
import com.example.trac.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeTracScreen(
    userName: String = "User",
    userProfileImage: String = "",
    isIndonesian: Boolean = false,
    isDarkMode: Boolean = false,
    reportsList: List<ReportData> = emptyList(),
    unreadNotificationCount: Int = 0,
    onRefreshReports: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onViewAllReportsClick: () -> Unit = {},
    onReportsTabClick: () -> Unit = {},
    onReportItemClick: (ReportData) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    var showZoomedAvatar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefreshReports()
    }

    // Dynamic Theme Colors
    val pageBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

    // Calculate real dynamic metric counts from live Supabase reports
    val totalReportsCount = reportsList.size
    val inProgressCount = reportsList.count { it.status.equals("In Progress", ignoreCase = true) }
    val completedCount = reportsList.count { it.status.equals("Completed", ignoreCase = true) }

    // Active reports for Home (Pending & In Progress only, sorted newest to oldest)
    val activeReports = remember(reportsList) {
        reportsList
            .filterNot { it.status.equals("Completed", ignoreCase = true) }
            .sortedWith(
                compareByDescending<ReportData> { it.createdAt ?: "" }
                    .thenByDescending { it.id ?: "" }
            )
    }

    // Staggered entrance animation states
    val headerAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val headerOffsetY = remember { Animatable(if (isPreview) 0f else -35f) }

    val metricsAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val metricsOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }

    val bannerAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val bannerOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }

    val reportsAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val reportsOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }

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
                metricsAlpha.animateTo(1f, animationSpec = tween(450))
            }
            launch {
                metricsOffsetY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            delay(120)
            launch {
                bannerAlpha.animateTo(1f, animationSpec = tween(400))
            }
            launch {
                bannerOffsetY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            delay(100)
            launch {
                reportsAlpha.animateTo(1f, animationSpec = tween(400))
            }
            launch {
                reportsOffsetY.animateTo(
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
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Header Bar: Avatar, User Greeting, Bell Icon
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
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val profileBitmap = remember(userProfileImage) {
                        if (userProfileImage.isNotBlank()) {
                            ImageUtils.base64ToBitmap(userProfileImage)
                        } else null
                    }

                    Surface(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0x202563EB))
                            .clip(CircleShape)
                            .clickable(enabled = profileBitmap != null) {
                                showZoomedAvatar = true
                            },
                        color = Color(0xFFE0ECFF)
                    ) {
                        if (profileBitmap != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = profileBitmap.asImageBitmap(),
                                    contentDescription = "User Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0xAA000000)
                                ) {
                                    Text("🔍", fontSize = 7.sp, modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp))
                                }
                            }
                        } else {
                            val initials = if (userName.isNotBlank()) {
                                userName.take(2).uppercase()
                            } else "TR"

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isIndonesian) "Halo, $userName!" else "Hello, $userName!",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isIndonesian) "Terima kasih telah peduli lingkungan sekolah." else "Thank you for caring about the school environment.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bell Icon with unread badge indicator
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onNotificationClick() }
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            spotColor = Color(0x10000000)
                        ),
                    shape = CircleShape,
                    color = cardBg
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BellIcon(color = textPrimary)
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2. Metrics Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = metricsAlpha.value
                        translationY = metricsOffsetY.value.dp.toPx()
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    count = totalReportsCount.toString(),
                    label = if (isIndonesian) "Total Laporan" else "Total Reports",
                    numberColor = Color(0xFF2563EB),
                    cardBg = cardBg,
                    borderCol = borderCol,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    count = inProgressCount.toString(),
                    label = if (isIndonesian) "Proses" else "In Progress",
                    numberColor = Color(0xFFD97706),
                    cardBg = cardBg,
                    borderCol = borderCol,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    count = completedCount.toString(),
                    label = if (isIndonesian) "Selesai" else "Completed",
                    numberColor = Color(0xFF059669),
                    cardBg = cardBg,
                    borderCol = borderCol,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Primary Banner: "+ Create New Report"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateReportClick() }
                    .graphicsLayer {
                        alpha = bannerAlpha.value
                        translationY = bannerOffsetY.value.dp.toPx()
                    }
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color(0x352563EB),
                        ambientColor = Color(0x152563EB)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2563EB)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isIndonesian) "+ Buat Laporan Baru" else "+ Create New Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isIndonesian) "Ketuk untuk mengirim laporan kerusakan fasilitas" else "Tap to submit a new issue",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PlusIcon(color = Color.White, size = 20.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 4. Section Header: "Recent Reports" + "View All"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = reportsAlpha.value
                        translationY = reportsOffsetY.value.dp.toPx()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isIndonesian) "Laporan Terbaru" else "Recent Reports",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                Text(
                    text = if (isIndonesian) "Lihat Semua" else "View All",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.clickable { onViewAllReportsClick() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Recent Live Reports List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = reportsAlpha.value
                        translationY = reportsOffsetY.value.dp.toPx()
                    }
            ) {
                if (reportsList.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = Color(0x0D000000)
                            ),
                        shape = RoundedCornerShape(20.dp),
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
                                text = if (isIndonesian) "Belum ada laporan. Klik + Buat Laporan Baru!" else "No reports yet. Click + Create New Report!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        }
                    }
                } else if (activeReports.isEmpty()) {
                    // All reports are already completed
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = Color(0x0D000000)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFFDCFCE7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CheckmarkIcon(color = Color(0xFF15803D), size = 22.dp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isIndonesian) "Semua Laporan Telah Selesai!" else "All Reports Completed!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isIndonesian) "Tidak ada laporan aktif yang belum selesai saat ini." else "There are no pending or in-progress reports right now.",
                                fontSize = 12.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    activeReports.take(5).forEach { report ->
                        ReportCardItem(
                            report = report,
                            isIndonesian = isIndonesian,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onClick = { onReportItemClick(report) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(84.dp))
        }

        if (showZoomedAvatar && userProfileImage.isNotBlank()) {
            ZoomableImageViewerDialog(
                base64Image = userProfileImage,
                title = if (isIndonesian) "Foto Profil" else "Profile Photo",
                subtitle = userName.ifBlank { null },
                onDismiss = { showZoomedAvatar = false }
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    count: String,
    label: String,
    numberColor: Color,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x0D000000),
                ambientColor = Color(0x05000000)
            ),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = numberColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary
            )
        }
    }
}

@Composable
private fun ReportCardItem(
    report: ReportData,
    isIndonesian: Boolean,
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
                spotColor = Color(0x0D000000),
                ambientColor = Color(0x05000000)
            ),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFEFF6FF), shape = RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIconSmall()
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = report.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (report.priority.equals("Darurat", ignoreCase = true) || report.priority.equals("High", ignoreCase = true)) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (isIndonesian) "DARURAT" else "EMERGENCY",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        } else if (report.priority.equals("Rendah", ignoreCase = true) || report.priority.equals("Low", ignoreCase = true)) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (isIndonesian) "RENDAH" else "LOW",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${report.location} • ${report.createdAt?.take(10) ?: ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (report.upvoteCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "👍 ${report.upvoteCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge Pill
            val statusDisplay = when {
                report.status.equals("In Progress", ignoreCase = true) -> if (isIndonesian) "Proses" else "In Progress"
                report.status.equals("Completed", ignoreCase = true) -> if (isIndonesian) "Selesai" else "Completed"
                else -> if (isIndonesian) "Menunggu" else "Pending"
            }

            val badgeBg = when {
                report.status.equals("Completed", ignoreCase = true) -> Color(0xFFD1FAE5)
                report.status.equals("In Progress", ignoreCase = true) -> Color(0xFFFEF3C7)
                else -> Color(0xFFF1F5F9)
            }

            val badgeColor = when {
                report.status.equals("Completed", ignoreCase = true) -> Color(0xFF059669)
                report.status.equals("In Progress", ignoreCase = true) -> Color(0xFFD97706)
                else -> Color(0xFF64748B)
            }

            Box(
                modifier = Modifier
                    .background(badgeBg, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusDisplay,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
private fun CategoryIconSmall() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val color = Color(0xFF2563EB)

        drawCircle(color = color, radius = w * 0.4f, style = Stroke(width = 2.dp.toPx()))
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.3f),
            end = Offset(w * 0.5f, h * 0.55f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = color, radius = 1.2.dp.toPx(), center = Offset(w * 0.5f, h * 0.7f))
    }
}

@Composable
private fun BellIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        val bellPath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            cubicTo(w * 0.3f, h * 0.12f, w * 0.2f, h * 0.3f, w * 0.2f, h * 0.58f)
            lineTo(w * 0.12f, h * 0.72f)
            lineTo(w * 0.88f, h * 0.72f)
            lineTo(w * 0.8f, h * 0.58f)
            cubicTo(w * 0.8f, h * 0.3f, w * 0.7f, h * 0.12f, w * 0.5f, h * 0.12f)
            close()
        }
        drawPath(
            path = bellPath,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun PlusIcon(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.15f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.85f, h * 0.5f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun CheckmarkIcon(color: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.78f, h * 0.28f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeTracScreenPreview() {
    HomeTracScreen()
}
