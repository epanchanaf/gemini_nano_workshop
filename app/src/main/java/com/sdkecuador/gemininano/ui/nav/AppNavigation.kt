package com.sdkecuador.gemininano.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sdkecuador.gemininano.ui.home.HomeScreen
import com.sdkecuador.gemininano.ui.home.chat.ChatScreen
import com.sdkecuador.gemininano.ui.home.imagedesc.ImageDescScreen
import com.sdkecuador.gemininano.ui.home.proofread.ProofReadingScreen
import com.sdkecuador.gemininano.ui.home.rewrite.RewriteScreen
import com.sdkecuador.gemininano.ui.home.summarization.SummarizationScreen
import com.sdkecuador.gemininano.ui.home.speech.SpeechRecognitionScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigate = { feature -> navController.navigate(feature.route) })
        }

        composable(Feature.Summarization.route) {
            SummarizationScreen(onBack = { navController.navigateUp() })
        }

        composable(Feature.Proofreading.route) {
            ProofReadingScreen(onBack = { navController.navigateUp() })
        }

        composable(Feature.Rewrite.route) {
            RewriteScreen(onBack = { navController.navigateUp() })
        }

        composable(Feature.ImageDescription.route) {
            ImageDescScreen(onBack = { navController.navigateUp() })
        }

        composable(Feature.Chat.route) {
            ChatScreen(onBack = { navController.navigateUp() })
        }

        composable(Feature.SpeechRecognition.route) {
            SpeechRecognitionScreen(onBack = { navController.navigateUp() })
        }
    }
}
