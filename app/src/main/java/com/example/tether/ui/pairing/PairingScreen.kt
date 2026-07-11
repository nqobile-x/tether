package com.example.tether.ui.pairing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tether.R
import com.example.tether.ui.common.pressScale
import com.example.tether.ui.common.rememberReducedMotion
import com.example.tether.ui.common.staggeredEntrance
import com.example.tether.ui.data.PairedDevice
import com.example.tether.ui.theme.SafeContainer
import com.example.tether.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(viewModel: PairingViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val reducedMotion = rememberReducedMotion()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.pairing_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Why: the scan indicator and the rescan button swap is a state change, a quick
            // crossfade prevents a jarring jump, with the exit faster than the entry.
            AnimatedContent(
                targetState = state.isScanning,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "scanState"
            ) { scanning ->
                if (scanning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.scanning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Why: press feedback confirms the tap registered.
                    val scanInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.scan() },
                        interactionSource = scanInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(scanInteraction)
                    ) {
                        Text(stringResource(R.string.btn_scan_again))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                // Why: empty states fade in with a slight upward drift instead of popping,
                // nothing in a real interface appears out of nothing.
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.devices.isEmpty() && !state.isScanning,
                    enter = if (reducedMotion) {
                        fadeIn(tween(220))
                    } else {
                        fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 20 }
                    },
                    exit = fadeOut(tween(120)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.empty_devices_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.empty_devices_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.devices.isNotEmpty()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Why: staggered entrance (40ms per item, capped) reads as a list
                        // arriving, not flooding in, and animateItem keeps reorders smooth.
                        itemsIndexed(state.devices, key = { _, d -> d.id }) { index, device ->
                            DeviceCard(
                                device = device,
                                onPairClick = { viewModel.pairDevice(device) },
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredEntrance(index)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: PairedDevice,
    onPairClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected) SafeContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(device.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (device.isConnected) stringResource(R.string.paired_badge) else stringResource(R.string.tap_to_pair),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (device.isConnected) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!device.isConnected) {
                // Why: press feedback confirms the tap registered on the pairing action.
                val pairInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = onPairClick,
                    interactionSource = pairInteraction,
                    modifier = Modifier.pressScale(pairInteraction)
                ) {
                    Text("Pair")
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SafeGreen
                ) {
                    Text(
                        stringResource(R.string.paired_badge),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
