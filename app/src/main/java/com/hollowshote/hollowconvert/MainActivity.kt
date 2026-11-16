package com.hollowshote.hollowconvert

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF10121A)
private val Panel = Color(0xFF1B1F2B)
private val Accent = Color(0xFF7C6CFF)
private val Muted = Color(0xFF9AA2B5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HollowToolboxApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HollowToolboxApp() {
    val context = LocalContext.current
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("No file selected") }
    var sourceMime by remember { mutableStateOf<String?>(null) }
    var outputText by remember { mutableStateOf("") }
    var outputBytes by remember { mutableStateOf<ByteArray?>(null) }
    var outputExtension by remember { mutableStateOf("txt") }
    var outputMime by remember { mutableStateOf("text/plain") }
    var status by remember { mutableStateOf("Choose a file to get started.") }
    var selectedTool by remember { mutableStateOf("Text & documents") }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var showConverter by remember { mutableStateOf(false) }
    var plannedTool by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceName = queryDisplayName(context, uri)
            sourceMime = context.contentResolver.getType(uri)
            val detected = detectFileType(sourceName, sourceMime)
            selectedTool = when (detected) {
                "Image" -> "Images"
                "ZIP archive" -> "Archives"
                "Text document" -> "Text & documents"
                "Audio" -> "Audio"
                "Video" -> "Video"
                else -> "Other"
            }
            outputText = ""
            outputBytes = null
            status = "File ready. Choose a conversion."
        }
    }

    val chooseZipFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folderUri ->
        val zipUri = pendingZipUri
        if (folderUri != null && zipUri != null) {
            scope.launch {
                isBusy = true
                status = "Extracting ZIP archive..."
                val result = runCatching { withContext(Dispatchers.IO) { extractZipSafely(context, zipUri, folderUri) } }
                status = result.fold(
                    { "Extracted " + it.toString() + " file(s)." },
                    { "Extraction failed: " + (it.message ?: "Unknown error") }
                )
                isBusy = false
                pendingZipUri = null
            }
        }
    }

    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null && (outputBytes != null || outputText.isNotEmpty())) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    val bytes = outputBytes ?: outputText.toByteArray(Charsets.UTF_8)
                    stream.write(bytes)
                } ?: error("Unable to create the output file.")
            }.onSuccess { status = "Export complete." }
             .onFailure { status = "Could not save: ${it.message}" }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Ink, topBar = {
            TopAppBar(
                navigationIcon = {
                    if (showConverter) {
                        IconButton(onClick = { showConverter = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to home", tint = Color.White)
                        }
                    }
                },
                title = {
                    Column {
                        Text("HOLLOW TOOLBOX", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Text("YOUR FILES. YOUR FORMAT.", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        }) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!showConverter) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text("WELCOME TO HOLLOW TOOLBOX", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                            Text("What do you want to do?", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                            Text("Your all-in-one space for file tools.", color = Muted, fontSize = 14.sp)
                            Card(
                                onClick = {
                                    showConverter = true
                                    scope.launch { listState.scrollToItem(0) }
                                },
                                modifier = Modifier.fillMaxWidth().height(174.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF272344)),
                                shape = RoundedCornerShape(22.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Accent)
                            ) {
                                Row(
                                    Modifier.fillMaxSize().padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                        Text("CONVERT FILES", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                        Text("Open the file converter and choose what to convert.", color = Muted, fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("OPEN CONVERTER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(7.dp))
                                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Accent)
                                        }
                                    }
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Accent, modifier = Modifier.size(48.dp))
                                }
                            }
                        }
                    }
                } else {
                item {
                    val detectedFileType = detectFileType(sourceName, sourceMime)
                    Text("CONVERT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("CONVERT FILES", fontWeight = FontWeight.Black, fontSize = 21.sp)
                            Text("Choose a file. Hollow Toolbox detects its type and shows the formats it can convert to.", color = Muted, fontSize = 12.sp)

                            var format by remember(selectedTool, sourceName, sourceUri) {
                                mutableStateOf(defaultOutputFormat(selectedTool, sourceName))
                            }
                            val fileExtension = sourceName.substringAfterLast('.', "").lowercase()
                            val outputFormats = if (sourceUri == null) emptyList() else when (selectedTool) {
                                "Images" -> listOf("PNG", "JPG", "WEBP").filterNot {
                                    it.lowercase() == fileExtension || (it == "JPG" && fileExtension == "jpeg")
                                }
                                "Archives" -> listOf("EXTRACT ZIP")
                                "Other" -> listOf("ZIP")
                                "Audio", "Video" -> emptyList()
                                else -> listOf("TXT", "HTML", "JSON", "CSV", "BASE64", "URL ENCODE", "URL DECODE").filterNot {
                                    it.lowercase() == fileExtension || (it == "HTML" && fileExtension == "htm")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    onClick = {
                                        openFile.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.size(76.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF272344)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Accent),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        Modifier.fillMaxSize().padding(5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Accent, modifier = Modifier.size(27.dp))
                                        Text("FILE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("INPUT FILE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text(if (sourceUri == null) "Choose a file" else sourceName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                    Text(if (sourceUri == null) "Type will be detected automatically" else "Detected: " + detectedFileType, color = Accent, fontSize = 10.sp, maxLines = 1)
                                }
                            }

                            Text(
                                "TO",
                                modifier = Modifier.fillMaxWidth(),
                                color = Accent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    onClick = {
                                        if (outputBytes != null || outputText.isNotEmpty()) {
                                            saveFile.launch("hollow-toolbox-output." + (if (selectedTool == "Archives") "zip" else format.lowercase().replace(" ", "-")))
                                        } else {
                                            status = "Convert the file first, then save the output here."
                                        }
                                    },
                                    modifier = Modifier.size(76.dp),
                                    colors = CardDefaults.cardColors(containerColor = Panel),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF454B60)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        Modifier.fillMaxSize().padding(5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.ArrowForward, null, tint = if (outputBytes != null || outputText.isNotEmpty()) Accent else Muted, modifier = Modifier.size(27.dp))
                                        Text("OUTPUT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("TO FORMAT", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    if (outputFormats.isNotEmpty()) {
                                        FormatDropdown(
                                            value = if (selectedTool == "Archives") "EXTRACT ZIP" else format,
                                            options = outputFormats,
                                            onSelected = { format = it }
                                        )
                                    } else {
                                        Text("No compatible output formats yet", color = Muted, fontSize = 12.sp)
                                    }
                                    Text(
                                        when (selectedTool) {
                                            "Images" -> "Image formats supported by this device"
                                            "Archives" -> "Extract the ZIP contents to a folder"
                                            "Other" -> "Package this file as a ZIP archive"
                                            "Audio" -> "Audio conversion engine not included yet"
                                            "Video" -> "Video conversion engine not included yet"
                                            else -> "Text and document formats"
                                        },
                                        color = Muted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (selectedTool == "Archives") {
                                        if (sourceUri == null) {
                                            status = "Select a ZIP file first."
                                        } else {
                                            pendingZipUri = sourceUri
                                            chooseZipFolder.launch(null)
                                        }
                                    } else {
                                    val uri = sourceUri
                                    if (uri == null) {
                                        status = "Select a file first."
                                    } else {
                                        runCatching {
                                            if (selectedTool == "Other") {
                                                outputBytes = createZip(context, listOf(uri))
                                                outputText = ""
                                                outputExtension = "zip"
                                            } else if (selectedTool == "Images") {
                                                val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                                                    ?: error("This file could not be decoded as an image.")
                                                val stream = ByteArrayOutputStream()
                                                val imageFormat = when (format) {
                                                    "JPG" -> Bitmap.CompressFormat.JPEG
                                                    "WEBP" -> Bitmap.CompressFormat.WEBP
                                                    else -> Bitmap.CompressFormat.PNG
                                                }
                                                val mime = when (format) {
                                                    "JPG" -> "image/jpeg"
                                                    "WEBP" -> "image/webp"
                                                    else -> "image/png"
                                                }
                                                check(bitmap.compress(imageFormat, 92, stream)) { "Image conversion failed." }
                                                outputBytes = stream.toByteArray()
                                                outputText = ""
                                                outputExtension = format.lowercase()
                                                outputMime = mime
                                                bitmap.recycle()
                                            } else {
                                                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                                    ?: error("Unable to read this file.")
                                                outputText = convertText(raw, format)
                                                outputBytes = null
                                                outputExtension = format.lowercase().replace(" ", "-")
                                                outputMime = "text/plain"
                                            }
                                            status = "Conversion ready. Save your result below."
                                        }.onFailure { status = "Conversion failed: ${it.message}" }
                                    }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isBusy && sourceUri != null && outputFormats.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text(if (selectedTool == "Archives") "Choose folder & extract" else if (selectedTool == "Images") "Convert image" else "Convert file")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, null)
                            }
                            if (outputText.isNotEmpty() || outputBytes != null) {
                                Text(status, color = Color(0xFF5DD6A0), fontSize = 12.sp)
                                OutlinedButton(
                                    onClick = { saveFile.launch("hollow-toolbox-output.$outputExtension") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save converted file")
                                }
                            }
                        }
                    }
                }
                item {
                    if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Accent)
                    Text(status, color = Muted, fontSize = 11.sp)
                    Text("Audio/video transcoding and specialist 3D or game-asset conversion need additional engines and are not included yet.", color = Muted, fontSize = 10.sp)
                }
                }
            }
        }
        plannedTool?.let { tool ->
            val details = when (tool) {
                "Images" -> "PNG, JPG, and WebP conversion is not included in this build yet. Image conversion is still under development."
                "Audio" -> "MP3, WAV, and AAC conversion needs the planned audio conversion engine, which is not included in this build yet."
                else -> "MP4, MOV, and WebM conversion needs the planned video conversion engine, which is not included in this build yet."
            }
            AlertDialog(
                onDismissRequest = { plannedTool = null },
                title = { Text("$tool conversion is coming next") },
                text = { Text(details) },
                confirmButton = {
                    TextButton(onClick = { plannedTool = null }) { Text("Got it") }
                },
                containerColor = Panel
            )
        }
    }
}

