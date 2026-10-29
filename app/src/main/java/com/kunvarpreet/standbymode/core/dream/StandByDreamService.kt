package com.kunvarpreet.standbymode.core.dream

import android.service.dreams.DreamService
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kunvarpreet.standbymode.core.preferences.PreferencesRepository
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.ui.dream.DreamClockContent

/**
 * Native Android DreamService integration allowing StandByMode to operate as
 * the official system screensaver (Daydream).
 *
 * Implements [LifecycleOwner], [SavedStateRegistryOwner], and [ViewModelStoreOwner]
 * to host Jetpack Compose within a native Service window with complete lifecycle safety.
 */
class StandByDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private lateinit var preferencesRepository: PreferencesRepository
    private var composeView: ComposeView? = null
    private var isDreamingState by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        preferencesRepository = PreferencesRepository(applicationContext)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Configure ambient dream window
        isFullscreen = DreamPolicy.DEFAULT_IS_FULLSCREEN
        isInteractive = DreamPolicy.DEFAULT_IS_INTERACTIVE

        val decor = window?.decorView ?: return
        decor.setViewTreeLifecycleOwner(this)
        decor.setViewTreeSavedStateRegistryOwner(this)
        decor.setViewTreeViewModelStoreOwner(this)

        val newComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@StandByDreamService))
            setContent {
                val preferences by preferencesRepository.preferencesFlow.collectAsState(
                    initial = UserClockPreferences()
                )

                // Adjust dream screen brightness in response to Night Mode
                setScreenBright(DreamPolicy.resolveScreenBrightness(preferences.isNightModeEnabled))

                DreamClockContent(
                    preferences = preferences,
                    isDreaming = isDreamingState
                )
            }
        }

        composeView = newComposeView
        setContentView(newComposeView)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        isDreamingState = true
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        isDreamingState = false
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        composeView?.disposeComposition()
        composeView = null
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.disposeComposition()
        composeView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
