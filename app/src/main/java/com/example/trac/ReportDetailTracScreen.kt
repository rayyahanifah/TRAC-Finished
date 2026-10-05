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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trac.components.ZoomableImageViewerDialog
import com.example.trac.data.ReportData
import com.example.trac.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CommentData(
    val id: String,
    val userName: String,
    val userRole: String,
    val commentText: String,
    val timeAgo: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailTracScreen(
    reportData: ReportData? = null,
    reportId: String = "TRC-403B",
    title: String = "AC not cooling",
    location: String = "XI RPL Classroom",
    reportDate: String = "Reported 20 Sep 2026, 08:32",
    statusText: String = "In Progress",
    descriptionText: String = "The AC unit in XI RPL Classroom is not cooling and makes a constant rattling noise. The classroom has become uncomfortable for learning activities, especially during afternoon sessions when temperatures are highest.",
    category: String = "Electronics",
    handledBy: String = "Facilities Team",
    estCompletion: String = "22 Sep 2026",
    isIndonesian: Boolean = false,
    isDarkMode: Boolean = false,
    onUpvoteClick: (reportId: String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onHomeTabClick: () -> Unit = {},
    onReportsTabClick: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current

    // Dynamic Theme Colors
    val pageBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

    val displayTitle = reportData?.title ?: title
    val displayLocation = reportData?.location ?: location
    val displayCategory = reportData?.category ?: category
    val displayDescription = reportData?.description ?: descriptionText
    val displayStatus = reportData?.status ?: statusText
    val displayDate = reportData?.createdAt?.take(10) ?: reportDate
    val displayPriority = reportData?.priority ?: "Sedang"

    var hasUpvotedLocally by remember(reportData?.id) { mutableStateOf(false) }

    var commentsList by remember(reportData?.id) {
        mutableStateOf<List<CommentData>>(emptyList())
    }

    var showCommentSheet by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    var zoomedImageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var zoomedImageTitle by remember { mutableStateOf("Pratinjau Foto") }

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

            // Header Bar
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        BackArrowIcon(color = textPrimary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${if (isIndonesian) "Laporan" else "Report"} #${reportData?.id?.take(8) ?: reportId}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                }

                // Priority & Status Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val (pColor, pBg) = when (displayPriority.lowercase()) {
                        "darurat" -> Color(0xFFEF4444) to Color(0xFFFEE2E2)
                        "rendah" -> Color(0xFF10B981) to Color(0xFFD1FAE5)
                        else -> Color(0xFFF59E0B) to Color(0xFFFEF3C7)
                    }

                    Box(
                        modifier = Modifier
                            .background(pBg, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = displayPriority.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = pColor
                        )
                    }

                    val statusDisplay = when {
                        displayStatus.equals("In Progress", ignoreCase = true) -> if (isIndonesian) "Proses" else "In Progress"
                        displayStatus.equals("Completed", ignoreCase = true) -> if (isIndonesian) "Selesai" else "Completed"
                        else -> if (isIndonesian) "Menunggu" else "Pending"
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                when (displayStatus.lowercase()) {
                                    "completed" -> Color(0xFFD1FAE5)
                                    "in progress" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFF1F5F9)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = statusDisplay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (displayStatus.lowercase()) {
                                "completed" -> Color(0xFF059669)
                                "in progress" -> Color(0xFFD97706)
                                else -> Color(0xFF64748B)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Main Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffsetY.value.dp.toPx()
                    }
            ) {
                // Photo Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0x15000000)
                        )
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color(0xFFE2E8F0)
                ) {
                    val reportImageBitmap = remember(reportData?.imageUrl) {
                        if (!reportData?.imageUrl.isNullOrBlank()) {
                            ImageUtils.base64ToBitmap(reportData.imageUrl)
                        } else null
                    }

                    if (reportImageBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    zoomedImageBitmap = reportImageBitmap.asImageBitmap()
                                    zoomedImageTitle = if (isIndonesian) "Foto Fasilitas (Laporan)" else "Facility Photo (Report)"
                                }
                        ) {
                            Image(
                                bitmap = reportImageBitmap.asImageBitmap(),
                                contentDescription = "Facility Photo",
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
                    } else {
                        ACFacilityPhotoIllustration()
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = displayTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocationPinIcon(color = textSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$displayLocation • $displayDate",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Saya Juga Mengalami" (+1 Upvote) Interactive Card
                val totalUpvotes = (reportData?.upvoteCount ?: 0) + (if (hasUpvotedLocally) 1 else 0)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0A000000))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !hasUpvotedLocally) {
                            hasUpvotedLocally = true
                            reportData?.id?.let { onUpvoteClick(it) }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasUpvotedLocally) Color(0xFFEFF6FF) else cardBg,
                    border = BorderStroke(1.dp, if (hasUpvotedLocally) Color(0xFF2563EB) else borderCol)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "👍", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isIndonesian) "Saya Juga Mengalami Ini" else "I Experience This Too",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasUpvotedLocally) Color(0xFF1D4ED8) else textPrimary
                                )
                                Text(
                                    text = if (hasUpvotedLocally) {
                                        if (isIndonesian) "Dukunganmu tercatat! Menambah urgensi perbaikan." else "Your support was recorded! Raised fix priority."
                                    } else {
                                        if (isIndonesian) "Klik jika kamu juga terganggu oleh fasilitas ini" else "Tap if you're also affected by this facility"
                                    },
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (hasUpvotedLocally) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+$totalUpvotes",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasUpvotedLocally) Color.White else textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Stepper Card
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
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = if (isIndonesian) "Status Perbaikan" else "Track Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircleCheckIcon(color = Color(0xFF10B981))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isIndonesian) "Dikirim" else "Submitted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                color = if (displayStatus.equals("In Progress", ignoreCase = true) || displayStatus.equals("Completed", ignoreCase = true)) Color(0xFF10B981) else borderCol,
                                thickness = 2.dp
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (displayStatus.equals("In Progress", ignoreCase = true) || displayStatus.equals("Completed", ignoreCase = true)) {
                                    CircleWarningIcon(color = Color(0xFFF59E0B))
                                } else {
                                    CircleOutlineIcon(color = textSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isIndonesian) "Dalam Perbaikan" else "Under Repair",
                                    fontSize = 11.sp,
                                    fontWeight = if (displayStatus.equals("In Progress", ignoreCase = true)) FontWeight.Bold else FontWeight.Medium,
                                    color = if (displayStatus.equals("In Progress", ignoreCase = true)) Color(0xFFD97706) else textSecondary
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                color = if (displayStatus.equals("Completed", ignoreCase = true)) Color(0xFF10B981) else borderCol,
                                thickness = 2.dp
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (displayStatus.equals("Completed", ignoreCase = true)) {
                                    CircleCheckIcon(color = Color(0xFF10B981))
                                } else {
                                    CircleOutlineIcon(color = textSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isIndonesian) "Selesai" else "Completed",
                                    fontSize = 11.sp,
                                    fontWeight = if (displayStatus.equals("Completed", ignoreCase = true)) FontWeight.Bold else FontWeight.Medium,
                                    color = if (displayStatus.equals("Completed", ignoreCase = true)) Color(0xFF059669) else textSecondary
                                )
                            }
                        }
                    }
                }

                // Bukti Penyelesaian (Before vs After) Card
                if (displayStatus.equals("Completed", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(16.dp))
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
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "✨", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isIndonesian) "Bukti Penyelesaian (Before & After)" else "Completion Proof (Before & After)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Before photo
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isIndonesian) "Sebelum (Laporan):" else "Before (Report):",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val beforeBitmap = remember(reportData?.imageUrl) {
                                        if (!reportData?.imageUrl.isNullOrBlank()) {
                                            ImageUtils.base64ToBitmap(reportData.imageUrl)
                                        } else null
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .then(
                                                if (beforeBitmap != null) {
                                                    Modifier.clickable {
                                                        zoomedImageBitmap = beforeBitmap.asImageBitmap()
                                                        zoomedImageTitle = if (isIndonesian) "Foto Sebelum (Laporan)" else "Before Photo (Report)"
                                                    }
                                                } else Modifier
                                            ),
                                        color = Color(0xFFE2E8F0)
                                    ) {
                                        if (beforeBitmap != null) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                Image(
                                                    bitmap = beforeBitmap.asImageBitmap(),
                                                    contentDescription = "Before Photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xCC0F172A)
                                                ) {
                                                    Text(
                                                        text = "🔍",
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(if (isIndonesian) "Foto Awal" else "Initial Photo", fontSize = 11.sp, color = textSecondary)
                                            }
                                        }
                                    }
                                }

                                // After photo
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isIndonesian) "Sesudah (Selesai):" else "After (Completed):",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val afterBitmap = remember(reportData?.completionImageUrl) {
                                        if (!reportData?.completionImageUrl.isNullOrBlank()) {
                                            ImageUtils.base64ToBitmap(reportData.completionImageUrl)
                                        } else null
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .then(
                                                if (afterBitmap != null) {
                                                    Modifier.clickable {
                                                        zoomedImageBitmap = afterBitmap.asImageBitmap()
                                                        zoomedImageTitle = if (isIndonesian) "Foto Sesudah (Selesai)" else "After Photo (Completed)"
                                                    }
                                                } else Modifier
                                            ),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        if (afterBitmap != null) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                Image(
                                                    bitmap = afterBitmap.asImageBitmap(),
                                                    contentDescription = "After Photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xCC0F172A)
                                                ) {
                                                    Text(
                                                        text = "🔍",
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(if (isIndonesian) "✓ Terverifikasi Selesai" else "✓ Verified Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                            }
                                        }
                                    }
                                }
                            }

                            if (!reportData?.completionNotes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(text = "📝", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isIndonesian) "Catatan Petugas / Teknisi:" else "Technician / Staff Notes:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534)
                                            )
                                            Text(
                                                text = reportData.completionNotes ?: "",
                                                fontSize = 12.sp,
                                                color = Color(0xFF14532D),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description Card
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
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = if (isIndonesian) "Deskripsi" else "Description",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = displayDescription,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Details Card
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
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = if (isIndonesian) "Rincian Laporan" else "Report Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        DetailRowItem(
                            iconBg = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
                            label = if (isIndonesian) "Kategori" else "Category",
                            value = displayCategory,
                            iconType = DetailIconType.TAG,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = borderCol,
                            thickness = 1.dp
                        )

                        val (pColor, pBg) = when (displayPriority.lowercase()) {
                            "darurat" -> Color(0xFFEF4444) to Color(0xFFFEE2E2)
                            "rendah" -> Color(0xFF10B981) to Color(0xFFD1FAE5)
                            else -> Color(0xFFF59E0B) to Color(0xFFFEF3C7)
                        }

                        DetailRowItem(
                            iconBg = pBg,
                            iconColor = pColor,
                            label = if (isIndonesian) "Tingkat Urgensi" else "Priority Level",
                            value = displayPriority.uppercase(),
                            iconType = DetailIconType.TAG,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = borderCol,
                            thickness = 1.dp
                        )

                        DetailRowItem(
                            iconBg = Color(0xFFECFDF5),
                            iconColor = Color(0xFF10B981),
                            label = if (isIndonesian) "Ditangani oleh" else "Handled by",
                            value = handledBy,
                            iconType = DetailIconType.TEAM,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Comments History Card
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
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (isIndonesian) "Komentar & Pembaruan" else "Comments & Updates"} (${commentsList.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )

                            Text(
                                text = "+ Add",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.clickable { showCommentSheet = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (commentsList.isEmpty()) {
                            Text(
                                text = if (isIndonesian) "Belum ada komentar atau pembaruan. Klik + Add atau tombol di bawah untuk menulis komentar pertama!" else "No comments or updates yet. Click + Add or the button below to write a comment!",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary,
                                lineHeight = 18.sp
                            )
                        } else {
                            commentsList.forEachIndexed { index, comment ->
                                CommentRowItem(comment, textPrimary, textSecondary)
                                if (index < commentsList.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = borderCol,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Button
                Button(
                    onClick = { showCommentSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = Color(0x402563EB),
                            ambientColor = Color(0x202563EB)
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommentIcon()
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isIndonesian) "Tambah Komentar" else "Add Comment",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Add Comment Modal Bottom Sheet
        if (showCommentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCommentSheet = false },
                sheetState = sheetState,
                containerColor = cardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isIndonesian) "Tambah Komentar atau Catatan" else "Add Comment or Update",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = pageBg,
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            if (newCommentText.isEmpty()) {
                                Text(
                                    text = if (isIndonesian) "Tuliskan catatan atau pembaruan untuk laporan ini..." else "Write a note or update for this report...",
                                    fontSize = 13.5.sp,
                                    color = textSecondary
                                )
                            }
                            BasicTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                textStyle = TextStyle(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                val newEntry = CommentData(
                                    id = System.currentTimeMillis().toString(),
                                    userName = reportData?.userName ?: "Pelapor",
                                    userRole = if (isIndonesian) "Pelapor" else "Reporter",
                                    commentText = newCommentText.trim(),
                                    timeAgo = if (isIndonesian) "Baru saja" else "Just now"
                                )
                                commentsList = commentsList + newEntry
                                newCommentText = ""
                                showCommentSheet = false
                            }
                        },
                        enabled = newCommentText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isIndonesian) "Kirim Komentar" else "Post Comment",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (zoomedImageBitmap != null) {
            ZoomableImageViewerDialog(
                imageBitmap = zoomedImageBitmap,
                title = zoomedImageTitle,
                subtitle = displayTitle,
                onDismiss = { zoomedImageBitmap = null }
            )
        }
    }
}

@Composable
private fun CommentRowItem(comment: CommentData, textPrimary: Color, textSecondary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        val initials = comment.userName.take(2).uppercase()
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFFEFF6FF), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.userName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = comment.userRole,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Text(
                    text = comment.timeAgo,
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.commentText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary,
                lineHeight = 17.sp
            )
        }
    }
}