private fun defaultOutputFormat(selectedTool: String, fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (selectedTool) {
        "Archives" -> "EXTRACT ZIP"
        "Other" -> "ZIP"
        "Images" -> listOf("PNG", "JPG", "WEBP").firstOrNull {
            it.lowercase() != extension && !(it == "JPG" && extension == "jpeg")
        } ?: "PNG"
        "Audio", "Video" -> ""
        else -> {
            val options = listOf("TXT", "HTML", "JSON", "CSV", "BASE64", "URL ENCODE", "URL DECODE")
            options.firstOrNull { it.lowercase() != extension && !(it == "HTML" && extension == "htm") } ?: "TXT"
        }
    }
}
private fun detectFileType(fileName: String, mimeType: String?): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when {
        extension == "zip" || mimeType == "application/zip" || mimeType == "application/x-zip-compressed" -> "ZIP archive"
        mimeType?.startsWith("image/") == true || extension in setOf("png", "jpg", "jpeg", "webp", "bmp", "gif") -> "Image"
        mimeType?.startsWith("text/") == true || extension in setOf("txt", "text", "html", "htm", "json", "csv", "md", "markdown", "xml", "yaml", "yml", "css", "js", "ts", "kt", "java", "py", "log", "ini", "toml", "properties", "sql") -> "Text document"
        mimeType?.startsWith("audio/") == true -> "Audio"
        mimeType?.startsWith("video/") == true -> "Video"
        else -> "Other file"
    }
}
@Composable
private fun FormatDropdown(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(value, modifier = Modifier.weight(1f), maxLines = 1, fontSize = 13.sp)
            Icon(Icons.Default.ArrowForward, contentDescription = "Choose format", tint = Accent)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Panel).heightIn(max = 320.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.HomeToolTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(106.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF272344) else Panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Accent else Color(0xFF34394B)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp)
            Text(subtitle, color = Muted, fontSize = 8.sp, maxLines = 1)
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) name = cursor.getString(index) ?: name
        }
    }
    return name
}

