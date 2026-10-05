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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trac.components.InAppBanner
import com.example.trac.components.ZoomableImageViewerDialog
import com.example.trac.data.SessionPreferences
import com.example.trac.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FloorLocationGroup(
    val floorName: String,
    val rooms: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportTracScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isIndonesian: Boolean = false,
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onHomeTabClick: () -> Unit = {},
    onReportsTabClick: () -> Unit = {},
    onSubmitReportClick: (category: String, location: String, title: String, description: String, imageUrl: String?, priority: String) -> Unit = { _, _, _, _, _, _ -> },
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    var isEncodingImage by remember { mutableStateOf(false) }
    var zoomedImageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var zoomedImageTitle by remember { mutableStateOf("Pratinjau Foto") }

    // Dynamic Theme Colors
    val pageBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFD)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var selectedLocation by rememberSaveable { mutableStateOf("") }
    var customCategory by rememberSaveable { mutableStateOf("") }
    var customLocation by rememberSaveable { mutableStateOf("") }

    var reportTitle by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedPriority by rememberSaveable { mutableStateOf("Sedang") }

    var isCategoryExpanded by remember { mutableStateOf(false) }

    // Searchable Location Bottom Sheet States
    var showLocationPickerSheet by remember { mutableStateOf(false) }
    var locationSearchQuery by remember { mutableStateOf("") }
    val locationSheetState = rememberModalBottomSheetState()

    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }

    val photoSheetState = rememberModalBottomSheetState()

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

    val categories = listOf(
        "Electronics",
        "Furniture",
        "Plumbing",
        "Building Facility",
        "AC & Air System",
        "Lighting",
        "Sanitary / Toilet",
        "Lainnya (Kustom)"
    )

    val sessionPrefs = remember { SessionPreferences(context) }
    val facilityLocations = remember { sessionPrefs.getFacilityLocations() }

    // Floor Grouped Room Locations Data synced with Master Data & Admin settings
    val locationGroups = remember(facilityLocations, isIndonesian) {
        facilityLocations.map { loc ->
            FloorLocationGroup(
                floorName = loc.floorName,
                rooms = loc.rooms
            )
        } + FloorLocationGroup(
            floorName = if (isIndonesian) "LAINNYA" else "OTHER",
            rooms = listOf(if (isIndonesian) "Lainnya (Kustom)" else "Other (Custom)")
        )
    }

    val finalCategory = if (selectedCategory == "Lainnya (Kustom)") customCategory else selectedCategory
    val finalLocation = if (selectedLocation == "Lainnya (Kustom)") customLocation else selectedLocation

    val isFormValid = finalCategory.isNotBlank() && finalLocation.isNotBlank() &&
            reportTitle.isNotBlank() && description.isNotBlank()

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

                Spacer(modifier = Modifier.width(18.dp))

                Text(
                    text = if (isIndonesian) "Buat Laporan Baru" else "Create New Report",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            InAppBanner(
                message = errorMessage,
                isError = true
            )

            // Main Body Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffsetY.value.dp.toPx()
                    }
            ) {
                // Field 1: Category Dropdown
                Text(
                    text = if (isIndonesian) "Kategori" else "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = Color(0x0D000000)
                            )
                            .clickable { isCategoryExpanded = true },
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
                                text = if (selectedCategory.isNotBlank()) selectedCategory else if (isIndonesian) "Pilih kategori..." else "Select category...",
                                fontSize = 14.sp,
                                color = if (selectedCategory.isNotBlank()) textPrimary else textSecondary,
                                fontWeight = if (selectedCategory.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                            )

                            ChevronDownIcon()
                        }
                    }

                    DropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .background(cardBg)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = category,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textPrimary
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedCategory == "Lainnya (Kustom)") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TracTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        placeholder = if (isIndonesian) "Tuliskan kategori kustom..." else "Type custom category...",
                        isDarkMode = isDarkMode
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Field 2: Location Picker Field (Click to open Grouped Searchable Sheet)
                Text(
                    text = if (isIndonesian) "Lokasi" else "Location",
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
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0x0D000000)
                        )
                        .clickable { showLocationPickerSheet = true },
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
                            text = if (selectedLocation.isNotBlank()) selectedLocation else if (isIndonesian) "Pilih lokasi ruangan..." else "Select location...",
                            fontSize = 14.sp,
                            color = if (selectedLocation.isNotBlank()) textPrimary else textSecondary,
                            fontWeight = if (selectedLocation.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                        )

                        ChevronDownIcon()
                    }
                }

                if (selectedLocation == "Lainnya (Kustom)") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TracTextField(
                        value = customLocation,
                        onValueChange = { customLocation = it },
                        placeholder = if (isIndonesian) "Tuliskan lokasi kustom..." else "Type custom location...",
                        isDarkMode = isDarkMode
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Field 3: Report Title Input
                Text(
                    text = if (isIndonesian) "Judul Laporan" else "Report Title",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                TracTextField(
                    value = reportTitle,
                    onValueChange = { reportTitle = it },
                    placeholder = if (isIndonesian) "contoh: Lampu kelas tidak menyala" else "e.g. Light not working",
                    isDarkMode = isDarkMode
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Field 4: Description Multiline Area
                Text(
                    text = if (isIndonesian) "Deskripsi Laporan" else "Description",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
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
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        if (description.isEmpty()) {
                            Text(
                                text = if (isIndonesian) "Jelaskan masalah fasilitas secara detail..." else "Describe the issue in detail...",
                                fontSize = 14.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        BasicTextField(
                            value = description,
                            onValueChange = { description = it },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Field 5: Urgency / Priority Level Selection
                Text(
                    text = if (isIndonesian) "Tingkat Urgensi / Prioritas" else "Priority Level",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val priorityOptions = listOf(
                    Triple("Rendah", if (isIndonesian) "Rendah" else "Low", Color(0xFF10B981)),
                    Triple("Sedang", if (isIndonesian) "Sedang" else "Medium", Color(0xFFF59E0B)),
                    Triple("Darurat", if (isIndonesian) "Darurat" else "Emergency", Color(0xFFEF4444))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    priorityOptions.forEach { (key, label, accentColor) ->
                        val isSelected = selectedPriority.equals(key, ignoreCase = true)
                        val chipBg = if (isSelected) accentColor.copy(alpha = 0.15f) else cardBg
                        val chipBorder = if (isSelected) accentColor else borderCol

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPriority = key }
                                .shadow(
                                    elevation = if (isSelected) 4.dp else 1.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    spotColor = if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0x0A000000)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            color = chipBg,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, chipBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(accentColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) accentColor else textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 6: Photo
                Text(
                    text = if (isIndonesian) "Foto Fasilitas (Opsional)" else "Photo (Optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = Color(0x0A000000)
                            )
                            .clickable { showPhotoPickerSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CameraIcon()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isIndonesian) "Tambah" else "Add Photo",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(photoBitmap, photoUri) {
                        if (photoBitmap != null) {
                            previewBitmap = photoBitmap
                        } else if (photoUri != null) {
                            withContext(Dispatchers.IO) {
                                val decoded = ImageUtils.uriToBitmap(context, photoUri!!)
                                withContext(Dispatchers.Main) {
                                    previewBitmap = decoded
                                }
                            }
                        } else {
                            previewBitmap = null
                        }
                    }

                    val currentPreview = previewBitmap
                    if (currentPreview != null) {
                        Box(
                            modifier = Modifier.size(80.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = Color(0x10000000)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        zoomedImageBitmap = currentPreview.asImageBitmap()
                                        zoomedImageTitle = if (isIndonesian) "Foto Bukti Kerusakan" else "Damage Proof Photo"
                                    },
                                color = Color(0xFFE2E8F0)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = currentPreview.asImageBitmap(),
                                        contentDescription = "Report Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xCC000000)
                                    ) {
                                        Text("🔍", fontSize = 8.sp, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(Color(0xFFEF4444), shape = CircleShape)
                                    .clickable {
                                        photoBitmap = null
                                        photoUri = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary "+ Submit Report" Button
                Button(
                    onClick = {
                        if (isFormValid && !isLoading && !isEncodingImage) {
                            coroutineScope.launch {
                                isEncodingImage = true
                                val encodedImage = when {
                                    photoBitmap != null -> ImageUtils.bitmapToBase64Async(photoBitmap!!)
                                    photoUri != null -> ImageUtils.uriToBase64Async(context, photoUri!!)
                                    else -> null
                                }
                                isEncodingImage = false
                                onSubmitReportClick(finalCategory, finalLocation, reportTitle, description, encodedImage, selectedPriority)
                            }
                        }
                    },
                    enabled = isFormValid && !isLoading && !isEncodingImage,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlusIcon(color = Color.White, size = 18.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isIndonesian) "Kirim Laporan" else "Submit Report",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Grouped & Searchable Location Picker Modal Bottom Sheet
        if (showLocationPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLocationPickerSheet = false },
                sheetState = locationSheetState,
                containerColor = cardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isIndonesian) "Pilih Lokasi Ruangan" else "Select Room Location",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-Time Room Location Search Input Bar
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
                                if (locationSearchQuery.isEmpty()) {
                                    Text(
                                        text = if (isIndonesian) "Cari lokasi / ruangan... (contoh: Lab, Toilet, XII RPL)" else "Search location... (e.g. Lab, Toilet, XII RPL)",
                                        fontSize = 13.5.sp,
                                        color = textSecondary
                                    )
                                }
                                BasicTextField(
                                    value = locationSearchQuery,
                                    onValueChange = { locationSearchQuery = it },
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

                    // Grouped List of Rooms by Floor (Lantai 1 to 4)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locationGroups.forEach { group ->
                            val matchingRooms = group.rooms.filter {
                                it.contains(locationSearchQuery, ignoreCase = true)
                            }

                            if (matchingRooms.isNotEmpty()) {
                                item {
                                    // Section Floor Header Banner
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkMode) Color(0xFF334155) else Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = "🔹 ${group.floorName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF2563EB),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                items(matchingRooms) { room ->
                                    val isSelected = room == selectedLocation
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLocation = room
                                                showLocationPickerSheet = false
                                                locationSearchQuery = ""
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = room,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF2563EB) else textPrimary
                                            )

                                            if (isSelected) {
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
            }
        }

        // Photo Picker Modal Bottom Sheet
        if (showPhotoPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhotoPickerSheet = false },
                sheetState = photoSheetState,
                containerColor = cardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isIndonesian) "Pilih Sumber Foto" else "Select Photo Source",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                cameraLauncher.launch(null)
                            },
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
                            .clickable {
                                galleryLauncher.launch("image/*")
                            },
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

        if (zoomedImageBitmap != null) {
            ZoomableImageViewerDialog(
                imageBitmap = zoomedImageBitmap,
                title = zoomedImageTitle,
                subtitle = reportTitle.ifBlank { null },
                onDismiss = { zoomedImageBitmap = null }
            )
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
private fun CameraIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val color = Color(0xFF2563EB)

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
            color = color,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color = color, radius = 3.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.62f), style = Stroke(width = 1.8.dp.toPx()))
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateReportTracScreenPreview() {
    CreateReportTracScreen()
}
