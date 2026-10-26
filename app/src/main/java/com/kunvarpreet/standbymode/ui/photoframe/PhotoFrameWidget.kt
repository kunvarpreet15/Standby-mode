package com.kunvarpreet.standbymode.ui.photoframe

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.R
import com.kunvarpreet.standbymode.core.photoframe.AsyncPhotoLoader
import com.kunvarpreet.standbymode.core.photoframe.CropAlignment
import com.kunvarpreet.standbymode.core.photoframe.FaceBounds
import com.kunvarpreet.standbymode.core.photoframe.FaceCropAnalyzer
import com.kunvarpreet.standbymode.core.photoframe.PhotoFrameLogic
import com.kunvarpreet.standbymode.core.photoframe.PhotoItemState
import com.kunvarpreet.standbymode.core.photoframe.SmartCropCalculator
import com.kunvarpreet.standbymode.ui.theme.StandByBackground
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Compact built-in Photo Frame widget for Duo Layout panes.
 * Displays user-selected personal photos in an automatically advancing ambient slideshow
 * with on-device face-aware smart cropping.
 */
@Composable
fun PhotoFrameWidget(
    modifier: Modifier = Modifier,
    photoUris: List<String>,
    isSlideshowActive: Boolean,
    isNightMode: Boolean,
    accentColor: Color,
    onChoosePhotos: () -> Unit,
    onOpenFullscreen: (() -> Unit)? = null
) {
    if (photoUris.isEmpty()) {
        PhotoFrameEmptyState(
            isNightMode = isNightMode,
            accentColor = accentColor,
            onChoosePhotos = onChoosePhotos,
            modifier = modifier
        )
        return
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val validCount = photoUris.size

    // Clamp index whenever the photo list changes
    val clampedIndex = currentIndex.coerceIn(0, validCount - 1)
    if (clampedIndex != currentIndex) {
        currentIndex = clampedIndex
    }

    // Automatically advance slideshow when actively visible and more than one photo is available
    LaunchedEffect(isSlideshowActive, validCount) {
        if (isSlideshowActive && validCount > 1) {
            while (isActive) {
                delay(PhotoFrameLogic.SLIDESHOW_INTERVAL_MILLIS)
                currentIndex = PhotoFrameLogic.nextIndex(currentIndex, validCount)
            }
        }
    }

    val currentUriString = photoUris[currentIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .semantics {
                contentDescription = "Photo ${currentIndex + 1} of $validCount"
            },
        contentAlignment = Alignment.Center
    ) {
        // Subtle 400ms crossfade between photo transitions
        Crossfade(
            targetState = currentUriString,
            animationSpec = tween(durationMillis = 400),
            label = "photo_slideshow_crossfade"
        ) { uriString ->
            PhotoItemView(
                uriString = uriString,
                photoIndex = currentIndex,
                totalPhotos = validCount,
                isNightMode = isNightMode,
                accentColor = accentColor,
                onManagePhotos = onChoosePhotos,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (onOpenFullscreen != null) {
            Surface(
                onClick = onOpenFullscreen,
                shape = RoundedCornerShape(8.dp),
                color = if (isNightMode) StandByNightSurface else Color(0x66000000),
                border = BorderStroke(
                    1.dp,
                    if (isNightMode) StandByNightTextMuted.copy(alpha = 0.25f) else Color(0x33FFFFFF)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .semantics {
                        contentDescription = "Expand photo frame to fullscreen"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fullscreen),
                        contentDescription = null,
                        tint = if (isNightMode) StandByNightText else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual photo renderer with asynchronous loading, on-device face detection,
 * and adaptive smart crop alignment.
 */
@Composable
internal fun PhotoItemView(
    uriString: String,
    photoIndex: Int,
    totalPhotos: Int,
    isNightMode: Boolean,
    accentColor: Color,
    onManagePhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        val cachedPhoto = remember(uriString) { AsyncPhotoLoader.getCachedPhoto(uriString) }
        val cachedFaces = remember(uriString) { FaceCropAnalyzer.getCachedFaces(uriString) }

        var loadState by remember(uriString) {
            mutableStateOf(
                if (cachedPhoto != null) {
                    val initialAlignment = if (cachedFaces != null) {
                        SmartCropCalculator.calculateCropAlignment(
                            imageWidth = cachedPhoto.bitmap.width,
                            imageHeight = cachedPhoto.bitmap.height,
                            containerWidth = containerWidthPx,
                            containerHeight = containerHeightPx,
                            faces = cachedFaces
                        )
                    } else {
                        CropAlignment.Center
                    }
                    PhotoItemState.Success(cachedPhoto, initialAlignment)
                } else {
                    PhotoItemState.Loading
                }
            )
        }

        var detectedFaces by remember(uriString) {
            mutableStateOf<List<FaceBounds>?>(cachedFaces)
        }

        // 1. Asynchronously load photo off the main thread
        LaunchedEffect(uriString) {
            if (loadState !is PhotoItemState.Success) {
                val result = AsyncPhotoLoader.loadPhoto(context.contentResolver, uriString)
                loadState = result.fold(
                    onSuccess = { decoded ->
                        val faces = FaceCropAnalyzer.getCachedFaces(uriString)
                        detectedFaces = faces
                        val initialAlignment = if (faces != null) {
                            SmartCropCalculator.calculateCropAlignment(
                                imageWidth = decoded.bitmap.width,
                                imageHeight = decoded.bitmap.height,
                                containerWidth = containerWidthPx,
                                containerHeight = containerHeightPx,
                                faces = faces
                            )
                        } else {
                            CropAlignment.Center
                        }
                        PhotoItemState.Success(decoded, initialAlignment)
                    },
                    onFailure = { PhotoItemState.Unavailable(uriString, it.localizedMessage) }
                )
            }
        }

        // 2. Perform on-device face detection only when photo is successfully loaded and not yet analyzed
        val currentSuccess = loadState as? PhotoItemState.Success
        LaunchedEffect(uriString, currentSuccess?.decodedPhoto) {
            val photo = currentSuccess?.decodedPhoto ?: return@LaunchedEffect
            if (detectedFaces == null) {
                val faceResult = FaceCropAnalyzer.analyzeFaces(uriString, photo.bitmap)
                detectedFaces = faceResult.getOrDefault(emptyList())
            }
        }

        when (val state = loadState) {
            is PhotoItemState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StandByBackground),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = if (isNightMode) StandByNightTextMuted else accentColor,
                        strokeWidth = 2.5.dp
                    )
                }
            }
            is PhotoItemState.Success -> {
                // Dynamically resolve target crop alignment based on detected faces and active container size
                val targetAlignment = remember(
                    detectedFaces,
                    state.decodedPhoto,
                    containerWidthPx,
                    containerHeightPx
                ) {
                    if (detectedFaces != null) {
                        SmartCropCalculator.calculateCropAlignment(
                            imageWidth = state.decodedPhoto.bitmap.width,
                            imageHeight = state.decodedPhoto.bitmap.height,
                            containerWidth = containerWidthPx,
                            containerHeight = containerHeightPx,
                            faces = detectedFaces
                        )
                    } else {
                        state.cropAlignment
                    }
                }

                // Smoothly glide to smart crop coordinates in 300ms to eliminate jarring jumps or flicker
                val animatedBiasX by animateFloatAsState(
                    targetValue = targetAlignment.biasX,
                    animationSpec = tween(durationMillis = 300),
                    label = "smart_crop_bias_x"
                )
                val animatedBiasY by animateFloatAsState(
                    targetValue = targetAlignment.biasY,
                    animationSpec = tween(durationMillis = 300),
                    label = "smart_crop_bias_y"
                )

                // Photo is deliberately rendered in full natural color (never tinted red in Night Mode)
                Image(
                    bitmap = state.bitmap,
                    contentDescription = "Photo ${photoIndex + 1} of $totalPhotos",
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(animatedBiasX, animatedBiasY),
                    modifier = Modifier.fillMaxSize()
                )
            }
            is PhotoItemState.Unavailable -> {
                PhotoUnavailableState(
                    isNightMode = isNightMode,
                    accentColor = accentColor,
                    onManagePhotos = onManagePhotos,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Empty state displayed when Photo Frame is enabled but no photos have been chosen.
 */
@Composable
fun PhotoFrameEmptyState(
    isNightMode: Boolean,
    accentColor: Color,
    onChoosePhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .semantics {
                contentDescription = "Photo frame empty state. No photos selected."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_photo_frame),
                contentDescription = null,
                tint = if (isNightMode) StandByNightTextMuted else StandByTextMuted,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No Photos Selected",
                color = if (isNightMode) StandByNightText else StandByTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Select photos from your device to display in StandBy Mode",
                color = if (isNightMode) StandByNightTextMuted else StandByTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                onClick = onChoosePhotos,
                color = if (isNightMode) StandByNightSurface else accentColor,
                shape = RoundedCornerShape(20.dp),
                border = if (isNightMode) BorderStroke(1.dp, StandByNightText) else null,
                modifier = Modifier.semantics {
                    contentDescription = "Choose photos button"
                }
            ) {
                Text(
                    text = "Choose Photos",
                    color = if (isNightMode) StandByNightText else Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Recovery state displayed when a selected photo cannot be accessed or loaded.
 */
@Composable
fun PhotoUnavailableState(
    isNightMode: Boolean,
    accentColor: Color,
    onManagePhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .semantics {
                contentDescription = "Photo unavailable. Actionable recovery state."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_photo_broken),
                contentDescription = null,
                tint = if (isNightMode) StandByNightText else StandByTextMuted,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "Photo Unavailable",
                color = if (isNightMode) StandByNightText else StandByTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "This image could not be loaded. You can reselect or remove photos in Settings.",
                color = if (isNightMode) StandByNightTextMuted else StandByTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                onClick = onManagePhotos,
                color = if (isNightMode) StandByNightSurface else accentColor,
                shape = RoundedCornerShape(20.dp),
                border = if (isNightMode) BorderStroke(1.dp, StandByNightText) else null,
                modifier = Modifier.semantics {
                    contentDescription = "Manage photos button"
                }
            ) {
                Text(
                    text = "Manage Photos",
                    color = if (isNightMode) StandByNightText else Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
