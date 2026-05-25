package com.eazpire.creator.wear

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.wear.auth.WearAuthListenerService
import com.eazpire.creator.wear.auth.bootstrapAuthFromPhone
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tokenStore: SecureTokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepInteractiveForWear()
        tokenStore = SecureTokenStore(this)
        setContent {
            WearEazTheme {
                WearApp(tokenStore = tokenStore)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        keepInteractiveForWear()
        if (!::tokenStore.isInitialized) return
        lifecycleScope.launch {
            bootstrapAuthFromPhone(this@MainActivity, tokenStore)
            if (tokenStore.isLoggedIn()) {
                sendBroadcast(
                    android.content.Intent(WearAuthListenerService.ACTION_AUTH_CHANGED)
                        .setPackage(packageName),
                )
            }
        }
    }

    /** Prevent immediate ambient / watch-face takeover on Wear emulators while testing. */
    private fun keepInteractiveForWear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
