package com.example.futurediary

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.futurediary.ui.screen.DiaryNavHost
import com.example.futurediary.ui.theme.FutureDiaryTheme
import com.example.futurediary.ui.util.SecurityManager
import dagger.hilt.android.AndroidEntryPoint

import androidx.activity.viewModels
import androidx.work.*
import com.example.futurediary.data.sync.SyncWorker
import com.example.futurediary.ui.viewmodel.SecurityViewModel
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var sharedLink by mutableStateOf<String?>(null)
    private val securityViewModel: SecurityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        scheduleSync()
        
        enableEdgeToEdge()

        setContent {
            FutureDiaryTheme {
                val securityManager = remember { SecurityManager(this) }
                val isUnlocked by securityViewModel.isUnlocked.collectAsState()
                var authError by remember { mutableStateOf<String?>(null) }
                
                // If biometric is disabled, consider it unlocked
                LaunchedEffect(Unit) {
                    if (!securityManager.isBiometricEnabled) {
                        securityViewModel.setUnlocked(true)
                    }
                }
                
                if (!isUnlocked) {
                    // Show a secure screen while authenticating
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Journal Locked",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            if (authError != null) {
                                Text(
                                    text = authError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                                Button(onClick = {
                                    authError = null
                                    securityManager.showBiometricPrompt(
                                        activity = this@MainActivity,
                                        onSuccess = { securityViewModel.setUnlocked(true) },
                                        onError = { authError = it }
                                    )
                                }) {
                                    Text("Try Again")
                                }
                            }
                        }

                        LaunchedEffect(Unit) {
                            if (securityManager.isBiometricEnabled) {
                                securityManager.showBiometricPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = { securityViewModel.setUnlocked(true) },
                                    onError = { authError = it }
                                )
                            }
                        }
                    }
                } else {
                    DiaryNavHost(sharedLink = sharedLink, onSharedLinkConsumed = { sharedLink = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                // PIPELINE: Extract the first URL found in the text, 
                // regardless of what music app prefix is there.
                val urlPattern = Regex("(https?://\\S+)")
                val match = urlPattern.find(text)
                if (match != null) {
                    sharedLink = match.value
                }
            }
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "diary_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
