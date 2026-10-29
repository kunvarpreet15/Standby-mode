package com.kunvarpreet.standbymode.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.kunvarpreet.standbymode.MainActivity
import com.kunvarpreet.standbymode.core.dream.DreamPolicy

/**
 * Lightweight trampoline activity referenced by standby_dream_metadata.xml.
 * When the user taps the settings gear icon next to StandByMode in Android's
 * system screensaver settings, this activity launches MainActivity directly into
 * the Settings screen.
 */
class DreamSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(DreamPolicy.EXTRA_TARGET_SCREEN, DreamPolicy.SCREEN_SETTINGS)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }
}
