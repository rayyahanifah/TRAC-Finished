package com.example.trac.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.trac.Screen
import com.example.trac.util.ImageUtils

enum class SidebarMenuItem {
    HOME,
    CREATE_REPORT,
    MY_REPORTS,
    HISTORY,
    ADMIN_PANEL,
    PROFILE,
    SETTINGS
}

enum class SidebarIconType {
    HOME,
    CREATE_REPORT,
    MY_REPORTS,
    ADMIN_PANEL,
    PROFILE,
    SETTINGS
}

@Composable
fun ProfileSidebarDrawer(
    isOpen: Boolean,
    userName: String = "Joshua Benjamin",
    userClass: String = "XI RPL",
    userRole: String = "Siswa / Pelapor",
    userProfileImage: String = "",
    isAdmin: Boolean = false,
    isIndonesian: Boolean = false,
    isDarkMode: Boolean = false,
    currentScreen: Screen = Screen.HOME,
    onClose: () -> Unit = {},
    onMenuItemClick: (SidebarMenuItem) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    // Dynamic Theme Colors
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)

    var showZoomedAvatar by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Dark Scrim Overlay with Fade Animation
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isOpen) 0.5f else 0f,
            animationSpec = tween(300),
            label = "ScrimAlpha"
        )

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable { onClose() }
            )
        }

        // 2. Right-Side Drawer Panel with Smooth Right-to-Left Slide Animation
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Main Drawer Panel
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.76f)
                        .shadow(
                            elevation = 20.dp,
                            spotColor = Color(0x35000000),
                            ambientColor = Color(0x15000000)
                        ),
                    color = cardBg
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Header: TRAC Portal & Logo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ShieldLogoSmall()

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "TRAC Portal",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tradevis Track and Care",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            HorizontalDivider(
                                color = borderCol,
                                thickness = 1.dp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Navigation Menu Items
                            SidebarNavItem(
                                icon = SidebarIconType.HOME,
                                label = if (isIndonesian) "Beranda" else "Home",
                                isSelected = currentScreen == Screen.HOME,
                                isDarkMode = isDarkMode,
                                onClick = {
                                    onMenuItemClick(SidebarMenuItem.HOME)
                                    onClose()
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SidebarNavItem(
                                icon = SidebarIconType.CREATE_REPORT,
                                label = if (isIndonesian) "Buat Laporan" else "Create Report",
                                isSelected = currentScreen == Screen.CREATE_REPORT,
                                isDarkMode = isDarkMode,
                                onClick = {
                                    onMenuItemClick(SidebarMenuItem.CREATE_REPORT)
                                    onClose()
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SidebarNavItem(
                                icon = SidebarIconType.MY_REPORTS,
                                label = if (isIndonesian) "Laporan Saya" else "My Reports",
                                isSelected = currentScreen == Screen.REPORT_LIST,
                                isDarkMode = isDarkMode,
                                onClick = {
                                    onMenuItemClick(SidebarMenuItem.MY_REPORTS)
                                    onClose()
                                }
                            )

                            if (isAdmin) {
                                Spacer(modifier = Modifier.height(8.dp))

                                SidebarAdminNavItem(
                                    label = if (isIndonesian) "Admin Panel" else "Admin Dashboard",
                                    badge = "ADMIN",
                                    isSelected = currentScreen == Screen.ADMIN_DASHBOARD,
                                    isDarkMode = isDarkMode,
                                    onClick = {
                                        onMenuItemClick(SidebarMenuItem.ADMIN_PANEL)
                                        onClose()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            SidebarNavItem(
                                icon = SidebarIconType.PROFILE,
                                label = if (isIndonesian) "Profil & Identitas" else "Profile",
                                isSelected = false,
                                isDarkMode = isDarkMode,
                                onClick = {
                                    onMenuItemClick(SidebarMenuItem.PROFILE)
                                    onClose()
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SidebarNavItem(
                                icon = SidebarIconType.SETTINGS,
                                label = if (isIndonesian) "Pengaturan" else "Settings",
                                isSelected = false,
                                isDarkMode = isDarkMode,
                                onClick = {
                                    onMenuItemClick(SidebarMenuItem.SETTINGS)
                                    onClose()
                                }
                            )
                        }

                        // Bottom User Profile & Log Out Section
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                color = borderCol,
                                thickness = 1.dp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // User Info Card
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val profileBitmap = remember(userProfileImage) {
                                    if (userProfileImage.isNotBlank()) {
                                        ImageUtils.base64ToBitmap(userProfileImage)
                                    } else null
                                }

                                Surface(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0x202563EB))
                                        .clip(CircleShape)
                                        .clickable(enabled = profileBitmap != null) {
                                            showZoomedAvatar = true
                                        },
                                    color = if (isAdmin) Color(0xFFEEF2FF) else Color(0xFFEFF6FF)
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
                                        val initials = if (userName.isNotBlank()) {
                                            userName.take(2).uppercase()
                                        } else "TR"

                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAdmin) Color(0xFF4F46E5) else Color(0xFF2563EB)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = userName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val displayRole = if (isAdmin) {
                                        if (isIndonesian) "Pengurus / Admin" else "Administrator"
                                    } else {
                                        userRole
                                    }
                                    Text(
                                        text = "$displayRole • $userClass",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isAdmin) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAdmin) Color(0xFF4F46E5) else textSecondary
                                    )
                                }
                            }

                            if (isAdmin) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onMenuItemClick(SidebarMenuItem.ADMIN_PANEL)
                                            onClose()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.7f) else Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF4338CA) else Color(0xFFC7D2FE))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SidebarIcon(SidebarIconType.ADMIN_PANEL, Color(0xFF4F46E5))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isIndonesian) "Buka Admin Hub" else "Open Admin Hub",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF3730A3)
                                                )
                                                Text(
                                                    text = if (isIndonesian) "Kelola laporan & fasilitas" else "Manage reports & facilities",
                                                    fontSize = 10.5.sp,
                                                    color = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF6366F1)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "→",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Log Out Red Button
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        onLogoutClick()
                                        onClose()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFEF2F2)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    LogOutIcon()
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isIndonesian) "Keluar" else "Log Out",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }

                // Circle Chevron Button (<) Floating Outside White Panel in Dark Scrim Area
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-21).dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onClose() }
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                spotColor = Color(0x35000000)
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
                }
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
private fun SidebarNavItem(
    icon: SidebarIconType,
    label: String,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF2563EB) else if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarIcon(icon, contentColor)

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                fontSize = 14.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SidebarIcon(type: SidebarIconType, color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        when (type) {
            SidebarIconType.HOME -> {
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.9f)
                    lineTo(w * 0.15f, h * 0.45f)
                    lineTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.85f, h * 0.45f)
                    lineTo(w * 0.85f, h * 0.9f)
                    close()
                }
                drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SidebarIconType.CREATE_REPORT -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.7f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawLine(color = color, start = Offset(w * 0.5f, h * 0.38f), end = Offset(w * 0.5f, h * 0.62f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.38f, h * 0.5f), end = Offset(w * 0.62f, h * 0.5f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            SidebarIconType.MY_REPORTS -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.7f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.38f), end = Offset(w * 0.65f, h * 0.38f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.52f), end = Offset(w * 0.65f, h * 0.52f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.66f), end = Offset(w * 0.55f, h * 0.66f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            SidebarIconType.ADMIN_PANEL -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.85f, h * 0.28f)
                    lineTo(w * 0.85f, h * 0.6f)
                    cubicTo(w * 0.85f, h * 0.82f, w * 0.5f, h * 0.95f, w * 0.5f, h * 0.95f)
                    cubicTo(w * 0.5f, h * 0.95f, w * 0.15f, h * 0.82f, w * 0.15f, h * 0.6f)
                    lineTo(w * 0.15f, h * 0.28f)
                    close()
                }
                drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
            }
            SidebarIconType.PROFILE -> {
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(w * 0.5f, h * 0.32f), style = Stroke(width = 2.dp.toPx()))
                val body = Path().apply {
                    moveTo(w * 0.2f, h * 0.85f)
                    cubicTo(w * 0.2f, h * 0.6f, w * 0.8f, h * 0.6f, w * 0.8f, h * 0.85f)
                }
                drawPath(body, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
            SidebarIconType.SETTINGS -> {
                drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = color, radius = 7.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun SidebarAdminNavItem(
    label: String,
    badge: String,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFFEEF2FF) else if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.6f) else Color(0xFFF5F3FF)
    val contentColor = Color(0xFF4F46E5)
    val borderCol = if (isSelected) Color(0xFF6366F1) else Color(0xFFC7D2FE).copy(alpha = 0.5f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarIcon(SidebarIconType.ADMIN_PANEL, contentColor)

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF4F46E5)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ShieldLogoSmall() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color(0x352563EB)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2563EB),
                        Color(0xFF1D4ED8)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height

            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.10f)
                cubicTo(w * 0.70f, h * 0.10f, w * 0.86f, h * 0.14f, w * 0.86f, h * 0.26f)
                cubicTo(w * 0.86f, h * 0.58f, w * 0.68f, h * 0.82f, w * 0.5f, h * 0.90f)
                cubicTo(w * 0.32f, h * 0.82f, w * 0.14f, h * 0.58f, w * 0.14f, h * 0.26f)
                cubicTo(w * 0.14f, h * 0.14f, w * 0.30f, h * 0.10f, w * 0.5f, h * 0.10f)
                close()
            }
            drawPath(shieldPath, color = Color.White, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            val checkPath = Path().apply {
                moveTo(w * 0.37f, h * 0.48f)
                lineTo(w * 0.47f, h * 0.58f)
                lineTo(w * 0.63f, h * 0.39f)
            }
            drawPath(checkPath, color = Color.White, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun LogOutIcon() {
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
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.2f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.8f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileSidebarDrawerPreview() {
    ProfileSidebarDrawer(isOpen = true)
}