private fun createZip(context: android.content.Context, uris: List<Uri>): ByteArray {
    require(uris.size <= 100) { "Select 100 files or fewer." }
    val result = ByteArrayOutputStream()
    var total = 0L
    val used = mutableSetOf<String>()
    ZipOutputStream(result).use { zip ->
        uris.forEachIndexed { index, uri ->
            val original = queryDisplayName(context, uri).replace("/", "_")
            var name = original.ifBlank { "file_" + (index + 1) }
            var counter = 2
            while (!used.add(name.lowercase())) {
                val dot = original.lastIndexOf('.')
                name = if (dot > 0) original.substring(0, dot) + "_" + counter + original.substring(dot) else original + "_" + counter
                counter++
            }
            zip.putNextEntry(ZipEntry(name))
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 64L * 1024L * 1024L) { "Selected files exceed the 64 MB ZIP limit." }
                    zip.write(buffer, 0, count)
                }
            } ?: error("Could not read " + original)
            zip.closeEntry()
        }
    }
    return result.toByteArray()
}

private fun extractZipSafely(context: android.content.Context, zipUri: Uri, folderUri: Uri): Int {
    val root = DocumentFile.fromTreeUri(context, folderUri) ?: error("Destination folder is unavailable.")
    var extracted = 0
    var total = 0L
    val input = context.contentResolver.openInputStream(zipUri) ?: error("Could not open the ZIP file.")
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            val path = entry.name
            val parts = path.split('/').filter { it.isNotEmpty() }
            require(!path.startsWith("/") && parts.none { it == "." || it == ".." }) { "Archive contains an unsafe path." }
            require(extracted < 1000) { "Archive exceeds the 1,000-file limit." }
            if (parts.isNotEmpty()) {
                var parent = root
                for (folder in parts.dropLast(1)) {
                    parent = parent.findFile(folder) ?: parent.createDirectory(folder) ?: error("Could not create folder.")
                }
                if (entry.isDirectory) {
                    if (parent.findFile(parts.last()) == null) parent.createDirectory(parts.last())
                } else {
                    val file = parent.createFile("application/octet-stream", parts.last()) ?: error("Could not create extracted file.")
                    context.contentResolver.openOutputStream(file.uri)?.use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 256L * 1024L * 1024L) { "Extracted data exceeds the 256 MB safety limit." }
                            output.write(buffer, 0, count)
                        }
                    } ?: error("Could not write extracted file.")
                    extracted++
                }
            }
            zip.closeEntry()
        }
    }
    return extracted
}

