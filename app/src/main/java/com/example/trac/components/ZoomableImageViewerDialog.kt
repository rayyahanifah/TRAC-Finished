package com.example.trac.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.trac.util.ImageUtils
import kotlin.math.roundToInt

/**
 * Universal Zoomable Image Viewer Dialog for TRAC App.
 *
 * Supports:
 * - Pinch-to-zoom (1.0x - 5.0x)
 * - Pan / Drag when zoomed in with edge-bound constraints
 * - Double tap to toggle zoom (1x <-> 2.5x)
 * - Dedicated On-screen Zoom In / Zoom Out / Reset controls (+, -, ↺)
 * - Zoom percentage indicator badge (e.g., 100%, 250%)
 * - Dark immersive background with sleek glassmorphism styling
 * - Accepts both [ImageBitmap], raw [android.graphics.Bitmap], or [base64] string
 */
@Composable
fun ZoomableImageViewerDialog(
    imageBitmap: ImageBitmap? = null,
    base64Image: String? = null,
    title: String = "Pratinjau Foto",
    subtitle: String? = null,
    onDismiss: () -> Unit
) {
    val resolvedBitmap: ImageBitmap? = remember(imageBitmap, base64Image) {
        imageBitmap ?: base64Image?.let {
            if (it.isNotBlank()) ImageUtils.base64ToBitmap(it)?.asImageBitmap() else null
        }
    }

    if (resolvedBitmap == null) {
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF00A0E17))
                .onSizeChanged { containerSize = it }
        ) {
            // Main Zoomable & Pannable Image Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
                    .pointerInput(containerSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val targetScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = targetScale

                            if (targetScale > 1f) {
                                val maxOffsetX = ((containerSize.width * (targetScale - 1f)) / 2f).coerceAtLeast(0f)
                                val maxOffsetY = ((containerSize.height * (targetScale - 1f)) / 2f).coerceAtLeast(0f)
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = resolvedBitmap,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 80.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }

            // Top Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color(0xCC0F172A),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Cubit layar atau ketuk 2x untuk perbesar foto",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Close Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        color = Color(0x33FFFFFF),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Floating Controls (Zoom Controls & Level)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                color = Color(0xE61E293B),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zoom Out Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                val newScale = (scale - 0.5f).coerceAtLeast(1f)
                                scale = newScale
                                if (newScale <= 1f) {
                                    offset = Offset.Zero
                                } else {
                                    val maxOffsetX = ((containerSize.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                    val maxOffsetY = ((containerSize.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                    offset = Offset(
                                        x = offset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                        y = offset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                }
                            },
                        color = if (scale > 1f) Color(0x33FFFFFF) else Color(0x15FFFFFF),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "－",
                                color = if (scale > 1f) Color.White else Color(0xFF64748B),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Zoom Indicator / Reset Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                scale = 1f
                                offset = Offset.Zero
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${(scale * 100).roundToInt()}%",
                                color = if (scale > 1.05f) Color(0xFF60A5FA) else Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (scale > 1.05f) {
                                Text(
                                    text = "↺",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Zoom In Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                val newScale = (scale + 0.5f).coerceAtMost(5f)
                                scale = newScale
                            },
                        color = if (scale < 5f) Color(0x33FFFFFF) else Color(0x15FFFFFF),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "＋",
                                color = if (scale < 5f) Color.White else Color(0xFF64748B),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
