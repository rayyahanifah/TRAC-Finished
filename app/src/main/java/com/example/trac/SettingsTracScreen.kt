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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trac.components.ZoomableImageViewerDialog
import com.example.trac.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsTracScreen(
    userName: String = "Joshua Benjamin",
    userEmail: String = "joshua@trac.id",
    userProfileImage: String = "",
    userRoleInitial: String = "Siswa",
    isIndonesianInitial: Boolean = false,
    isDarkModeInitial: Boolean = false,
    onBackClick: () -> Unit = {},
    onLanguageChange: (isIndonesian: Boolean) -> Unit = {},
    onThemeChange: (isDark: Boolean) -> Unit = {},
    onRoleChange: (newRole: String) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current

    var isIndonesian by rememberSaveable { mutableStateOf(isIndonesianInitial) }
    var isDarkMode by rememberSaveable { mutableStateOf(isDarkModeInitial) }
    var showZoomedAvatar by remember { mutableStateOf(false) }
    var currentRole by rememberSaveable { mutableStateOf(userRoleInitial) }

    // Dynamic Theme Colors
    val pageBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

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

            // 1. Header Bar: Circle Back Button + Title "Settings" / "Pengaturan"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = headerAlpha.value
                        translationY = headerOffsetY.value.dp.toPx()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onBackClick() }
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = if (isDarkMode) Color(0x30000000) else Color(0x1A000000)
                        ),
                    shape = CircleShape,
                    color = cardBg
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ChevronLeftIcon(color = textPrimary)
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                Text(
                    text = if (isIndonesian) "Pengaturan" else "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffsetY.value.dp.toPx()
                    }
            ) {
                // 2. User Account Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0x0D000000)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val profileBitmap = remember(userProfileImage) {
                            if (userProfileImage.isNotBlank()) {
                                ImageUtils.base64ToBitmap(userProfileImage)
                            } else null
                        }

                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .clickable(enabled = profileBitmap != null) {
                                    showZoomedAvatar = true
                                },
                            color = Color(0xFFEFF6FF)
                        ) {
                            if (profileBitmap != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = profileBitmap.asImageBitmap(),
                                        contentDescription = "User Avatar",
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
                                val initials = if (userName.isNotBlank()) userName.take(2).uppercase() else "RA"
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = userEmail,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Section 1: LANGUAGE
                Text(
                    text = if (isIndonesian) "BAHASA" else "LANGUAGE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 1.1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Option 1: English
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isIndonesian = false
                                    onLanguageChange(false)
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlobeIcon(color = if (!isIndonesian) Color(0xFF2563EB) else textSecondary)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "English",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }

                            RadioButton(
                                selected = !isIndonesian,
                                onClick = {
                                    isIndonesian = false
                                    onLanguageChange(false)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF2563EB),
                                    unselectedColor = Color(0xFF94A3B8)
                                )
                            )
                        }

                        HorizontalDivider(color = borderCol, thickness = 1.dp)

                        // Option 2: Indonesian
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isIndonesian = true
                                    onLanguageChange(true)
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlobeIcon(color = if (isIndonesian) Color(0xFF2563EB) else textSecondary)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Indonesian (Bahasa)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }

                            RadioButton(
                                selected = isIndonesian,
                                onClick = {
                                    isIndonesian = true
                                    onLanguageChange(true)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF2563EB),
                                    unselectedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Section 2: THEME / APPEARANCE
                Text(
                    text = if (isIndonesian) "TEMA / TAMPILAN" else "THEME / APPEARANCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 1.1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Light Mode Segment Button
                        val isLightActive = !isDarkMode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable {
                                    isDarkMode = false
                                    onThemeChange(false)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isLightActive) Color(0xFFEFF6FF) else Color.Transparent,
                            border = if (isLightActive) BorderStroke(1.5.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                SunIcon(color = if (isLightActive) Color(0xFF2563EB) else textSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isIndonesian) "Terang" else "Light",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLightActive) Color(0xFF2563EB) else textSecondary
                                )
                            }
                        }

                        // Dark Mode Segment Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable {
                                    isDarkMode = true
                                    onThemeChange(true)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) Color(0xFF334155) else Color.Transparent,
                            border = if (isDarkMode) BorderStroke(1.5.dp, Color(0xFF38BDF8)) else BorderStroke(1.dp, Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MoonIcon(color = if (isDarkMode) Color(0xFF38BDF8) else textSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isIndonesian) "Gelap" else "Dark",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF38BDF8) else textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 3: STATUS AKUN & PERAN
                Text(
                    text = if (isIndonesian) "STATUS & PERAN AKUN" else "ACCOUNT ROLE & STATUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 1.1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isAdminUser = userRoleInitial.contains("Admin", ignoreCase = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isAdminUser) (if (isIndonesian) "Admin / Pengurus Fasilitas" else "Facility Administrator")
                                    else (if (isIndonesian) "Siswa / Pelapor Terdaftar" else "Student Reporter"),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAdminUser) Color(0xFF2563EB) else Color(0xFF10B981)
                                ) {
                                    Text(
                                        text = if (isAdminUser) "ADMIN" else "TERVERIFIKASI",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAdminUser)
                                    (if (isIndonesian) "Memiliki hak istimewa mengelola laporan & menugaskan staf." else "Has administrative privileges to manage reports and staff.")
                                else
                                    (if (isIndonesian) "Hak akses: membuat laporan & memantau status fasilitas." else "Access rights: submit reports & track facility status."),
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 5. Sign Out Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { onLogoutClick() }
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(18.dp),
                            spotColor = Color(0x15EF4444)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LogOutIconRed()
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isIndonesian) "Keluar dari Akun" else "Sign Out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Footer Version
                Text(
                    text = "TRAC - Alpha 1.0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
private fun GlobeIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(color = color, radius = w * 0.42f, style = Stroke(width = 1.8.dp.toPx()))
        drawLine(color = color, start = Offset(w * 0.1f, h * 0.5f), end = Offset(w * 0.9f, h * 0.5f), strokeWidth = 1.5.dp.toPx())
        drawCircle(color = color, radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun SunIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(color = color, radius = w * 0.25f, style = Stroke(width = 2.dp.toPx()))
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val startX = (w * 0.5f + Math.cos(angle) * (w * 0.35f)).toFloat()
            val startY = (h * 0.5f + Math.sin(angle) * (h * 0.35f)).toFloat()
            val endX = (w * 0.5f + Math.cos(angle) * (w * 0.45f)).toFloat()
            val endY = (h * 0.5f + Math.sin(angle) * (h * 0.45f)).toFloat()
            drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun MoonIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.8f, h * 0.65f)
            cubicTo(w * 0.4f, h * 0.8f, w * 0.15f, h * 0.55f, w * 0.3f, h * 0.15f)
            cubicTo(w * 0.15f, h * 0.35f, w * 0.25f, h * 0.75f, w * 0.8f, h * 0.65f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun LogOutIconRed() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val color = Color(0xFFEF4444)

        val doorPath = Path().apply {
            moveTo(w * 0.55f, h * 0.15f)
            lineTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.2f, h * 0.85f)
            lineTo(w * 0.55f, h * 0.85f)
        }
        drawPath(doorPath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(color = color, start = Offset(w * 0.4f, h * 0.5f), end = Offset(w * 0.85f, h * 0.5f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)

        val arrowPath = Path().apply {
            moveTo(w * 0.7f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.7f, h * 0.65f)
        }
        drawPath(arrowPath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun ChevronLeftIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsTracScreenPreview() {
    SettingsTracScreen()
}