private fun convertText(input: String, output: String): String = when (output) {
    "HTML" -> "<!doctype html>\n<html><head><meta charset=\"utf-8\"><title>Converted by Hollow Convert</title></head><body><pre>" +
        input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") +
        "</pre></body></html>"
    "JSON" -> {
        val trimmed = input.trim()
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            trimmed.isEmpty() -> "[]"
            else -> csvToJson(input)
        }
    }
    "CSV" -> jsonToCsv(input)
    "BASE64" -> Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    "URL ENCODE" -> java.net.URLEncoder.encode(input, "UTF-8")
    "URL DECODE" -> java.net.URLDecoder.decode(input, "UTF-8")
    else -> input
}

private fun parseCsv(input: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0
    while (i < input.length) {
        val ch = input[i]
        when {
            ch == '"' && quoted && i + 1 < input.length && input[i + 1] == '"' -> {
                cell.append('"'); i++
            }
            ch == '"' -> quoted = !quoted
            ch == ',' && !quoted -> {
                row.add(cell.toString()); cell.clear()
            }
            (ch == '\n' || ch == '\r') && !quoted -> {
                if (ch == '\r' && i + 1 < input.length && input[i + 1] == '\n') i++
                row.add(cell.toString()); cell.clear()
                if (row.any { it.isNotEmpty() }) rows.add(row)
                row = mutableListOf()
            }
            else -> cell.append(ch)
        }
        i++
    }
    row.add(cell.toString())
    if (row.any { it.isNotEmpty() }) rows.add(row)
    return rows
}

private fun csvToJson(input: String): String {
    val rows = parseCsv(input)
    if (rows.isEmpty()) return "[]"
    val headers = rows.first().mapIndexed { index, value -> value.ifBlank { "column_${index + 1}" } }
    val result = JSONArray()
    rows.drop(1).forEach { values ->
        val item = JSONObject()
        headers.forEachIndexed { index, key -> item.put(key, values.getOrElse(index) { "" }) }
        result.put(item)
    }
    return result.toString(2)
}

private fun jsonToCsv(input: String): String {
    val trimmed = input.trim()
    if (!trimmed.startsWith("[")) {
        return parseCsv(input).joinToString("\n") { row -> row.joinToString(",") { csvEscape(it) } }
    }
    val array = JSONArray(trimmed)
    if (array.length() == 0) return ""
    val first = array.optJSONObject(0) ?: error("CSV export needs a JSON array of objects.")
    val headers = (0 until first.length()).map { first.names()?.optString(it) ?: "" }
    val lines = mutableListOf(headers.joinToString(",") { csvEscape(it) })
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: error("Every JSON array item must be an object.")
        lines.add(headers.joinToString(",") { key -> csvEscape(item.opt(key)?.toString() ?: "") })
    }
    return lines.joinToString("\n")
}

private fun csvEscape(value: String): String =
    if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else value

