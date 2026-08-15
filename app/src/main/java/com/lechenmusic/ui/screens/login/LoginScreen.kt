package com.lechenmusic.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: MainViewModel, onLoginSuccess: () -> Unit) {
    var serverUrl by remember { mutableStateOf("http://j.tthsdd.top:3334") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onLoginSuccess()
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val primaryColor = Color(0xFF6C63FF)
    val accentColor = Color(0xFFFF6584)
    val bgDark = Color(0xFF0D0D1A)
    val cardBg = Color(0xFF1A1A2E)

    // Entry animations
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.7f) }
    val formAlpha = remember { Animatable(0f) }
    val formOffset = remember { Animatable(60f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(500))
        logoScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f))
        formAlpha.animateTo(1f, animationSpec = tween(500, delayMillis = 200))
        formOffset.animateTo(0f, animationSpec = tween(500, delayMillis = 200, easing = FastOutSlowInEasing))
    }

    // Pulsing animation for login button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val buttonGlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "glow"
    )

    Scaffold(
        containerColor = bgDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgDark)
                .padding(innerPadding)
        ) {
            // Subtle gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                // Logo
                Box(
                    modifier = Modifier
                        .alpha(logoAlpha.value)
                        .scale(logoScale.value),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryColor, accentColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎵", fontSize = 36.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Welcome text
                Text(
                    "欢迎回来",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.alpha(logoAlpha.value)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "登录您的悦音账号",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.alpha(logoAlpha.value)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Form card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(formAlpha.value)
                        .offset(y = formOffset.value.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Server URL
                        Text(
                            "服务器地址",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            placeholder = {
                                Text(
                                    "http://your-server:3334",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Link, null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                cursorColor = primaryColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Username
                        Text(
                            "用户名",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = {
                                Text(
                                    "请输入用户名",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person, null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                cursorColor = primaryColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password
                        Text(
                            "密码",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = {
                                Text(
                                    "请输入密码",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock, null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                        viewModel.login(serverUrl, username, password)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                cursorColor = primaryColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Error message
                        AnimatedVisibility(
                            visible = loginError != null,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            if (loginError != null) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFF4757).copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = loginError!!,
                                        color = Color(0xFFFF6B81),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Login button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.login(serverUrl, username, password)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                disabledContainerColor = primaryColor.copy(alpha = 0.3f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp
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
                                    "立即登录",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Terms
                Text(
                    "登录即代表您已同意 服务协议 和 隐私政策",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.25f),
                    textAlign = TextAlign.Center
                )

                // Extra space for keyboard
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
