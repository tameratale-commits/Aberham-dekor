package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.AbrahamDecorTheme
import com.example.ui.theme.AppThemeStyle
import com.example.ui.viewmodel.BusinessViewModel
import com.example.ui.viewmodel.BusinessViewModelFactory

class MainActivity : ComponentActivity() {
    
    private val viewModel: BusinessViewModel by viewModels {
        BusinessViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPrefs = getSharedPreferences("abraham_decor_prefs", Context.MODE_PRIVATE)
        
        setContent {
            val savedThemeName = sharedPrefs.getString("theme_style", AppThemeStyle.CLASSIC_GOLD.name)
            var currentTheme by remember { 
                mutableStateOf(
                    try {
                        AppThemeStyle.valueOf(savedThemeName ?: AppThemeStyle.CLASSIC_GOLD.name)
                    } catch (e: Exception) {
                        AppThemeStyle.CLASSIC_GOLD
                    }
                )
            }

            var isUnlocked by remember { 
                mutableStateOf(sharedPrefs.getBoolean("is_app_unlocked", false)) 
            }
            
            AbrahamDecorTheme(themeStyle = currentTheme) {
                if (!isUnlocked) {
                    AppLockScreen(
                        onUnlockSuccess = {
                            isUnlocked = true
                            sharedPrefs.edit().putBoolean("is_app_unlocked", true).apply()
                        }
                    )
                } else {
                    MainDashboard(
                        viewModel = viewModel,
                        currentTheme = currentTheme,
                        onThemeChange = { newTheme ->
                            currentTheme = newTheme
                            sharedPrefs.edit().putString("theme_style", newTheme.name).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep Slate
                        Color(0xFF020617)  // Ultra Dark Slate
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Lock Icon
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF00F0FF).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00F0FF)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand / Title
            Text(
                text = "የይለፍ ቃል ማረጋገጫ",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "የሒሳብ መዝገብ አያያዝ መተግበሪያ",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Password Input Box
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (isError) isError = false
                },
                label = { Text("እባክዎ የይለፍ ቃል ያስገቡ", color = Color.White.copy(alpha = 0.6f)) },
                placeholder = { Text("የይለፍ ቃል", color = Color.White.copy(alpha = 0.4f)) },
                singleLine = true,
                isError = isError,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00F0FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFF00F0FF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = Color(0xFFFF5252),
                    errorLabelColor = Color(0xFFFF5252),
                    errorTextColor = Color.White
                ),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ያስገቡት የይለፍ ቃል የተሳሳተ ነው! እባክዎ እንደገና ይሞክሩ።",
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (password == "tameratale6576") {
                        onUnlockSuccess()
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F0FF),
                    contentColor = Color(0xFF020617)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "ግባ (Verify & Enter)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
