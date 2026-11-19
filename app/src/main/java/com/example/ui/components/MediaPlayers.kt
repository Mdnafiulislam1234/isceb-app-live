package com.example.ui.components

import android.graphics.Bitmap
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.LessonMedia
import com.example.ui.icons.Audiotrack
import com.example.ui.icons.ChevronLeft
import com.example.ui.icons.ChevronRight
import com.example.ui.icons.Description
import com.example.ui.icons.Download
import com.example.ui.icons.Forward10
import com.example.ui.icons.Image as ImageIcon
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.Pause
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
import kotlinx.coroutines.withContext
import java.io.File

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPage by remember(pdfMedia.id, pdfMedia.url) { mutableIntStateOf(1) }
    var totalPages by remember(pdfMedia.id, pdfMedia.url) { mutableIntStateOf(0) }
    var resolvedFile by remember(pdfMedia.id, pdfMedia.url, pdfMedia.cloudFileId) { mutableStateOf<File?>(null) }
    var pageBitmap by remember(pdfMedia.id, pdfMedia.url) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pdfMedia.id, pdfMedia.url, pdfMedia.cloudFileId) { mutableStateOf(true) }
    var statusMessage by remember(pdfMedia.id, pdfMedia.url, pdfMedia.cloudFileId) { mutableStateOf<String?>(null) }

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
    }

    LaunchedEffect(resolvedFile?.absolutePath, currentPage, totalPages) {
        val file = resolvedFile
        if (file != null && file.exists() && totalPages > 0) {
            val idx = (currentPage - 1).coerceIn(0, totalPages - 1)
            pageBitmap = withContext(Dispatchers.IO) {
                MediaFileManager.renderPdfPageBitmap(file, idx, targetWidthPx = 960)
            }
        } else {
            pageBitmap = null
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
                    .clickable { onOpenFullscreen(pdfMedia) }
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var resolvedFile by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<File?>(null) }
    var currentPage by remember(media.id, media.url) { mutableIntStateOf(1) }
    var totalPages by remember(media.id, media.url) { mutableIntStateOf(0) }
    var bitmap by remember(media.id, media.url) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(media.id, media.url, media.cloudFileId) { mutableStateOf(true) }
    var errorMsg by remember(media.id, media.url, media.cloudFileId) { mutableStateOf<String?>(null) }

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
            if (pages <= 0) {
                errorMsg = "Could not render PDF inside built-in reader. Try Download or Open External App."
            }
        } else {
            errorMsg = "Document file could not be downloaded or found locally. Verify internet & Google Sync."
        }
        isLoading = false
    }

    LaunchedEffect(resolvedFile?.absolutePath, currentPage, totalPages) {
        val file = resolvedFile
        if (file != null && file.exists() && totalPages > 0) {
            val idx = (currentPage - 1).coerceIn(0, totalPages - 1)
            bitmap = withContext(Dispatchers.IO) {
                MediaFileManager.renderPdfPageBitmap(file, idx, targetWidthPx = 1400)
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
                        IconButton(onClick = { onDownload(media) }) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download PDF", tint = GoldMetallic)
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
                            onClick = { if (currentPage > 1) currentPage-- },
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
                            onClick = { if (currentPage < totalPages) currentPage++ },
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
    modifier: Modifier = Modifier
) {
    val totalMs = if (durationMs > 0) durationMs else 12 * 60 * 1000L + 30 * 1000L
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
                            text = videoMedia.fileName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "Executive Video • ${videoMedia.fileSizeFormatted}",
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

            // Video Screen Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color.Black)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                // Gradient backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    NavyPrimary.copy(alpha = 0.8f),
                                    NavyDark,
                                    Color.Black
                                )
                            )
                        )
                )

                // Visual telemetry overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isPlaying) "STREAMING LIVE" else "READY",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }

                        Text(
                            text = "1080p HD • BROWSER STREAMING",
                            color = GoldSoft,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Center Play/Pause Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GoldMetallic.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NavyPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Bottom Seek & Duration inside player
                    Column {
                        Slider(
                            value = progressRatio,
                            onValueChange = { ratio -> onSeek((ratio * totalMs).toLong()) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldMetallic,
                                activeTrackColor = GoldMetallic,
                                inactiveTrackColor = Color.DarkGray
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatTime(positionMs)} / ${formatTime(totalMs)}",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Auto-bandwidth Adaptive",
                                color = Color(0xFFA0B4CB),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