enum class DetailIconType {
    TAG,
    TEAM,
    CALENDAR
}

@Composable
private fun DetailRowItem(
    iconBg: Color,
    iconColor: Color,
    label: String,
    value: String,
    iconType: DetailIconType,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                DetailIcon(iconType, iconColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
        }

        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
    }
}

@Composable
private fun ACFacilityPhotoIllustration() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(color = Color(0xFFE2E8F0))

        for (i in 1..4) {
            val y = h * (i * 0.2f)
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        for (i in 1..6) {
            val x = w * (i * 0.15f)
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        val acWidth = w * 0.55f
        val acHeight = h * 0.45f
        val acLeft = (w - acWidth) / 2f
        val acTop = (h - acHeight) / 2f

        drawRoundRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(acLeft, acTop),
            size = Size(acWidth, acHeight),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(acLeft, acTop),
            size = Size(acWidth, acHeight),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        drawRoundRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(acLeft + acWidth * 0.15f, acTop + acHeight * 0.2f),
            size = Size(acWidth * 0.7f, acHeight * 0.6f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        val dropColor = Color(0xFF38BDF8)
        drawCircle(dropColor, radius = 3.dp.toPx(), center = Offset(acLeft + acWidth * 0.3f, acTop + acHeight + 10.dp.toPx()))
        drawCircle(dropColor, radius = 4.dp.toPx(), center = Offset(acLeft + acWidth * 0.5f, acTop + acHeight + 18.dp.toPx()))
        drawCircle(dropColor, radius = 3.dp.toPx(), center = Offset(acLeft + acWidth * 0.7f, acTop + acHeight + 8.dp.toPx()))
    }
}

@Composable
private fun BackArrowIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.2f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.65f, h * 0.8f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun LocationPinIcon(color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.25f, h * 0.15f, w * 0.15f, h * 0.35f, w * 0.15f, h * 0.5f)
            cubicTo(w * 0.15f, h * 0.75f, w * 0.5f, h * 0.92f, w * 0.5f, h * 0.92f)
            cubicTo(w * 0.5f, h * 0.92f, w * 0.85f, h * 0.75f, w * 0.85f, h * 0.5f)
            cubicTo(w * 0.85f, h * 0.35f, w * 0.75f, h * 0.15f, w * 0.5f, h * 0.15f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun CircleCheckIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(color = color, radius = w * 0.45f, style = Stroke(width = 2.dp.toPx()))
        val checkPath = Path().apply {
            moveTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.66f)
            lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(
            path = checkPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun CircleWarningIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(color = color, radius = w * 0.45f, style = Stroke(width = 2.dp.toPx()))
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.30f),
            end = Offset(w * 0.5f, h * 0.58f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = color, radius = 1.4.dp.toPx(), center = Offset(w * 0.5f, h * 0.72f))
    }
}

