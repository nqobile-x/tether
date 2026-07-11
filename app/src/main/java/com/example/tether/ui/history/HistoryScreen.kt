package com.example.tether.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.tether.ui.data.AlertEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val reducedMotion = rememberReducedMotion()
    var selectedAlert by remember { mutableStateOf<AlertEvent?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Why: empty states fade in with a slight upward drift instead of popping.
            AnimatedVisibility(
                visible = state.alerts.isEmpty(),
                enter = if (reducedMotion) {
                    fadeIn(tween(220))
                } else {
                    fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 20 }
                },
                exit = fadeOut(tween(120)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.empty_history_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.empty_history_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.alerts.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Why: staggered entrance reads as a list arriving, not flooding in,
                    // and animateItem keeps newly logged alerts sliding in smoothly.
                    itemsIndexed(state.alerts, key = { _, a -> a.id }) { index, alert ->
                        AlertCard(
                            alert = alert,
                            onClick = { selectedAlert = alert },
                            onToggleFalsePositive = { viewModel.toggleFalsePositive(alert.id) },
                            modifier = Modifier
                                .animateItem()
                                .staggeredEntrance(index)
                        )
                    }
                }
            }
        }

        selectedAlert?.let { alert ->
            AlertDialog(
                onDismissRequest = { selectedAlert = null },
                title = { Text(stringResource(R.string.alert_detail_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(alert.classification, style = MaterialTheme.typography.titleLarge)
                        Text(alert.timestamp, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.label_location), style = MaterialTheme.typography.labelLarge)
                        Text(alert.locationCoordinates, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.label_notified), style = MaterialTheme.typography.labelLarge)
                        Text(alert.notifiedContacts.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedAlert = null }) {
                        Text(stringResource(R.string.btn_close))
                    }
                }
            )
        }
    }
}

@Composable
private fun AlertCard(
    alert: AlertEvent,
    onClick: () -> Unit,
    onToggleFalsePositive: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Why: press feedback confirms the tap registered on a row that opens details.
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(alert.classification, style = MaterialTheme.typography.titleLarge)
                if (alert.isFalsePositive) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondary) {
                        Text(
                            stringResource(R.string.label_false_positive),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(alert.timestamp, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onToggleFalsePositive,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.btn_mark_false_positive))
            }
        }
    }
}
