package com.example.tether

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.tether.data.TetherRepository
import com.example.tether.service.TetherMonitorService
import com.example.tether.ui.TetherApp
import com.example.tether.ui.theme.TetherTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            TetherRepository.refreshPermissions()
            maybeStartMonitoring()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TetherApp()

            // Rationale shown before the system permission dialogs, per project convention:
            // the user should understand exactly why each sensitive permission is needed.
            var pending by remember { mutableStateOf(missingPermissions()) }
            if (pending.isNotEmpty()) {
                TetherTheme {
                    AlertDialog(
                        onDismissRequest = { pending = emptyList() },
                        title = { Text(stringResource(R.string.perm_rationale_title)) },
                        text = { Text(stringResource(R.string.perm_rationale_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                requestPermissions.launch(pending.toTypedArray())
                                pending = emptyList()
                            }) { Text(stringResource(R.string.btn_continue)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pending = emptyList() }) {
                                Text(stringResource(R.string.btn_not_now))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TetherRepository.refreshPermissions()
        maybeStartMonitoring()
    }

    private fun missingPermissions(): List<String> {
        val wanted = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            wanted += Manifest.permission.BLUETOOTH_SCAN
            wanted += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wanted += Manifest.permission.POST_NOTIFICATIONS
        }
        return wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun maybeStartMonitoring() {
        val bluetoothReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        if (!bluetoothReady) return
        if (!TetherRepository.settingsStore.settings.value.backgroundMonitoringEnabled) return

        CoroutineScope(Dispatchers.IO).launch {
            if (com.example.tether.data.db.TetherDatabase.get(applicationContext)
                    .pairedDeviceDao().getOnce() != null
            ) {
                TetherMonitorService.start(applicationContext)
            }
        }
    }
}
