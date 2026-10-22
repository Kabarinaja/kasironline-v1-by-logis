package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainAppContainer
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PinAuthScreen
import com.example.ui.theme.KasirDuniaTheme
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.PosViewModelFactory

class MainActivity : ComponentActivity() {

    private val app by lazy { application as KasirDuniaApp }

    private val viewModel: PosViewModel by viewModels {
        PosViewModelFactory(
            repository = app.repository,
            preferences = app.preferences,
            printerManager = app.printerManager,
            pdfReportGenerator = app.pdfReportGenerator,
            backupManager = app.backupManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasirDuniaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KasirDuniaAppContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun KasirDuniaAppContent(viewModel: PosViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        // Step 1: User needs to authenticate or set up PIN
        !uiState.isAuthenticated -> {
            PinAuthScreen(
                viewModel = viewModel,
                uiState = uiState,
                onSuccess = {}
            )
        }
        // Step 2: First-time onboarding business profile
        !uiState.isOnboarded -> {
            OnboardingScreen(
                viewModel = viewModel,
                uiState = uiState,
                onComplete = {}
            )
        }
        // Step 3: Main POS Application
        else -> {
            MainAppContainer(
                viewModel = viewModel,
                uiState = uiState
            )
        }
    }
}
