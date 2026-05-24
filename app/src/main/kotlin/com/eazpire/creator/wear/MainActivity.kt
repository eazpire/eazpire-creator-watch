package com.eazpire.creator.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.wear.auth.bootstrapAuthFromPhone
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tokenStore: SecureTokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenStore = SecureTokenStore(this)
        lifecycleScope.launch {
            bootstrapAuthFromPhone(this@MainActivity, tokenStore)
        }
        setContent {
            WearEazTheme {
                WearApp(tokenStore = tokenStore)
            }
        }
    }
}
