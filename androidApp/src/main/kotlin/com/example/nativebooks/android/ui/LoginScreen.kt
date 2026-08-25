package com.example.nativebooks.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativebooks.android.observability.ObservabilityStatus
import com.example.nativebooks.auth.LoginValidator
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.NativeTracer

@Composable
fun LoginScreen(
    observabilityStatus: ObservabilityStatus,
    onLogin: (String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("reader@example.com") }
    var password by rememberSaveable { mutableStateOf("books123") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        val validation = LoginValidator().validate(email, password)
        val context = NativeTracer.startSpan(
            name = "login.submit",
            attributes = mapOf(
                "app.feature" to "login",
                "login.valid" to validation.isValid.toString(),
            ),
        )
        NativeTracer.endSpan(
            context = context,
            attributes = mapOf("kmp.context.received" to context.isValid.toString()),
            status = if (validation.isValid) KmpSpanStatus.OK else KmpSpanStatus.ERROR,
        )

        if (validation.isValid) {
            validationMessage = null
            focusManager.clearFocus()
            onLogin(email)
        } else {
            validationMessage = validation.message
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeBooksColors.Paper),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-115).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(NativeBooksColors.Accent.copy(alpha = 0.12f)),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(NativeBooksColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                    )
                }
                Text(
                    text = "NATIVE BOOKS",
                    color = NativeBooksColors.Ink,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Your next good idea\nstarts here.",
                    style = MaterialTheme.typography.displaySmall,
                    color = NativeBooksColors.Ink,
                )
                Text(
                    text = "Sign in to open your small, thoughtfully curated library.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NativeBooksColors.SecondaryInk,
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NativeBooksColors.Card),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        placeholder = { Text("reader@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        colors = inputColors(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        placeholder = { Text("At least 6 characters") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                        colors = inputColors(),
                        shape = RoundedCornerShape(14.dp),
                    )

                    validationMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Button(
                        onClick = { submit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("login.button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NativeBooksColors.Accent),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Open my library", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = NativeBooksColors.Accent)
                Text(
                    text = "New Relic monitoring is active. ${observabilityStatus.destination}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NativeBooksColors.SecondaryInk,
                )
            }
        }
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NativeBooksColors.Accent,
    unfocusedBorderColor = NativeBooksColors.Line,
    focusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f),
    unfocusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f),
)
