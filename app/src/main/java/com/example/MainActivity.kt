package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BrightnessScreen
import com.example.ui.BrightnessViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrightnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                DisposableEffect(this) {
                    viewModel.bindActivityProvider { this@MainActivity }
                    onDispose {
                        viewModel.unbindActivityProvider()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    BrightnessScreen(
                        uiState = uiState,
                        onSliderChange = viewModel::onSliderValueChanged,
                        onNumericInputChange = viewModel::onNumericInputChanged,
                        onApplyNumericInput = viewModel::applyNumericInput,
                        onSelectPreset = viewModel::applyPreset,
                        onAdjustByDelta = viewModel::adjustBrightnessBy,
                        onToggleVoiceListening = viewModel::toggleVoiceListening,
                        onExecuteVoicePhrase = viewModel::executeSpokenPhrase,
                        onToggleTts = viewModel::toggleTtsEnabled,
                        onRefreshSystemState = viewModel::refreshSystemState,
                        onOpenWriteSettings = {
                            try {
                                startActivity(viewModel.buildManageWriteSettingsIntent())
                            } catch (_: Exception) {
                            }
                        },
                        onAudioPermissionChanged = viewModel::updateRecordAudioPermissionStatus
                    )
                }
            }
        }
    }
}
