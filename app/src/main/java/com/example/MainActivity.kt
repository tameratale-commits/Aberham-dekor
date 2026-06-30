package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            
            AbrahamDecorTheme(themeStyle = currentTheme) {
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
