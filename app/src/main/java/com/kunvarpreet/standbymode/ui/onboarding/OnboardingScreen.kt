package com.kunvarpreet.standbymode.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kunvarpreet.standbymode.core.onboarding.OnboardingPolicy
import com.kunvarpreet.standbymode.core.onboarding.OnboardingStep
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.ui.theme.StandByAccentGreen
import com.kunvarpreet.standbymode.ui.theme.StandByBackground
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * First-run onboarding wizard and permission walkthrough screen.
 * Guides the user through app orientation, auto-activation triggers, display policies,
 * and optional runtime permissions without demanding unnecessary access.
 */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    preferences: UserClockPreferences,
    onToggleAutoActivate: (Boolean) -> Unit,
    onToggleKeepScreenAwake: (Boolean) -> Unit,
    onCompleteOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = preferences.accentColor.color

    var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentStep = remember(currentStepIndex) { OnboardingStep.fromIndex(currentStepIndex) }

    // Live permission state tracking with automatic refresh on resume
    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var hasCalendarPermission by remember {
        mutableStateOf(checkCalendarPermission(context))
    }
    var hasMediaAccess by remember {
        mutableStateOf(checkMediaListenerAccess(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission(context)
                hasCalendarPermission = checkCalendarPermission(context)
                hasMediaAccess = checkMediaListenerAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission request launchers
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalendarPermission = granted
    }

    // Context-aware Back navigation
    BackHandler {
        if (OnboardingPolicy.canNavigateBack(currentStep)) {
            val prev = OnboardingPolicy.getPreviousStep(currentStep)
            if (prev != null) currentStepIndex = prev.stepIndex
        } else {
            onDismiss()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .safeDrawingPadding()
    ) {
        val isLandscape = maxWidth > maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isLandscape) 32.dp else 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEP ${currentStep.stepIndex + 1} OF ${OnboardingStep.TOTAL_STEPS}",
                    color = StandByTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )

                if (!OnboardingPolicy.isFinalStep(currentStep)) {
                    OutlinedButton(
                        onClick = onCompleteOnboarding,
                        border = BorderStroke(0.5.dp, StandByBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Skip",
                            color = StandByTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "onboarding_step_content"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        when (step) {
                            OnboardingStep.WELCOME -> {
                                WelcomeStepContent(accentColor = accentColor)
                            }
                            OnboardingStep.CHARGING_ORIENTATION -> {
                                ChargingOrientationStepContent(
                                    accentColor = accentColor,
                                    autoActivateEnabled = preferences.autoActivateOnChargeAndLandscape,
                                    onToggleAutoActivate = onToggleAutoActivate
                                )
                            }
                            OnboardingStep.DISPLAY_RULES -> {
                                DisplayRulesStepContent(
                                    accentColor = accentColor,
                                    keepAwakeEnabled = preferences.keepScreenAwake,
                                    onToggleKeepAwake = onToggleKeepScreenAwake
                                )
                            }
                            OnboardingStep.PERMISSIONS_INTEGRATIONS -> {
                                PermissionsIntegrationsStepContent(
                                    context = context,
                                    accentColor = accentColor,
                                    hasNotificationPermission = hasNotificationPermission,
                                    hasCalendarPermission = hasCalendarPermission,
                                    hasMediaAccess = hasMediaAccess,
                                    onRequestNotificationPermission = {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    onRequestCalendarPermission = {
                                        calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                                    },
                                    onOpenMediaAccessSettings = {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            OnboardingStep.READY -> {
                                ReadyStepContent(accentColor = accentColor)
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Footer (Dots + Back + Next/Get Started)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step Indicator Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until OnboardingStep.TOTAL_STEPS) {
                        val isSelected = i == currentStep.stepIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accentColor else StandByBorder)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (OnboardingPolicy.canNavigateBack(currentStep)) {
                        OutlinedButton(
                            onClick = {
                                val prev = OnboardingPolicy.getPreviousStep(currentStep)
                                if (prev != null) currentStepIndex = prev.stepIndex
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = "Back",
                                color = StandByTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    if (OnboardingPolicy.isFinalStep(currentStep)) {
                        Button(
                            onClick = onCompleteOnboarding,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .semantics { contentDescription = "Get Started button" }
                        ) {
                            Text(
                                text = "Get Started",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val next = OnboardingPolicy.getNextStep(currentStep)
                                if (next != null) currentStepIndex = next.stepIndex
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .semantics { contentDescription = "Next button" }
                        ) {
                            Text(
                                text = "Next",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Step 1: Welcome ---

@Composable
private fun WelcomeStepContent(accentColor: Color) {
    Text(
        text = "Welcome to StandByMode",
        color = StandByTextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Turn your Android phone into an elegant, glanceable bedside smart display.",
        color = StandByTextMuted,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    FeatureSummaryCard(
        title = "3 Glanceable Display Spaces",
        description = "Swipe horizontally between Duo split widget panels, Fullscreen Clocks, and Photo Frame slideshows.",
        accentColor = accentColor
    )
    Spacer(modifier = Modifier.height(10.dp))
    FeatureSummaryCard(
        title = "Curated Clock Faces",
        description = "Choose from Bold Digital, classic Retro Flip, and analog watch dials with custom colors.",
        accentColor = accentColor
    )
    Spacer(modifier = Modifier.height(10.dp))
    FeatureSummaryCard(
        title = "True Black OLED Efficiency",
        description = "Full #000000 background for power-saving bedside display with sub-pixel burn-in protection.",
        accentColor = accentColor
    )
}

// --- Step 2: Charging & Orientation ---

@Composable
private fun ChargingOrientationStepContent(
    accentColor: Color,
    autoActivateEnabled: Boolean,
    onToggleAutoActivate: (Boolean) -> Unit
) {
    Text(
        text = "Charging & Orientation",
        color = StandByTextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "StandByMode is optimized for horizontal bedside docks, stands, and charging cables.",
        color = StandByTextMuted,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = StandBySurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, StandByBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-activate in landscape while charging",
                        color = StandByTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Activates StandByMode when connected to power horizontally; dismisses when picked up upright or unplugged.",
                        color = StandByTextMuted,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = autoActivateEnabled,
                    onCheckedChange = onToggleAutoActivate,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161616), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Android Notice: Under modern Android security restrictions, when StandByMode is in the background, power connection alerts you with a notification that you tap to open.",
                    color = StandByTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// --- Step 3: Screen & Night Mode ---

@Composable
private fun DisplayRulesStepContent(
    accentColor: Color,
    keepAwakeEnabled: Boolean,
    onToggleKeepAwake: (Boolean) -> Unit
) {
    Text(
        text = "Screen & Night Mode",
        color = StandByTextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Ensure your clock stays visible on your desk or nightstand while preserving battery.",
        color = StandByTextMuted,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = StandBySurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, StandByBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep Screen Awake while charging",
                    color = StandByTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Prevents screen timeout while charging. Automatically clears when unplugged to avoid draining your battery.",
                    color = StandByTextMuted,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = keepAwakeEnabled,
                onCheckedChange = onToggleKeepAwake,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    FeatureSummaryCard(
        title = "Bedside Red Night Mode",
        description = "Turn on Night Mode anytime in Settings for a monochromatic soft red glow that won't ruin your sleep or dark-adapted eyes.",
        accentColor = StandByNightText
    )
}

// --- Step 4: Optional Features & Permissions ---

@Composable
private fun PermissionsIntegrationsStepContent(
    context: Context,
    accentColor: Color,
    hasNotificationPermission: Boolean,
    hasCalendarPermission: Boolean,
    hasMediaAccess: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onOpenMediaAccessSettings: () -> Unit
) {
    Text(
        text = "Optional Features",
        color = StandByTextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Enable extra integrations if desired. None of these permissions are required for the core clock.",
        color = StandByTextMuted,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(16.dp))

    // 1. Notification Permission (Android 13+)
    PermissionRowCard(
        title = "Charging Notifications",
        description = "Allows non-disruptive notifications when power connects so you can tap to enter StandByMode.",
        isGranted = hasNotificationPermission,
        actionLabel = "Allow",
        accentColor = accentColor,
        onAction = onRequestNotificationPermission
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 2. Calendar Permission
    PermissionRowCard(
        title = "Calendar & Agenda Widget",
        description = "Display upcoming events and a month grid directly in your Duo Layout widget stack.",
        isGranted = hasCalendarPermission,
        actionLabel = "Allow",
        accentColor = accentColor,
        onAction = onRequestCalendarPermission
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 3. Media Listener Access
    PermissionRowCard(
        title = "Universal Media Player",
        description = "Show track info and playback controls on your bedside display for Spotify, YouTube Music, and podcasts.",
        isGranted = hasMediaAccess,
        actionLabel = "Enable in Settings",
        accentColor = accentColor,
        onAction = onOpenMediaAccessSettings
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 4. Photo Frame
    Card(
        colors = CardDefaults.cardColors(containerColor = StandBySurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, StandByBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photo Frame Slideshow",
                    color = StandByTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ready (No Permission)",
                    color = StandByAccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Uses Android's system photo picker. You grant access only to photos you pick, with zero full-storage permissions.",
                color = StandByTextMuted,
                fontSize = 13.sp
            )
        }
    }
}

// --- Step 5: Ready ---

@Composable
private fun ReadyStepContent(accentColor: Color) {
    Text(
        text = "You're All Set!",
        color = StandByTextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "StandByMode is ready for your nightstand or desk.",
        color = StandByTextMuted,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    FeatureSummaryCard(
        title = "Horizontal Swipe Navigation",
        description = "Swipe left or right anytime to navigate between Duo Layout, Fullscreen Clock, and Photo Frame.",
        accentColor = accentColor
    )
    Spacer(modifier = Modifier.height(10.dp))
    FeatureSummaryCard(
        title = "Vertical Widget Flipping",
        description = "Swipe vertically on either Duo widget pane to cycle through your clocks, weather, calendar, and battery stacks.",
        accentColor = accentColor
    )
    Spacer(modifier = Modifier.height(10.dp))
    FeatureSummaryCard(
        title = "Settings Anytime",
        description = "Tap the gear icon in the top-right corner to change clock faces, colors, 24-hour format, or review permissions.",
        accentColor = accentColor
    )
}

// --- Helper UI Components ---

@Composable
private fun FeatureSummaryCard(
    title: String,
    description: String,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StandBySurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, StandByBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = accentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = StandByTextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    accentColor: Color,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StandBySurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, StandByBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = StandByTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isGranted) {
                    Text(
                        text = "Granted",
                        color = StandByAccentGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    OutlinedButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Text(
                text = description,
                color = StandByTextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// --- Permission Check Helpers ---

private fun checkNotificationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//
//    } else {
//        NotificationManagerCompat.from(context).areNotificationsEnabled()
//    }
}

private fun checkCalendarPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkMediaListenerAccess(context: Context): Boolean {
    val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabledListeners.contains(context.packageName)
}
