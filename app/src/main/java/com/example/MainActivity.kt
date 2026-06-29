package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.AbrahamDecorTheme
import com.example.ui.viewmodel.BusinessViewModel
import com.example.ui.viewmodel.BusinessViewModelFactory

class MainActivity : ComponentActivity() {
    
    private val viewModel: BusinessViewModel by viewModels {
        BusinessViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AbrahamDecorTheme {
                MainDashboard(viewModel = viewModel)
            }
        }
    }
}
