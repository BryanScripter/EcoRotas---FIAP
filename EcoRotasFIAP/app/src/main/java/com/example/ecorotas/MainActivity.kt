package com.example.ecorotas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ecorotas.navigation.EcoRotasNav
import com.example.ecorotas.ui.theme.EcoRotasFIAPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoRotasFIAPTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EcoRotasNav()
                }
            }
        }
    }
}
