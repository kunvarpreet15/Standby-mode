package com.kunvarpreet.standbymode.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByBorderInput
import com.kunvarpreet.standbymode.ui.theme.StandBySurface
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Settings section card container with uppercase title header.
 */
@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = StandByTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StandBySurfaceCard,
            border = BorderStroke(1.dp, StandByBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * C09: ClockFaceSelector
 * Segmented control for choosing between Bold Digital, Analog, and Retro Flip.
 */
@Composable
fun ClockFaceSelector(
    selectedFace: ClockFaceType,
    accentColor: Color,
    onSelectFace: (ClockFaceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ClockFaceType.entries.forEach { face ->
            val isSelected = (face == selectedFace)
            val backgroundColor = if (isSelected) accentColor else StandBySurface
            val textColor = if (isSelected) Color.Black else StandByTextPrimary
            val borderStroke = if (isSelected) null else BorderStroke(1.dp, StandByBorder)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor,
                border = borderStroke,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.RadioButton) { onSelectFace(face) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = face.displayName,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

/**
 * C10: AccentColorPicker
 * Horizontal row of circular swatches with checkmark and focus ring for the active color.
 */
@Composable
fun AccentColorPicker(
    selectedColor: AccentColorOption,
    onSelectColor: (AccentColorOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentColorOption.entries.forEach { option ->
            val isSelected = (option == selectedColor)

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.RadioButton) { onSelectColor(option) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // Outer focus ring
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(2.dp, option.color, CircleShape)
                    )
                }

                // Inner circular swatch
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(option.color),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "${option.displayName} selected",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive toggle row with title, optional subtitle, and accessible Switch.
 */
@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    accentColor: Color
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                color = if (enabled) StandByTextPrimary else StandByTextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = StandByTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = StandByTextMuted,
                uncheckedTrackColor = StandBySurface,
                uncheckedBorderColor = StandByBorderInput,
                disabledCheckedTrackColor = accentColor.copy(alpha = 0.4f),
                disabledUncheckedTrackColor = StandBySurface.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * Settings controls for Photo Frame: displays selection count, choose/manage button,
 * and remove all photos action.
 */
@Composable
fun PhotoFrameSettingsControls(
    modifier: Modifier = Modifier,
    photoCount: Int,
    accentColor: Color,
    onChoosePhotos: () -> Unit,
    onRemoveAllPhotos: () -> Unit,
    onOpenFullscreen: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val countText = if (photoCount == 0) {
            "No photos selected"
        } else {
            "$photoCount photo${if (photoCount == 1) "" else "s"} selected (up to 50)"
        }

        Text(
            text = countText,
            color = StandByTextMuted,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onChoosePhotos,
                color = accentColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (photoCount == 0) "Choose Photos" else "Manage Photos",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            if (photoCount > 0) {
                Surface(
                    onClick = onRemoveAllPhotos,
                    color = StandBySurface,
                    border = BorderStroke(1.dp, Color(0xFFFF453A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Remove All",
                            color = Color(0xFFFF453A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        if (onOpenFullscreen != null && photoCount > 0) {
            Surface(
                onClick = onOpenFullscreen,
                color = StandBySurface,
                border = BorderStroke(1.dp, accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View Fullscreen Slideshow",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