@Composable
private fun CircleOutlineIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        drawCircle(color = color, radius = w * 0.45f, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun DetailIcon(type: DetailIconType, color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height

        when (type) {
            DetailIconType.TAG -> {
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.15f)
                    lineTo(w * 0.55f, h * 0.15f)
                    lineTo(w * 0.9f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.9f)
                    lineTo(w * 0.15f, h * 0.55f)
                    close()
                }
                drawPath(path, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(color = color, radius = 1.2.dp.toPx(), center = Offset(w * 0.35f, h * 0.35f))
            }
            DetailIconType.TEAM -> {
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(w * 0.35f, h * 0.35f))
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(w * 0.65f, h * 0.35f))
                val body = Path().apply {
                    moveTo(w * 0.15f, h * 0.8f)
                    cubicTo(w * 0.15f, h * 0.6f, w * 0.55f, h * 0.6f, w * 0.55f, h * 0.8f)
                }
                drawPath(body, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
            }
            DetailIconType.CALENDAR -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.15f, h * 0.22f),
                    size = Size(w * 0.7f, h * 0.65f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawLine(color = color, start = Offset(w * 0.15f, h * 0.42f), end = Offset(w * 0.85f, h * 0.42f), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.12f), end = Offset(w * 0.35f, h * 0.28f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.65f, h * 0.12f), end = Offset(w * 0.65f, h * 0.28f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun CommentIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.2f)
            quadraticTo(w * 0.95f, h * 0.2f, w * 0.95f, h * 0.3f)
            lineTo(w * 0.95f, h * 0.7f)
            quadraticTo(w * 0.95f, h * 0.8f, w * 0.85f, h * 0.8f)
            lineTo(w * 0.45f, h * 0.8f)
            lineTo(w * 0.2f, h * 0.95f)
            lineTo(w * 0.2f, h * 0.8f)
            lineTo(w * 0.15f, h * 0.8f)
            quadraticTo(w * 0.05f, h * 0.8f, w * 0.05f, h * 0.7f)
            lineTo(w * 0.05f, h * 0.3f)
            quadraticTo(w * 0.05f, h * 0.2f, w * 0.15f, h * 0.2f)
            close()
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReportDetailTracScreenPreview() {
    ReportDetailTracScreen()
}
