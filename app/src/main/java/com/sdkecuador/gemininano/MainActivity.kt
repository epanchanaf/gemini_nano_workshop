package com.sdkecuador.gemininano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.sdkecuador.gemininano.ui.nav.AppNavigation
import com.sdkecuador.gemininano.ui.theme.AppTheme
import com.sdkecuador.gemininano.ui.theme.JournalThemeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: JournalThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val palette by themeViewModel.palette.collectAsStateWithLifecycle()
            AppTheme(palette = palette) {
                AppNavigation()
            }
        }
    }
}
