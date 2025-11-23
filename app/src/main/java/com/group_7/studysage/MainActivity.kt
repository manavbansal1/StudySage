package com.group_7.studysage

import android.Manifest
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.navigation.StudySageNavigation
import com.group_7.studysage.ui.theme.StudySageTheme
import com.group_7.studysage.viewmodels.AuthViewModel
import com.group_7.studysage.viewmodels.AuthViewModelFactory
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.group_7.studysage.utils.PermissionHandler
import com.group_7.studysage.utils.NotificationPermissionHelper
import com.group_7.studysage.utils.StudySageNotificationManager
import com.group_7.studysage.utils.ReminderScheduler
import com.group_7.studysage.services.NfcHostApduService
import com.group_7.studysage.data.nfc.NFCPayload
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // Observable states
    var nfcDataReceived = mutableStateOf<NFCPayload?>(null)
    var nfcWriteSuccess = mutableStateOf(false)

    var isInSendMode = mutableStateOf(false)
    var isInReceiveMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        Log.d("NFC_MAIN", "NFC Available: ${nfcAdapter != null}")
        Log.d("NFC_MAIN", "NFC Enabled: ${nfcAdapter?.isEnabled}")

        // Initialize notification manager
        StudySageNotificationManager.init(this)

        // Schedule daily study reminders (9 AM by default)
        ReminderScheduler.scheduleDailyReminder(this, hourOfDay = 9)

        PermissionHandler.init(this)
        PermissionHandler.requestAllPermissions(this)

        // Update daily streak when app opens (non-blocking, background operation)
        lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                authRepository.updateDailyStreak()
            } catch (e: Exception) {
                // Silently catch - streak update failure shouldn't crash app
                Log.e("MainActivity", "Failed to update daily streak: ${e.message}")
            }
        }

        // Set up HCE callback
        NfcHostApduService.onDataReceived = { data ->
            runOnUiThread {
                Log.d("NFC_MAIN", "Data received via HCE callback")
                // Handle received data
                // You can parse it here if needed
            }
        }

        setContent {
            StudySageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudySageApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("NFC_MAIN", "onResume - Send: ${isInSendMode.value}, Receive: ${isInReceiveMode.value}")

        nfcAdapter?.let { adapter ->
            if (isInReceiveMode.value) {
                enableReaderMode(adapter)
            }
            // HCE is always active when app is in foreground
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun enableReaderMode(adapter: NfcAdapter) {
        Log.d("NFC_MAIN", "Enabling Reader Mode")

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        adapter.enableReaderMode(this, { tag ->
            Log.d("NFC_MAIN", "Tag discovered in reader mode")
            handleTagInReceiveMode(tag)
        }, flags, null)
    }

    private fun handleTagInReceiveMode(tag: Tag) {
        Log.d("NFC_MAIN", "Processing tag in receive mode")

        val nfcManager = com.group_7.studysage.utils.NFCManager(this)
        val payload = nfcManager.readDataFromTag(tag)

        if (payload != null) {
            Log.d("NFC_MAIN", "Successfully read payload: ${payload.noteTitle}")
            runOnUiThread {
                nfcDataReceived.value = payload
            }
        } else {
            Log.e("NFC_MAIN", "Failed to read payload")
        }
    }

    fun enterSendMode() {
        Log.d("NFC_MAIN", "Entering SEND mode (HCE)")
        isInSendMode.value = true
        isInReceiveMode.value = false

        // Data is already prepared in NfcHostApduService
        // HCE will handle the rest
    }

    fun enterReceiveMode() {
        Log.d("NFC_MAIN", "Entering RECEIVE mode (Reader)")
        isInSendMode.value = false
        isInReceiveMode.value = true

        nfcAdapter?.let { enableReaderMode(it) }
    }

    fun exitNfcMode() {
        Log.d("NFC_MAIN", "Exiting NFC mode")
        isInSendMode.value = false
        isInReceiveMode.value = false

        nfcAdapter?.disableReaderMode(this)

        val nfcManager = com.group_7.studysage.utils.NFCManager(this)
        nfcManager.clearSendingData()
    }

    fun consumeNfcData(): NFCPayload? {
        val data = nfcDataReceived.value
        nfcDataReceived.value = null
        return data
    }

    override fun onDestroy() {
        super.onDestroy()
        NfcHostApduService.onDataReceived = null
        NfcHostApduService.dataToSend = null
    }
}


@Composable
fun StudySageApp() {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    // Permission launcher for notifications (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
            Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
            Log.d("NotificationPermission", "Permission GRANTED")
        } else {
            // Permission denied
            Toast.makeText(context, "Notifications disabled", Toast.LENGTH_SHORT).show()
            Log.d("NotificationPermission", "Permission DENIED")
        }
    }

    // Request permission on launch if needed
    LaunchedEffect(Unit) {
        Log.d("NotificationPermission", "LaunchedEffect triggered")
        Log.d("NotificationPermission", "Android version: ${Build.VERSION.SDK_INT}")
        Log.d("NotificationPermission", "TIRAMISU: ${Build.VERSION_CODES.TIRAMISU}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = NotificationPermissionHelper.isNotificationPermissionGranted(context)
            Log.d("NotificationPermission", "Permission already granted: $isGranted")

            if (!isGranted) {
                Log.d("NotificationPermission", "Requesting permission...")
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("NotificationPermission", "Permission already granted, skipping request")
            }
        } else {
            Log.d("NotificationPermission", "Android version < 13, no permission needed")
        }
    }

    StudySageNavigation(
        authViewModel = authViewModel,
        modifier = Modifier
    )
}