package com.example.tether.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tether.R
import com.example.tether.ui.common.pressScale
import com.example.tether.ui.common.rememberReducedMotion
import com.example.tether.ui.theme.AlertContainer
import com.example.tether.ui.theme.AlertOnContainer
import com.example.tether.ui.theme.AlertRed
import com.example.tether.ui.theme.SafeContainer
import com.example.tether.ui.theme.SafeGreen

@Composable
fun HomeScreen(
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val reducedMotion = rememberReducedMotion()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Why: the status change is a state indication, a calm low-stiffness spring
        // keeps it trustworthy instead of an abrupt color snap.
        val statusSpring = spring<Color>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
        val containerColor by animateColorAsState(
            targetValue = if (state.isConnected) SafeContainer else AlertContainer,
            animationSpec = statusSpring,
            label = "statusContainer"
        )
        val contentColor by animateColorAsState(
            targetValue = if (state.isConnected) SafeGreen else AlertOnContainer,
            animationSpec = statusSpring,
            label = "statusContent"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            // Why: the icon and copy swap is a state change, entering content starts at
            // 0.95 scale with a fade (never from scale 0) and the exit is faster than the
            // entry so the system feels responsive. Reduced motion keeps only the fade.
            AnimatedContent(
                targetState = state.isConnected,
                transitionSpec = {
                    val enter = if (reducedMotion) {
                        fadeIn(tween(220))
                    } else {
                        fadeIn(tween(220)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    val exit = if (reducedMotion) {
                        fadeOut(tween(120))
                    } else {
                        fadeOut(tween(120)) + scaleOut(targetScale = 0.97f, animationSpec = tween(120))
                    }
                    enter togetherWith exit
                },
                label = "statusSwap"
            ) { connected ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (connected) SafeGreen else AlertRed,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(if (connected) R.string.status_connected else R.string.status_disconnected),
                        style = MaterialTheme.typography.headlineMedium,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(if (connected) R.string.status_connected_desc else R.string.status_disconnected_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.device_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.pairedDevice?.name ?: stringResource(R.string.no_device_paired),
                    style = MaterialTheme.typography.titleLarge
                )
                state.pairedDevice?.let {
                    Text(
                        text = stringResource(R.string.last_sync_label, it.lastSyncTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Why: press feedback confirms the tap registered on the app's primary action.
        val testAlertInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = { viewModel.sendTestAlert() },
            interactionSource = testAlertInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pressScale(testAlertInteraction),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.btn_test_alert), style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.test_alert_desc),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickAccessCard(
                title = stringResource(R.string.qa_contacts_title),
                subtitle = stringResource(R.string.qa_contacts_desc),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToContacts
            )
            QuickAccessCard(
                title = stringResource(R.string.qa_settings_title),
                subtitle = stringResource(R.string.qa_settings_desc),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Why: press feedback confirms the tap registered on a navigation card.
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}
