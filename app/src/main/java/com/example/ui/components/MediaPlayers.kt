package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.LessonMedia
import com.example.ui.icons.Article
import com.example.ui.icons.Audiotrack
import com.example.ui.icons.ChevronLeft
import com.example.ui.icons.ChevronRight
import com.example.ui.icons.Description
import com.example.ui.icons.Download
import com.example.ui.icons.Forward10
import com.example.ui.icons.Image as ImageIcon
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.Pause
import com.example.ui.icons.PlayCircle
import com.example.ui.icons.Replay10
import com.example.ui.icons.SwapHoriz
import com.example.ui.icons.Videocam
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.util.MediaFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

// ======================= PDF VIEWER CARD =======================
@Composable
fun PdfViewerCard(
    pdfMedia: LessonMedia,
    isAdmin: Boolean,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onResolvePdfFile: suspend (LessonMedia) -> File? = { null },
    onOpenFullscreen: (LessonMedia) -> Unit = {},
    onOpenExternal: (LessonMedia) -> Unit = {},
    onDownload: (LessonMedia) -> Unit = {},
    initialPage: Int = 1,
    canDownload: Boolean = true,
    onPageStudyTick: (currentPage: Int, totalPages: Int, activeDeltaSec: Long, idleDeltaSec: Long) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPage by remember(pdfMedia.id, pdfMedia.url) { mutableIntStateOf(initialPage.coerceAtLeast(1)) }
    var totalPages by remember(pdfMedia.id, pdfMedia.url) { mutableIntStateOf(0) }
    var resolvedFile by remember(pdfMedia.id, pdfMedia.url, pdfMedia.cloudFileId) { mutableStateOf<File?>(null) }
    var pageBitmap by remember(pdfMedia.id, pdfMedia.url) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pdfMedia.id, pdfMedia.url, pdfMedia.cloudFileId) { mutableStateOf(true) }
    var statusMessage by remember(pdfMedia.id, pdfMedia.url, pdfMedia.cloudFileId) { mutableStateOf<String?>(null) }
    var lastInteractionMs by remember(pdfMedia.id, pdfMedia.url) {
        androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis())
    }

    val latestOnPageStudyTick by rememberUpdatedState(onPageStudyTick)

    LaunchedEffect(pdfMedia.id, pdfMedia.url, pdfMedia.cloudFileId) {
        isLoading = true
        statusMessage = null
        val file = withContext(Dispatchers.IO) {
            onResolvePdfFile(pdfMedia) ?: MediaFileManager.findExistingLocalFile(context, pdfMedia)
        }
        resolvedFile = file
        if (file != null && file.exists()) {
            val pages = withContext(Dispatchers.IO) { MediaFileManager.getPdfPageCount(file) }
            totalPages = pages
            if (pages > 0 && currentPage > pages) {
                currentPage = pages
            }
            if (pages <= 0) {
                statusMessage = "Unable to decode PDF pages. Tap Open PDF to view with an installed reader or Download."
            }
        } else {
            totalPages = 0
            statusMessage = when {
                pdfMedia.cloudFileId.isNotBlank() || pdfMedia.url.startsWith("http", ignoreCase = true) ->
                    "Cloud file could not be cached offline. Connect to internet & Google Sync, then reopen."
                else ->
                    "Sample attachment preview — upload a real PDF from Admin Panel or attach an HTTPS link."
            }
        }
        isLoading = false
        lastInteractionMs = System.currentTimeMillis()
    }

    LaunchedEffect(resolvedFile?.absolutePath, currentPage, totalPages) {
        val file = resolvedFile
        if (file != null && file.exists() && totalPages > 0) {
            lastInteractionMs = System.currentTimeMillis()
            latestOnPageStudyTick(currentPage, totalPages, 0L, 0L)
            val idx = (currentPage - 1).coerceIn(0, totalPages - 1)
            pageBitmap = withContext(Dispatchers.IO) {
                MediaFileManager.renderPdfPageBitmap(file, idx, targetWidthPx = 960)
            }
        } else {
            pageBitmap = null
        }
    }

    LaunchedEffect(pdfMedia.id, totalPages, isLoading) {
        if (!isLoading && totalPages > 0) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val now = System.currentTimeMillis()
                if (now - lastInteractionMs < 45_000L) {
                    latestOnPageStudyTick(currentPage, totalPages, 1L, 0L)
                } else {
                    latestOnPageStudyTick(currentPage, totalPages, 0L, 1L)
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("card_pdf_viewer"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "PDF Document",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = pdfMedia.displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = buildString {
                                append("PDF Document • ${pdfMedia.fileSizeFormatted}")
                                if (totalPages > 0) append(" • $totalPages pages")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7F96),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onReplace, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Replace PDF",
                                tint = NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete PDF",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real Rendered PDF Page Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .clickable {
                        lastInteractionMs = System.currentTimeMillis()
                        onOpenFullscreen(pdfMedia)
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = pageBitmap
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = NavyPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading PDF pages...",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    bmp != null -> {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "${pdfMedia.displayTitle} - Page $currentPage",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = GoldDark,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pdfMedia.fileName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = statusMessage ?: "Tap Read PDF to open document",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page navigation & Reader / Download controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (currentPage > 1) currentPage-- },
                        enabled = totalPages > 0 && currentPage > 1,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (totalPages > 0 && currentPage > 1) NavyPrimary else Color.LightGray
                        )
                    }

                    Text(
                        text = if (totalPages > 0) "$currentPage / $totalPages" else "-- / --",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )

                    IconButton(
                        onClick = { if (currentPage < totalPages) currentPage++ },
                        enabled = totalPages > 0 && currentPage < totalPages,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (totalPages > 0 && currentPage < totalPages) NavyPrimary else Color.LightGray
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (canDownload) {
                        IconButton(
                            onClick = { onDownload(pdfMedia) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download PDF",
                                tint = NavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (totalPages > 0) onOpenFullscreen(pdfMedia) else onOpenExternal(pdfMedia)
                        },
                        modifier = Modifier.height(34.dp).testTag("btn_open_pdf_reader"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Open PDF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ======================= FULLSCREEN IN-APP PDF READER =======================
@Composable
fun PdfPreviewDialog(
    media: LessonMedia,
    onResolvePdfFile: suspend (LessonMedia) -> File?,
    onOpenExternal: (LessonMedia) -> Unit,
    onDownload: (LessonMedia) -> Unit,
    onDismiss: () -> Unit,
    initialPage: Int = 1,
    canDownload: Boolean = true,
    onPageStudyTick: (currentPage: Int, totalPages: Int, activeDeltaSec: Long, idleDeltaSec: Long) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var resolvedFile by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<File?>(null) }
    var currentPage by remember(media.id, media.url) { mutableIntStateOf(initialPage.coerceAtLeast(1)) }
    var totalPages by remember(media.id, media.url) { mutableIntStateOf(0) }
    var bitmap by remember(media.id, media.url) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(media.id, media.url, media.cloudFileId) { mutableStateOf(true) }
    var errorMsg by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<String?>(null) }
    var lastInteractionMs by remember(media.id, media.url) {
        androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis())
    }

    val latestOnPageStudyTick by rememberUpdatedState(onPageStudyTick)

    LaunchedEffect(media.id, media.url, media.cloudFileId) {
        isLoading = true
        errorMsg = null
        val file = withContext(Dispatchers.IO) {
            onResolvePdfFile(media) ?: MediaFileManager.findExistingLocalFile(context, media)
        }
        resolvedFile = file
        if (file != null && file.exists()) {
            val pages = withContext(Dispatchers.IO) { MediaFileManager.getPdfPageCount(file) }
            totalPages = pages
            if (pages > 0 && currentPage > pages) {
                currentPage = pages
            }
            if (pages <= 0) {
                errorMsg = "Could not render PDF inside built-in reader. Try Download or Open External App."
            }
        } else {
            errorMsg = "Document file could not be downloaded or found locally. Verify internet & Google Sync."
        }
        isLoading = false
        lastInteractionMs = System.currentTimeMillis()
    }

    LaunchedEffect(resolvedFile?.absolutePath, currentPage, totalPages) {
        val file = resolvedFile
        if (file != null && file.exists() && totalPages > 0) {
            lastInteractionMs = System.currentTimeMillis()
            latestOnPageStudyTick(currentPage, totalPages, 0L, 0L)
            val idx = (currentPage - 1).coerceIn(0, totalPages - 1)
            bitmap = withContext(Dispatchers.IO) {
                MediaFileManager.renderPdfPageBitmap(file, idx, targetWidthPx = 1400)
            }
        }
    }

    LaunchedEffect(media.id, totalPages, isLoading) {
        if (!isLoading && totalPages > 0) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val now = System.currentTimeMillis()
                if (now - lastInteractionMs < 45_000L) {
                    latestOnPageStudyTick(currentPage, totalPages, 1L, 0L)
                } else {
                    latestOnPageStudyTick(currentPage, totalPages, 0L, 1L)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("dialog_pdf_preview"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.displayTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = if (totalPages > 0) "Page $currentPage of $totalPages • ${media.fileSizeFormatted}" else media.fileName,
                            color = GoldSoft,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canDownload) {
                            IconButton(onClick = { onDownload(media) }) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Download PDF", tint = GoldMetallic)
                            }
                        }
                        IconButton(onClick = { onOpenExternal(media) }) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open in External Viewer", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_pdf_preview")) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Reader", tint = Color.White)
                        }
                    }
                }

                // Viewer Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    when {
                        isLoading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GoldMetallic)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Preparing document...", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        bmp != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "${media.displayTitle} page $currentPage",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                )
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = errorMsg ?: "Unable to preview this PDF.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (canDownload) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { onDownload(media) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save / Download File", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Page Controls
                if (totalPages > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavyDark)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                lastInteractionMs = System.currentTimeMillis()
                                if (currentPage > 1) currentPage--
                            },
                            enabled = currentPage > 1,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontSize = 12.sp)
                        }

                        Text(
                            text = "Page $currentPage / $totalPages",
                            color = GoldMetallic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = {
                                lastInteractionMs = System.currentTimeMillis()
                                if (currentPage < totalPages) currentPage++
                            },
                            enabled = currentPage < totalPages,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark)
                        ) {
                            Text("Next", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ======================= FULLSCREEN IMAGE VIEWER =======================
@Composable
fun ImagePreviewDialog(
    media: LessonMedia,
    onResolveImageFile: suspend (LessonMedia) -> File?,
    onDownload: (LessonMedia) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var resolvedFile by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<File?>(null) }
    var isLoading by remember(media.id, media.url, media.cloudFileId) { mutableStateOf(true) }

    LaunchedEffect(media.id, media.url, media.cloudFileId) {
        isLoading = true
        resolvedFile = withContext(Dispatchers.IO) {
            onResolveImageFile(media) ?: MediaFileManager.findExistingLocalFile(context, media)
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("dialog_image_preview"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(media.displayTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text("Image • ${media.fileSizeFormatted}", color = GoldSoft, fontSize = 11.sp)
                    }
                    Row {
                        IconButton(onClick = { onDownload(media) }) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download Image", tint = GoldMetallic)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> CircularProgressIndicator(color = GoldMetallic)
                        resolvedFile != null && resolvedFile?.exists() == true -> {
                            AsyncImage(
                                model = resolvedFile,
                                contentDescription = media.displayTitle,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        media.url.startsWith("http", ignoreCase = true) -> {
                            AsyncImage(
                                model = media.url,
                                contentDescription = media.displayTitle,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.ImageIcon, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Image not downloaded offline yet.", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================= AUDIO PLAYER CARD =======================
@Composable
fun AudioPlayerCard(
    audioMedia: LessonMedia,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    isAdmin: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipSeconds: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = if (durationMs > 0) durationMs else 18 * 60 * 1000L
    val progressRatio = (progressMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("card_audio_player"),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavySecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio Lecture",
                            tint = GoldMetallic,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = audioMedia.fileName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "Audio Lecture • ${audioMedia.fileSizeFormatted}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GoldSoft,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onReplace, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Replace Audio",
                                tint = GoldSoft,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Audio",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated waveform bars simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyPrimary.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val heights = listOf(14, 24, 30, 18, 28, 12, 22, 32, 16, 26, 30, 20, 14, 28, 18, 24, 32, 15, 25, 20)
                heights.forEachIndexed { idx, h ->
                    val barProgress = idx.toFloat() / heights.size.toFloat()
                    val isActive = barProgress <= progressRatio
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(if (isPlaying) (h * (0.8f + (idx % 3) * 0.1f)).dp else (h * 0.5f).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isActive) GoldMetallic else Color(0xFF2C4362))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider
            Slider(
                value = progressRatio,
                onValueChange = { ratio ->
                    onSeek((ratio * totalMs).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = GoldMetallic,
                    activeTrackColor = GoldMetallic,
                    inactiveTrackColor = NavySecondary
                )
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(progressMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 11.sp)
                )
                Text(
                    text = formatTime(totalMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9FB2CA), fontSize = 11.sp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls: Skip -10, Play/Pause, Skip +10, Speed Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Playback speed selector chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (playbackSpeed == speed) GoldMetallic else NavySecondary)
                                .clickable { onSpeedChange(speed) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (playbackSpeed == speed) NavyPrimary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Playback buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSkipSeconds(-10) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(GoldMetallic)
                            .clickable { onPlayPause() }
                            .testTag("btn_audio_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NavyPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(onClick = { onSkipSeconds(10) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ======================= VIDEO PLAYER CARD =======================
@Composable
fun VideoPlayerCard(
    videoMedia: LessonMedia,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isAdmin: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onResolveVideoFile: suspend (LessonMedia) -> File? = { null },
    onOpenFullscreen: (LessonMedia) -> Unit = {},
    onOpenExternal: (LessonMedia) -> Unit = {},
    onDownload: (LessonMedia) -> Unit = {},
    canDownload: Boolean = true,
    onPlaybackProgress: (positionMs: Long, durationMs: Long, isPlaying: Boolean) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    var resolvedFile by remember(videoMedia.id, videoMedia.url, videoMedia.cloudFileId) { mutableStateOf<File?>(null) }
    var isResolving by remember(videoMedia.id, videoMedia.url, videoMedia.cloudFileId) { mutableStateOf(true) }
    var actualDurationMs by remember(videoMedia.id, videoMedia.url, videoMedia.cloudFileId) { mutableLongStateOf(0L) }
    var isPrepared by remember(videoMedia.id, videoMedia.url, videoMedia.cloudFileId) { mutableStateOf(false) }
    var playbackError by remember(videoMedia.id, videoMedia.url, videoMedia.cloudFileId) { mutableStateOf<String?>(null) }
    var videoViewRef by remember(videoMedia.id, videoMedia.url, videoMedia.cloudFileId) { mutableStateOf<VideoView?>(null) }

    val latestOnPlaybackProgress by rememberUpdatedState(onPlaybackProgress)
    val latestPositionMs by rememberUpdatedState(positionMs)

    LaunchedEffect(videoMedia.id, videoMedia.url, videoMedia.cloudFileId) {
        isResolving = true
        playbackError = null
        isPrepared = false
        val file = withContext(Dispatchers.IO) {
            onResolveVideoFile(videoMedia) ?: MediaFileManager.findExistingLocalFile(context, videoMedia)
        }
        resolvedFile = file
        isResolving = false
    }

    val trimmedUrl = videoMedia.url.trim()
    val playableUri: Uri? = remember(resolvedFile?.absolutePath, trimmedUrl) {
        when {
            resolvedFile != null && resolvedFile?.exists() == true && (resolvedFile?.length() ?: 0L) > 0L ->
                Uri.fromFile(resolvedFile)
            trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true) ->
                Uri.parse(trimmedUrl)
            else -> null
        }
    }

    LaunchedEffect(isPlaying, isPrepared, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        runCatching {
            if (isPlaying && !vv.isPlaying) {
                vv.start()
            } else if (!isPlaying && vv.isPlaying) {
                vv.pause()
            }
        }
    }

    LaunchedEffect(positionMs, isPrepared, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        val current = runCatching { vv.currentPosition.toLong() }.getOrDefault(positionMs)
        if (abs(current - positionMs) > 1500L) {
            runCatching { vv.seekTo(positionMs.toInt()) }
        }
    }

    LaunchedEffect(isPlaying, isPrepared, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        if (isPrepared && isPlaying) {
            while (true) {
                delay(500L)
                val pos = runCatching { vv.currentPosition.toLong() }.getOrNull() ?: break
                val dur = (runCatching { vv.duration.toLong() }.getOrDefault(actualDurationMs)).coerceAtLeast(1000L)
                val playingNow = runCatching { vv.isPlaying }.getOrDefault(false)
                latestOnPlaybackProgress(pos, dur, playingNow)
                if (!playingNow) break
            }
        }
    }

    DisposableEffect(videoMedia.id, videoViewRef) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    val totalMs = when {
        actualDurationMs > 0L -> actualDurationMs
        durationMs > 0L -> durationMs
        else -> 1000L
    }
    val progressRatio = (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("card_video_player"),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavySecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Lesson",
                            tint = GoldMetallic,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = videoMedia.displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = buildString {
                                append("Video Lecture • ${videoMedia.fileSizeFormatted}")
                                if (actualDurationMs > 0L) {
                                    append(" • ${formatTime(actualDurationMs)}")
                                } else if (!videoMedia.durationFormatted.isNullOrBlank()) {
                                    append(" • ${videoMedia.durationFormatted}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GoldSoft,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onReplace, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Replace Video",
                                tint = GoldSoft,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Video",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Real Video Canvas / Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (playableUri != null && !isInspection && playbackError == null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                runCatching {
                                    setVideoURI(playableUri)
                                    setOnPreparedListener { mp ->
                                        val dur = mp.duration.toLong().coerceAtLeast(1000L)
                                        actualDurationMs = dur
                                        isPrepared = true
                                        val resumePos = latestPositionMs
                                        if (resumePos in 1L until dur) {
                                            runCatching { seekTo(resumePos.toInt()) }
                                        }
                                        latestOnPlaybackProgress(resumePos, dur, isPlaying)
                                        if (isPlaying) {
                                            runCatching { start() }
                                        }
                                    }
                                    setOnCompletionListener {
                                        val dur = actualDurationMs.coerceAtLeast(1000L)
                                        latestOnPlaybackProgress(dur, dur, false)
                                    }
                                    setOnErrorListener { _, what, extra ->
                                        playbackError = "Codec/network error ($what/$extra). Try Open External App or Download."
                                        isPrepared = false
                                        true
                                    }
                                }
                                videoViewRef = this
                            }
                        },
                        update = { vv ->
                            if (videoViewRef !== vv) {
                                videoViewRef = vv
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = isPrepared) { onPlayPause() }
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isPlaying) Color(0xFFDC2626) else NavyPrimary.copy(alpha = 0.85f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when {
                                    !isPrepared -> "PREPARING..."
                                    isPlaying -> "PLAYING"
                                    else -> "PAUSED / READY"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onOpenFullscreen(videoMedia) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NavyDark.copy(alpha = 0.75f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Fullscreen Video",
                                    tint = GoldMetallic,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(GoldMetallic.copy(alpha = 0.92f))
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = NavyPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        NavyPrimary.copy(alpha = 0.85f),
                                        NavyDark,
                                        Color.Black
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isResolving -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = GoldMetallic, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Loading video stream...", color = Color.White, fontSize = 12.sp)
                                }
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = GoldMetallic,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = playbackError ?: when {
                                            videoMedia.cloudFileId.isNotBlank() ->
                                                "Cloud video could not be downloaded offline. Connect to internet & Google Sync."
                                            else ->
                                                "Video file is on the uploader's device only. Re-upload under 18 MB or attach an HTTPS video URL."
                                        },
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Seek Bar & Video Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Slider(
                    value = progressRatio,
                    onValueChange = { ratio -> onSeek((ratio * totalMs).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = GoldMetallic,
                        activeTrackColor = GoldMetallic,
                        inactiveTrackColor = NavySecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(positionMs)} / ${if (actualDurationMs > 0L || durationMs > 0L) formatTime(totalMs) else (videoMedia.durationFormatted ?: "00:00")}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenFullscreen(videoMedia) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSoft)
                        ) {
                            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fullscreen", fontSize = 11.sp)
                        }
                        if (canDownload) {
                            OutlinedButton(
                                onClick = { onDownload(videoMedia) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldMetallic)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", fontSize = 11.sp)
                            }
                        }
                        IconButton(
                            onClick = { onOpenExternal(videoMedia) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open Video Externally",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================= FULLSCREEN VIDEO DIALOG =======================
@Composable
fun VideoPreviewDialog(
    media: LessonMedia,
    onResolveVideoFile: suspend (LessonMedia) -> File?,
    onOpenExternal: (LessonMedia) -> Unit,
    onDownload: (LessonMedia) -> Unit,
    onDismiss: () -> Unit,
    canDownload: Boolean = true,
    onPlaybackProgress: (positionMs: Long, durationMs: Long, isPlaying: Boolean) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    var resolvedFile by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<File?>(null) }
    var isLoading by remember(media.id, media.url, media.cloudFileId) { mutableStateOf(true) }
    var playbackError by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<String?>(null) }
    var isPlaying by remember(media.id, media.url, media.cloudFileId) { mutableStateOf(true) }
    var isPrepared by remember(media.id, media.url, media.cloudFileId) { mutableStateOf(false) }
    var posMs by remember(media.id, media.url, media.cloudFileId) { mutableLongStateOf(0L) }
    var durMs by remember(media.id, media.url, media.cloudFileId) { mutableLongStateOf(1000L) }
    var videoViewRef by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<VideoView?>(null) }

    val latestOnProgress by rememberUpdatedState(onPlaybackProgress)

    LaunchedEffect(media.id, media.url, media.cloudFileId) {
        isLoading = true
        playbackError = null
        resolvedFile = withContext(Dispatchers.IO) {
            onResolveVideoFile(media) ?: MediaFileManager.findExistingLocalFile(context, media)
        }
        isLoading = false
    }

    val trimmedUrl = media.url.trim()
    val playableUri: Uri? = remember(resolvedFile?.absolutePath, trimmedUrl) {
        when {
            resolvedFile != null && resolvedFile?.exists() == true && (resolvedFile?.length() ?: 0L) > 0L ->
                Uri.fromFile(resolvedFile)
            trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true) ->
                Uri.parse(trimmedUrl)
            else -> null
        }
    }

    LaunchedEffect(isPlaying, isPrepared, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        if (isPrepared && isPlaying) {
            while (true) {
                delay(500L)
                val p = runCatching { vv.currentPosition.toLong() }.getOrNull() ?: break
                val d = runCatching { vv.duration.toLong() }.getOrDefault(durMs).coerceAtLeast(1000L)
                val active = runCatching { vv.isPlaying }.getOrDefault(false)
                posMs = p
                durMs = d
                latestOnProgress(p, d, active)
                if (!active) break
            }
        }
    }

    DisposableEffect(media.id, videoViewRef) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("dialog_video_preview"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF020617)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(media.displayTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text("Video • ${media.fileSizeFormatted}", color = GoldSoft, fontSize = 11.sp, maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canDownload) {
                            IconButton(onClick = { onDownload(media) }) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Download Video", tint = GoldMetallic)
                            }
                        }
                        IconButton(onClick = { onOpenExternal(media) }) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open External Player", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_video_preview")) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Video", tint = Color.White)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GoldMetallic)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Downloading video stream...", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        playableUri != null && !isInspection && playbackError == null -> {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        runCatching {
                                            setVideoURI(playableUri)
                                            setOnPreparedListener { mp ->
                                                durMs = mp.duration.toLong().coerceAtLeast(1000L)
                                                isPrepared = true
                                                if (isPlaying) start()
                                            }
                                            setOnCompletionListener {
                                                isPlaying = false
                                                posMs = durMs
                                                latestOnProgress(durMs, durMs, false)
                                            }
                                            setOnErrorListener { _, what, extra ->
                                                playbackError = "Unable to decode video stream ($what/$extra). Try Open External Player."
                                                isPrepared = false
                                                true
                                            }
                                        }
                                        videoViewRef = this
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = playbackError ?: "Video file could not be loaded offline. Connect to internet & Google Sync or open externally.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onOpenExternal(media) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open External Player", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (playableUri != null && playbackError == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavyDark)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        val ratio = (posMs.toFloat() / durMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        Slider(
                            value = ratio,
                            onValueChange = { r ->
                                val target = (r * durMs).toLong()
                                posMs = target
                                runCatching { videoViewRef?.seekTo(target.toInt()) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldMetallic,
                                activeTrackColor = GoldMetallic,
                                inactiveTrackColor = NavySecondary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${formatTime(posMs)} / ${formatTime(durMs)}", color = Color.White, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val target = (posMs - 10_000L).coerceAtLeast(0L)
                                    posMs = target
                                    runCatching { videoViewRef?.seekTo(target.toInt()) }
                                }) {
                                    Icon(imageVector = Icons.Default.Replay10, contentDescription = "Back 10s", tint = Color.White)
                                }
                                Button(
                                    onClick = {
                                        val vv = videoViewRef ?: return@Button
                                        if (isPlaying) {
                                            runCatching { vv.pause() }
                                            isPlaying = false
                                        } else {
                                            runCatching { vv.start() }
                                            isPlaying = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPlaying) "Pause" else "Play", fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = {
                                    val target = (posMs + 10_000L).coerceAtMost(durMs)
                                    posMs = target
                                    runCatching { videoViewRef?.seekTo(target.toInt()) }
                                }) {
                                    Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================= IN-APP DOCUMENT PREVIEW & VIEWER =======================
@Composable
fun DocumentPreviewDialog(
    media: LessonMedia,
    onResolveDocumentFile: suspend (LessonMedia) -> File?,
    onOpenExternal: (LessonMedia) -> Unit,
    onDownload: (LessonMedia) -> Unit,
    onDismiss: () -> Unit,
    canDownload: Boolean = true
) {
    val context = LocalContext.current
    var resolvedFile by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<File?>(null) }
    var textPreview by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<String?>(null) }
    var isLoading by remember(media.id, media.url, media.cloudFileId) { mutableStateOf(true) }

    LaunchedEffect(media.id, media.url, media.cloudFileId) {
        isLoading = true
        val file = withContext(Dispatchers.IO) {
            onResolveDocumentFile(media) ?: MediaFileManager.findExistingLocalFile(context, media)
        }
        resolvedFile = file
        textPreview = if (file != null && file.exists() && MediaFileManager.isPlainTextDocument(media)) {
            withContext(Dispatchers.IO) { MediaFileManager.readTextPreview(file) }
        } else {
            null
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("dialog_document_preview"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(media.displayTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text("${media.type.displayLabel} • ${media.fileSizeFormatted} • ${media.fileName}", color = GoldSoft, fontSize = 11.sp, maxLines = 1)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_document_preview")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Document", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = NavyPrimary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Syncing document file...", color = NavyPrimary, fontSize = 12.sp)
                            }
                        }
                        textPreview != null -> {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = textPreview.orEmpty().ifBlank { "(Empty text document)" },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(14.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFD6E2F0), RoundedCornerShape(14.dp))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Article,
                                    contentDescription = null,
                                    tint = GoldDark,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = media.displayTitle,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${media.fileName} • ${media.fileSizeFormatted}",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (media.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = media.description,
                                        color = Color(0xFF334155),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val statusText = when {
                                    resolvedFile != null && resolvedFile?.exists() == true ->
                                        "Ready offline on your phone. Open with your installed document app or save a copy."
                                    media.url.startsWith("http", ignoreCase = true) ->
                                        "Hosted via HTTPS link. Open in browser / document app."
                                    media.cloudFileId.isNotBlank() ->
                                        "Cloud attachment could not be downloaded yet. Verify internet & Google Sync."
                                    else ->
                                        "Attachment stored on uploader's device only."
                                }
                                Text(
                                    text = statusText,
                                    color = Color(0xFF475569),
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenExternal(media) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open External App", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    if (canDownload) {
                        Button(
                            onClick = { onDownload(media) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L)) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
