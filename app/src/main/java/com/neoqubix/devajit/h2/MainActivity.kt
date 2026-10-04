package com.neoqubix.devajit.h2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neoqubix.devajit.h2.presentation.navigation.AppRoot
import com.neoqubix.devajit.h2.ui.theme.H2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            H2Theme {
                AppRoot()
            }
        }
    }
}
