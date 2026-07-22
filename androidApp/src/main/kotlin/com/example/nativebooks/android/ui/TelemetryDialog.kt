package com.example.nativebooks.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nativebooks.android.observability.ObservabilityStatus
import com.example.nativebooks.android.observability.ObservabilitySystem
import com.example.nativebooks.observability.KmpSpanContext
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.NativeTracer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TelemetryDialog(
    observabilityStatus: ObservabilityStatus,
    kmpSpanEndAcknowledged: Boolean,
    onDismiss: () -> Unit,
) {
    var lastContext by remember { mutableStateOf<KmpSpanContext?>(null) }
    var lastEndAcknowledged by remember { mutableStateOf<Boolean?>(null) }
    var flushAcknowledged by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

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
                    detail = "OpenTelemetry Java 1.62.0",
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

                Button(
                    onClick = {
                        val context = NativeTracer.startSpan(
                            name = "debug.manual-span",
                            attributes = mapOf("debug.trigger" to "telemetry-dialog"),
                        )
                        lastContext = context
                        lastEndAcknowledged = NativeTracer.endSpan(
                            context = context,
                            attributes = mapOf(
                                "debug.completed" to "true",
                                "kmp.context.received" to context.isValid.toString(),
                            ),
                            status = KmpSpanStatus.OK,
                        )
                        scope.launch {
                            flushAcknowledged = withContext(Dispatchers.IO) {
                                ObservabilitySystem.forceFlush()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create test span")
                }

                lastContext?.let { context ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("LAST TEST CONTEXT", fontWeight = FontWeight.Bold)
                        Text("traceId  ${context.traceId}", fontFamily = FontFamily.Monospace)
                        Text("spanId   ${context.spanId}", fontFamily = FontFamily.Monospace)
                        Text("sampled  ${context.sampled}", fontFamily = FontFamily.Monospace)
                        Text("ended   ${if (lastEndAcknowledged == true) "acknowledged" else "not acknowledged"}")
                        flushAcknowledged?.let { Text("flushed  $it") }
                    }
                }

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
