package com.example.nativebooks.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nativebooks.android.observability.ObservabilityStatus

@Composable
fun TelemetryDialog(
    observabilityStatus: ObservabilityStatus,
    kmpSpanEndAcknowledged: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NativeBooksColors.Card),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("Observability", style = MaterialTheme.typography.headlineSmall)
                StatusRow(
                    icon = { Icon(Icons.Default.Memory, contentDescription = null) },
                    title = "Native module",
                    detail = "New Relic Android 7.8.1",
                )
                StatusRow(
                    icon = { Icon(Icons.Default.Timeline, contentDescription = null) },
                    title = "KMP round trip",
                    detail = if (kmpSpanEndAcknowledged) {
                        "Native span end acknowledged"
                    } else {
                        "Waiting for a KMP request"
                    },
                )
                StatusRow(
                    icon = {
                        Icon(
                            if (observabilityStatus.exportsToNewRelic) Icons.Default.Cloud else Icons.Default.Terminal,
                            contentDescription = null,
                        )
                    },
                    title = "Trace destination",
                    detail = observabilityStatus.destination,
                )

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    icon: @Composable () -> Unit,
    title: String,
    detail: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides NativeBooksColors.Accent,
        ) { icon() }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = NativeBooksColors.SecondaryInk)
        }
    }
}
