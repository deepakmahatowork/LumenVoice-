package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.LumenGold
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberBright
import com.example.ui.theme.StatusEmerald

@Composable
fun BrightnessScreen(
    uiState: BrightnessUiState,
    onSliderChange: (Float, Boolean) -> Unit,
    onNumericInputChange: (String) -> Unit,
    onApplyNumericInput: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onAdjustByDelta: (Int) -> Unit,
    onToggleVoiceListening: () -> Unit,
    onExecuteVoicePhrase: (String) -> Unit,
    onToggleTts: () -> Unit,
    onRefreshSystemState: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onAudioPermissionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onAudioPermissionChanged(isGranted)
        if (isGranted) {
            onToggleVoiceListening()
        }
    }

    // Automatically refresh system brightness & WRITE_SETTINGS permission when returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasAudio = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                onAudioPermissionChanged(hasAudio)
                onRefreshSystemState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Subtle ambient top glow reactive to current brightness percentage
        val glowAlpha by animateFloatAsState(
            targetValue = 0.08f + (uiState.brightnessPercent / 100f) * 0.26f,
            animationSpec = tween(durationMillis = 350),
            label = "ambientGlowAlpha"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.95f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SolarAmber.copy(alpha = glowAlpha),
                        AcousticCyan.copy(alpha = glowAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.14f),
                    radius = radius
                ),
                radius = radius,
                center = Offset(size.width * 0.5f, size.height * 0.14f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar with App Title, Offline Badge, and TTS / Sync Controls
                TopHeaderBar(
                    isTtsEnabled = uiState.isTtsEnabled,
                    onToggleTts = onToggleTts,
                    onRefresh = onRefreshSystemState
                )

                // Prominent Banner for Settings.ACTION_MANAGE_WRITE_SETTINGS when canWrite is false
                WriteSettingsPermissionSection(
                    canWriteSettings = uiState.canWriteSettings,
                    onOpenWriteSettings = onOpenWriteSettings
                )

                // Hero Luminous Gauge & 0-255 Telemetry Card
                LumenTelemetryHeroCard(
                    brightnessPercent = uiState.brightnessPercent,
                    systemBrightness255 = uiState.systemBrightness255,
                    isManualMode = uiState.isManualMode,
                    canWriteSettings = uiState.canWriteSettings
                )

                // Offline Voice Control Hub (Large Mic Button + Live Transcript + Spoken Confirmation)
                OfflineVoiceControlCard(
                    uiState = uiState,
                    onMicClick = {
                        val hasAudio = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasAudio) {
                            onToggleVoiceListening()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onRequestMicPermission = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onExecuteVoicePhrase = onExecuteVoicePhrase
                )

                // Core Manual Controls: 0-100% Slider, Quick Presets (25%, 43%, 75%, 100%), and Numeric Input
                CoreBrightnessControlsCard(
                    brightnessPercent = uiState.brightnessPercent,
                    numericInputText = uiState.numericInputText,
                    numericInputError = uiState.numericInputError,
                    onSliderChange = onSliderChange,
                    onNumericInputChange = onNumericInputChange,
                    onApplyNumericInput = onApplyNumericInput,
                    onSelectPreset = onSelectPreset,
                    onAdjustByDelta = onAdjustByDelta
                )

                // Command Activity Log
                if (uiState.commandHistory.isNotEmpty()) {
                    CommandHistoryCard(history = uiState.commandHistory)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    isTtsEnabled: Boolean,
    onToggleTts: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BrightnessHigh,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.subtitle_offline_badge),
                style = MaterialTheme.typography.labelSmall,
                color = AcousticCyan
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalIconButton(
                onClick = onToggleTts,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("tts_toggle_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isTtsEnabled) {
                        SolarAmber.copy(alpha = 0.18f)
                    } else {
                        SlateSurfaceVariant
                    },
                    contentColor = if (isTtsEnabled) SolarAmberBright else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isTtsEnabled) {
                        Icons.AutoMirrored.Filled.VolumeUp
                    } else {
                        Icons.AutoMirrored.Filled.VolumeOff
                    },
                    contentDescription = stringResource(R.string.tts_mute_toggle_desc)
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("refresh_brightness_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh_system_state_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WriteSettingsPermissionSection(
    canWriteSettings: Boolean,
    onOpenWriteSettings: () -> Unit
) {
    if (!canWriteSettings) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("write_settings_warning_card"),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A121D)
            ),
            border = BorderStroke(1.5.dp, AlertCoral.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = SolarAmberBright,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.permission_write_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.permission_write_settings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                )

                Button(
                    onClick = onOpenWriteSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("grant_write_settings_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarAmber,
                        contentColor = Color(0xFF1A1100)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_grant_write_settings),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("write_settings_granted_banner"),
            shape = MaterialTheme.shapes.medium,
            color = StatusEmerald.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = StatusEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.status_write_settings_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onOpenWriteSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("manage_write_settings_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.btn_grant_write_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LumenTelemetryHeroCard(
    brightnessPercent: Int,
    systemBrightness255: Int,
    isManualMode: Boolean,
    canWriteSettings: Boolean
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (brightnessPercent.coerceIn(0, 100) / 100f) * 260f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "brightnessArcSweep"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("brightness_telemetry_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.7f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Custom generated abstract lightwave backdrop with dynamic brightness opacity
            Image(
                painter = painterResource(id = R.drawable.img_lumen_backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.22f + (brightnessPercent / 100f) * 0.32f,
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.large)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MidnightSurface.copy(alpha = 0.55f),
                                MidnightSurface.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Radial Solar Arc Meter
                    Box(
                        modifier = Modifier.size(136.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 12.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                (size.width - diameter) / 2f,
                                (size.height - diameter) / 2f
                            )
                            val arcSize = Size(diameter, diameter)

                            // Track arc
                            drawArc(
                                color = SlateSurfaceVariant,
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Active luminous arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        AcousticCyan,
                                        SolarAmber,
                                        LumenGold,
                                        SolarAmberBright
                                    )
                                ),
                                startAngle = 140f,
                                sweepAngle = animatedSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when {
                                    brightnessPercent < 30 -> Icons.Filled.BrightnessLow
                                    brightnessPercent < 75 -> Icons.Filled.BrightnessMedium
                                    else -> Icons.Filled.BrightnessHigh
                                },
                                contentDescription = null,
                                tint = SolarAmberBright,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "$brightnessPercent%",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("current_brightness_percent_text")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Telemetry Readout Column (0-255 integer scale & Mode)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ObsidianBackground.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_system_scale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$systemBrightness255 / 255",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SolarAmberBright,
                                    modifier = Modifier.testTag("current_brightness_255_text")
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ObsidianBackground.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_brightness_mode),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isManualMode || canWriteSettings) {
                                        stringResource(R.string.mode_manual)
                                    } else {
                                        stringResource(R.string.mode_adaptive)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isManualMode || canWriteSettings) {
                                        AcousticCyan
                                    } else {
                                        SolarAmber
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflineVoiceControlCard(
    uiState: BrightnessUiState,
    onMicClick: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onExecuteVoicePhrase: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val micButtonContainerColor by animateColorAsState(
        targetValue = if (uiState.isListening) AlertCoral else SolarAmber,
        animationSpec = tween(250),
        label = "micButtonColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_control_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(
            width = if (uiState.isListening) 1.5.dp else 1.dp,
            color = if (uiState.isListening) AcousticCyan else SlateBorder.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = AcousticCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.voice_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = AcousticCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AcousticCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (uiState.isListening) "LISTENING" else "LOCAL ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AcousticCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Large Microphone Button with Live Acoustic Pulse Rings
            Box(
                modifier = Modifier
                    .size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isListening) {
                    val rmsBoost = (uiState.rmsDb / 12f).coerceIn(0f, 1f) * 0.25f
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseRadius = size.minDimension * 0.36f
                        drawCircle(
                            color = AcousticCyan.copy(alpha = 0.18f),
                            radius = baseRadius * (pulseScale + rmsBoost)
                        )
                        drawCircle(
                            color = SolarAmber.copy(alpha = 0.26f),
                            radius = baseRadius * (1.05f + rmsBoost * 0.7f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Button(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(92.dp)
                        .testTag("voice_mic_button")
                        .semantics {
                            contentDescription = if (uiState.isListening) {
                                "Stop listening for voice command"
                            } else {
                                "Start listening for voice brightness command"
                            }
                        },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = micButtonContainerColor,
                        contentColor = if (uiState.isListening) Color.White else Color(0xFF1A1100)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = if (uiState.isListening) {
                    stringResource(R.string.mic_button_listening)
                } else {
                    stringResource(R.string.mic_button_tap_to_speak)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (uiState.isListening) AcousticCyan else SolarAmberBright,
                textAlign = TextAlign.Center
            )

            Text(
                text = uiState.voiceStatusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // If microphone permission is not yet granted, provide an explicit inline action chip too
            if (!uiState.hasRecordAudioPermission) {
                OutlinedButton(
                    onClick = onRequestMicPermission,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("request_mic_permission_button"),
                    border = BorderStroke(1.dp, AcousticCyan.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = AcousticCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_grant_mic_permission),
                        color = AcousticCyan
                    )
                }
            }

            // Live partial transcript or last spoken confirmation feedback
            AnimatedVisibility(
                visible = uiState.partialTranscript.isNotBlank() ||
                    uiState.lastRecognizedPhrase.isNotBlank() ||
                    uiState.lastSpokenConfirmation.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_feedback_banner"),
                    shape = MaterialTheme.shapes.medium,
                    color = ObsidianBackground.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activePhrase = if (uiState.isListening && uiState.partialTranscript.isNotBlank()) {
                            uiState.partialTranscript
                        } else {
                            uiState.lastRecognizedPhrase
                        }

                        if (activePhrase.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.last_voice_command_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AcousticCyan
                                )
                                if (uiState.lastMatchedRule.isNotBlank()) {
                                    Text(
                                        text = uiState.lastMatchedRule,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarAmberBright
                                    )
                                }
                            }
                            Text(
                                text = "\"$activePhrase\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("recognized_phrase_text")
                            )
                        }

                        if (uiState.lastSpokenConfirmation.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = StatusEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TTS: \"${uiState.lastSpokenConfirmation}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusEmerald,
                                    modifier = Modifier.testTag("tts_confirmation_text")
                                )
                            }
                        }
                    }
                }
            }

            // Voice error alert if speech recognition encountered an issue
            AnimatedVisibility(visible = uiState.voiceErrorMessage != null) {
                uiState.voiceErrorMessage?.let { errorMsg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = AlertCoral.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, AlertCoral.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))

            // Supported Voice Phrases Guide + Quick Tap-to-Test Phrase Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.voice_commands_cheatsheet_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.voice_hint_exact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    text = stringResource(R.string.voice_hint_increase),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    text = stringResource(R.string.voice_hint_decrease),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    text = stringResource(R.string.voice_hint_extremes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Interactive voice phrase chips to test the local Kotlin parser & TTS directly
                val sampleVoicePhrases = listOf(
                    "Set brightness to 43 percent" to "voice_chip_43",
                    "Brighter" to "voice_chip_brighter",
                    "Dimmer" to "voice_chip_dimmer",
                    "Max" to "voice_chip_max",
                    "Minimum" to "voice_chip_minimum"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleVoicePhrases.forEach { (phrase, tag) ->
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable { onExecuteVoicePhrase(phrase) }
                                .testTag(tag),
                            shape = RoundedCornerShape(50),
                            color = SlateSurfaceVariant.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = SolarAmberBright,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "\"$phrase\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreBrightnessControlsCard(
    brightnessPercent: Int,
    numericInputText: String,
    numericInputError: String?,
    onSliderChange: (Float, Boolean) -> Unit,
    onNumericInputChange: (String) -> Unit,
    onApplyNumericInput: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onAdjustByDelta: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("core_controls_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. 0%–100% Slider with -15% / +15% buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.slider_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "0% — 100%",
                        style = MaterialTheme.typography.labelMedium,
                        color = SolarAmberBright
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onAdjustByDelta(-15) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("decrease_15_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = SlateSurfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease brightness by 15 percent"
                        )
                    }

                    Slider(
                        value = brightnessPercent.toFloat(),
                        onValueChange = { newValue ->
                            onSliderChange(newValue, false)
                        },
                        onValueChangeFinished = {
                            onSliderChange(brightnessPercent.toFloat(), true)
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("brightness_slider")
                            .semantics {
                                contentDescription = "Screen brightness percentage slider"
                            },
                        colors = SliderDefaults.colors(
                            thumbColor = SolarAmberBright,
                            activeTrackColor = SolarAmber,
                            inactiveTrackColor = SlateSurfaceVariant
                        )
                    )

                    FilledTonalIconButton(
                        onClick = { onAdjustByDelta(15) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("increase_15_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = SlateSurfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase brightness by 15 percent"
                        )
                    }
                }
            }

            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))

            // 2. Quick Preset Buttons (25%, 43%, 75%, 100%)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.presets_section_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val presets = listOf(25, 43, 75, 100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presets.forEach { presetValue ->
                        val isSelected = brightnessPercent == presetValue
                        FilledTonalButton(
                            onClick = { onSelectPreset(presetValue) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("preset_${presetValue}_button"),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSelected) SolarAmber else SlateSurfaceVariant,
                                contentColor = if (isSelected) Color(0xFF1A1100) else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) SolarAmberBright else SlateBorder
                            )
                        ) {
                            Text(
                                text = "$presetValue%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))

            // 3. Numeric Text Field (0–100) + Apply Button
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = numericInputText,
                        onValueChange = onNumericInputChange,
                        label = {
                            Text(text = stringResource(R.string.numeric_input_label))
                        },
                        suffix = {
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.labelLarge,
                                color = SolarAmberBright
                            )
                        },
                        singleLine = true,
                        isError = numericInputError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onApplyNumericInput()
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("brightness_numeric_input"),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolarAmber,
                            unfocusedBorderColor = SlateBorder,
                            focusedLabelColor = SolarAmberBright,
                            cursorColor = SolarAmber
                        )
                    )

                    Button(
                        onClick = {
                            onApplyNumericInput()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .padding(top = 4.dp)
                            .testTag("apply_numeric_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarAmber,
                            contentColor = Color(0xFF1A1100)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_apply_numeric),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                if (numericInputError != null) {
                    Text(
                        text = numericInputError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AlertCoral,
                        modifier = Modifier.testTag("numeric_input_error_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandHistoryCard(
    history: List<CommandLogEntry>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("command_history_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "RECENT BRIGHTNESS EVENTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            history.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = SlateBorder.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = entry.sourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = AcousticCyan
                            )
                            Text(
                                text = entry.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = entry.inputDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${entry.targetPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarAmberBright
                        )
                        Text(
                            text = "${entry.targetSystem255}/255",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
