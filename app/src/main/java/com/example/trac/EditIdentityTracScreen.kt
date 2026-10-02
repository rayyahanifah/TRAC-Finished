package com.example.trac

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trac.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIdentityTracScreen(
    currentName: String = "Joshua Benjamin",
    currentClass: String = "XI RPL",
    currentRole: String = "Siswa / Pelapor",
    currentProfileImage: String = "",
    isLoading: Boolean = false,
    isIndonesian: Boolean = false,
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onUpdateIdentityClick: (newName: String, newClass: String, profileImageBase64: String?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    var isEncodingImage by remember { mutableStateOf(false) }

    // Dynamic Theme Colors
    val pageBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

    var fullName by rememberSaveable { mutableStateOf(currentName) }
    var userClass by rememberSaveable { mutableStateOf(currentClass) }
    var customClass by rememberSaveable { mutableStateOf("") }

    var showClassPickerSheet by remember { mutableStateOf(false) }
    var classSearchQuery by remember { mutableStateOf("") }
    val classSheetState = rememberModalBottomSheetState()

    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            photoBitmap = bitmap
            photoUri = null
        }
        showPhotoPickerSheet = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            photoBitmap = null
        }
        showPhotoPickerSheet = false
    }

    // Real Class List
    val realClassList = listOf(
        "X AKL 1",
        "X AKL 2",
        "X AKL 3",
        "X BD",
        "X Manlog",
        "X MP",
        "X RPL",
        "X ULW",
        "XI AK 1",
        "XI AK 2",
        "XI AK 3",
        "XI BD",
        "XI BR 1",
        "XI BR 2",
        "XI Manlog",
        "XI MP",
        "XI RPL",
        "XI ULW",
        "XII AK 1",
        "XII AK 2",
        "XII AK 3",
        "XII BD",
        "XII BR 1",
        "XII BR 2",
        "XII MP 1",
        "XII MP 2",
        "XII RPL",
        "XII ULW",
        "Lainnya (Kustom)"
    )

    val finalClass = if (userClass == "Lainnya (Kustom)") customClass else userClass

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
                    text = if (isIndonesian) "Edit Identitas" else "Edit Identity",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffsetY.value.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar Circle
                var avatarBitmap by remember { mutableStateOf<Bitmap?>(photoBitmap) }
                LaunchedEffect(photoBitmap, photoUri, currentProfileImage) {
                    if (photoBitmap != null) {
                        avatarBitmap = photoBitmap
                    } else if (photoUri != null) {
                        withContext(Dispatchers.IO) {
                            val decoded = ImageUtils.uriToBitmap(context, photoUri!!)
                            withContext(Dispatchers.Main) {
                                avatarBitmap = decoded
                            }
                        }
                    } else if (currentProfileImage.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            val decoded = ImageUtils.base64ToBitmap(currentProfileImage)
                            withContext(Dispatchers.Main) {
                                avatarBitmap = decoded
                            }
                        }
                    } else {
                        avatarBitmap = null
                    }
                }

                Box(
                    modifier = Modifier.size(110.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                spotColor = Color(0x202563EB)
                            )
                            .clip(CircleShape),
                        color = Color(0xFFEFF6FF)
                    ) {
                        val currentAvatar = avatarBitmap
                        if (currentAvatar != null) {
                            Image(
                                bitmap = currentAvatar.asImageBitmap(),
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initials = if (fullName.isNotBlank()) fullName.take(2).uppercase() else "RA"
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x302563EB))
                            .background(Color(0xFF2563EB), shape = CircleShape)
                            .clickable { showPhotoPickerSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        SmallCameraIcon()
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isIndonesian) "Ubah Foto Profil" else "Change Profile Photo",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.clickable { showPhotoPickerSheet = true }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Edit Form Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(22.dp),
                            spotColor = Color(0x0D000000)
                        ),
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Field 1: Full Name
                        Text(
                            text = if (isIndonesian) "Nama Lengkap" else "Full Name",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        TracTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = if (isIndonesian) "Masukkan nama lengkap..." else "Enter full name...",
                            isDarkMode = isDarkMode
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Field 2: Class / Grade
                        Text(
                            text = if (isIndonesian) "Kelas / Tingkat" else "Class / Grade",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0x0D000000)
                                )
                                .clickable { showClassPickerSheet = true },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, borderCol)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (userClass.isNotBlank()) userClass else if (isIndonesian) "Pilih Kelas..." else "Select Class...",
                                    fontSize = 14.sp,
                                    color = if (userClass.isNotBlank()) textPrimary else textSecondary,
                                    fontWeight = if (userClass.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                                )

                                ChevronDownIcon()
                            }
                        }

                        if (userClass == "Lainnya (Kustom)") {
                            Spacer(modifier = Modifier.height(8.dp))
                            TracTextField(
                                value = customClass,
                                onValueChange = { customClass = it },
                                placeholder = if (isIndonesian) "Tuliskan nama kelas..." else "Type class name...",
                                isDarkMode = isDarkMode
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Field 3: System Role
                        Text(
                            text = if (isIndonesian) "Peran Sistem" else "System Role",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, borderCol)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShieldRoleIcon()
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = currentRole,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary Button
                Button(
                    onClick = {
                        if (fullName.isNotBlank() && finalClass.isNotBlank() && !isLoading && !isEncodingImage) {
                            coroutineScope.launch {
                                isEncodingImage = true
                                val encodedImage = when {
                                    photoBitmap != null -> ImageUtils.bitmapToBase64Async(photoBitmap!!)
                                    photoUri != null -> ImageUtils.uriToBase64Async(context, photoUri!!)
                                    currentProfileImage.isNotBlank() -> currentProfileImage
                                    else -> null
                                }
                                isEncodingImage = false
                                onUpdateIdentityClick(fullName.trim(), finalClass.trim(), encodedImage)
                            }
                        }
                    },
                    enabled = fullName.isNotBlank() && finalClass.isNotBlank() && !isLoading && !isEncodingImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = Color(0x402563EB),
                            ambientColor = Color(0x202563EB)
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF93C5FD),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    if (isLoading || isEncodingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (isIndonesian) "Simpan Identitas" else "Update Identity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Searchable Class Picker Modal Bottom Sheet
        if (showClassPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showClassPickerSheet = false },
                sheetState = classSheetState,
                containerColor = cardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isIndonesian) "Pilih Kelas / Tingkat" else "Select Class / Grade",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-Time Class Search Input Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SearchIconSmall(color = textSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (classSearchQuery.isEmpty()) {
                                    Text(
                                        text = if (isIndonesian) "Cari nama kelas... (contoh: RPL, AKL)" else "Search class... (e.g. RPL, AKL)",
                                        fontSize = 13.5.sp,
                                        color = textSecondary
                                    )
                                }
                                BasicTextField(
                                    value = classSearchQuery,
                                    onValueChange = { classSearchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val filteredClasses = realClassList.filter {
                        it.contains(classSearchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredClasses) { cls ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userClass = cls
                                        showClassPickerSheet = false
                                        classSearchQuery = ""
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (cls == userClass) Color(0xFFEFF6FF) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cls,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (cls == userClass) FontWeight.Bold else FontWeight.Medium,
                                        color = if (cls == userClass) Color(0xFF2563EB) else textPrimary
                                    )

                                    if (cls == userClass) {
                                        Text(
                                            text = "✓",
                                            fontSize = 14.sp,
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

        // Photo Picker Modal Bottom Sheet
        if (showPhotoPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhotoPickerSheet = false },
                sheetState = sheetState,
                containerColor = cardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isIndonesian) "Pilih Foto Profil" else "Select Profile Photo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { cameraLauncher.launch(null) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEFF6FF)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📷", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (isIndonesian) "Kamera (Ambil Foto)" else "Camera (Take Photo)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🖼️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (isIndonesian) "Galeri HP (Pilih Foto)" else "Gallery (Choose Photo)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchIconSmall(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = color,
            radius = w * 0.32f,
            center = Offset(w * 0.4f, h * 0.4f),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.62f),
            end = Offset(w * 0.88f, h * 0.88f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SmallCameraIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height

        val cameraBody = Path().apply {
            moveTo(w * 0.15f, h * 0.35f)
            lineTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.42f, h * 0.25f)
            lineTo(w * 0.58f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.35f)
            quadraticTo(w * 0.95f, h * 0.35f, w * 0.95f, h * 0.45f)
            lineTo(w * 0.95f, h * 0.8f)
            quadraticTo(w * 0.95f, h * 0.9f, w * 0.85f, h * 0.9f)
            lineTo(w * 0.15f, h * 0.9f)
            quadraticTo(w * 0.05f, h * 0.9f, w * 0.05f, h * 0.8f)
            lineTo(w * 0.05f, h * 0.45f)
            quadraticTo(w * 0.05f, h * 0.35f, w * 0.15f, h * 0.35f)
            close()
        }
        drawPath(
            path = cameraBody,
            color = Color.White,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color = Color.White, radius = 2.8.dp.toPx(), center = Offset(w * 0.5f, h * 0.62f), style = Stroke(width = 1.6.dp.toPx()))
    }
}

@Composable
private fun ShieldRoleIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
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
        drawPath(shieldPath, color = Color(0xFF2563EB), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun ChevronDownIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.75f, h * 0.35f)
        }
        drawPath(
            path = path,
            color = Color(0xFF64748B),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
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
fun EditIdentityTracScreenPreview() {
    EditIdentityTracScreen()
}
