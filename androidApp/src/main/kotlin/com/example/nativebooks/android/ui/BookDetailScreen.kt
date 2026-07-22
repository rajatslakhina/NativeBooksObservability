package com.example.nativebooks.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativebooks.model.Book
import com.example.nativebooks.observability.KmpSpanContext
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.NativeTracer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(book: Book, onBack: () -> Unit) {
    var traceContext by remember(book.id) { mutableStateOf<KmpSpanContext?>(null) }
    BackHandler(onBack = onBack)

    DisposableEffect(book.id) {
        val context = NativeTracer.startSpan(
            name = "screen.book.detail",
            attributes = mapOf(
                "screen.name" to "book-detail",
                "book.id" to book.id,
                "book.category" to book.category,
            ),
        )
        traceContext = context
        onDispose {
            NativeTracer.endSpan(
                context = context,
                attributes = mapOf("screen.disappeared" to "true"),
                status = KmpSpanStatus.OK,
            )
        }
    }

    Scaffold(
        containerColor = NativeBooksColors.Paper,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NativeBooksColors.Paper),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BookCover(
                category = book.category,
                title = book.title,
                modifier = Modifier.padding(top = 18.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        book.category.uppercase(),
                        color = NativeBooksColors.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(book.title, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "by ${book.author}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NativeBooksColors.SecondaryInk,
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = NativeBooksColors.Card),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BookStat(book.year.toString(), "Published", Modifier.weight(1f))
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(42.dp)
                                .background(NativeBooksColors.Line),
                        )
                        BookStat("${book.readingMinutes} min", "Reading time", Modifier.weight(1f))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("About this book", style = MaterialTheme.typography.titleLarge)
                    Text(
                        book.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = NativeBooksColors.SecondaryInk,
                    )
                }

                traceContext?.takeIf { it.isValid }?.let { context ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                NativeBooksColors.Accent.copy(alpha = 0.09f),
                                RoundedCornerShape(14.dp),
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NativeBooksColors.Accent,
                            )
                            Text(
                                "KMP → native span stitched",
                                modifier = Modifier.padding(start = 7.dp),
                                color = NativeBooksColors.Accent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            "Trace ${context.traceId.take(12)}… · Span ${context.spanId.take(8)}…",
                            color = NativeBooksColors.SecondaryInk,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp, color = NativeBooksColors.SecondaryInk)
    }
}
