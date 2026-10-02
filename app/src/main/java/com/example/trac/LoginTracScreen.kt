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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trac.components.InAppBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginTracScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLoginClick: (email: String, pass: String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var emailOrStudentId by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Staggered entrance animation states
    val headerAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val headerOffsetY = remember { Animatable(if (isPreview) 0f else -35f) }

    val emailAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val emailOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }

    val passAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val passOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }

    val buttonAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val buttonOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }

    val footerAlpha = remember { Animatable(if (isPreview) 1f else 0f) }

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

            delay(120)
            launch {
                emailAlpha.animateTo(1f, animationSpec = tween(400))
            }
            launch {
                emailOffsetY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            delay(100)
            launch {
                passAlpha.animateTo(1f, animationSpec = tween(400))
            }
            launch {
                passOffsetY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            delay(100)
            launch {
                buttonAlpha.animateTo(1f, animationSpec = tween(400))
            }
            launch {
                buttonOffsetY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            delay(100)
            launch {
                footerAlpha.animateTo(1f, animationSpec = tween(400))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 180.dp.toPx(),
                center = Offset(-size.width * 0.05f, size.height * 0.15f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 170.dp.toPx(),
                center = Offset(size.width * 1.05f, size.height * 0.12f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = headerAlpha.value
                            translationY = headerOffsetY.value.dp.toPx()
                        }
                ) {
                    ShieldIconSmall()

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "LOGIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Welcome back!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Sign in to continue.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                InAppBanner(
                    message = errorMessage,
                    isError = true
                )

                // Form Field 1: Email / Student ID
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = emailAlpha.value
                            translationY = emailOffsetY.value.dp.toPx()
                        }
                ) {
                    Text(
                        text = "Email Address",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TracTextField(
                        value = emailOrStudentId,
                        onValueChange = { emailOrStudentId = it },
                        placeholder = "enter your email address...",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Form Field 2: Password
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = passAlpha.value
                            translationY = passOffsetY.value.dp.toPx()
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        Text(
                            text = "Forgot password?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.clickable { onForgotPasswordClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TracTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        isPassword = true,
                        isPasswordVisible = isPasswordVisible,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            onLoginClick(emailOrStudentId, password)
                        }),
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        onLoginClick(emailOrStudentId, password)
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer {
                            alpha = buttonAlpha.value
                            translationY = buttonOffsetY.value.dp.toPx()
                        }
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
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Footer Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .graphicsLayer {
                        alpha = footerAlpha.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp
                    )
                    Text(
                        text = "or",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New here? ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Register",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable { onRegisterClick() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TracTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    isDarkMode: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onTogglePasswordVisibility: () -> Unit = {}
) {
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = Color(0xFF94A3B8)
    val borderCol = if (isDarkMode) Color(0xFF334155) else Color(0xFFEFF3F8)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x10000000),
                ambientColor = Color(0x06000000)
            ),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Normal
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isPassword) {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = Modifier.size(24.dp)
                ) {
                    EyeIcon(isVisible = isPasswordVisible)
                }
            }
        }
    }
}

@Composable
fun EyeIcon(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        val eyePath = Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.15f, w * 0.9f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.85f, w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(
            path = eyePath,
            color = Color(0xFF64748B),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = Color(0xFF64748B),
            radius = 3.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.5f)
        )

        if (!isVisible) {
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(w * 0.2f, h * 0.2f),
                end = Offset(w * 0.8f, h * 0.8f),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ShieldIconSmall(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x352563EB),
                ambientColor = Color(0x152563EB)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2563EB),
                        Color(0xFF1D4ED8)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
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
            drawPath(
                path = shieldPath,
                color = Color.White,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            val checkPath = Path().apply {
                moveTo(w * 0.37f, h * 0.48f)
                lineTo(w * 0.47f, h * 0.58f)
                lineTo(w * 0.63f, h * 0.39f)
            }
            drawPath(
                path = checkPath,
                color = Color.White,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginTracScreenPreview() {
    LoginTracScreen()
}
