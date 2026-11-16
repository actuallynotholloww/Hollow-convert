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
    var outputText by remember { mutableStateOf("") }
    var outputBytes by remember { mutableStateOf<ByteArray?>(null) }
    var outputExtension by remember { mutableStateOf("txt") }
    var outputMime by remember { mutableStateOf("text/plain") }
    var status by remember { mutableStateOf("Choose a file to get started.") }
    var selectedTool by remember { mutableStateOf("Text & documents") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var zipAction by remember { mutableStateOf("Create ZIP") }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var plannedTool by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected file"
            outputText = ""
            outputBytes = null
            status = "File ready. Choose a conversion."
        }
    }
    val openMultipleFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            sourceUri = uris.first()
            sourceName = if (uris.size == 1) queryDisplayName(context, uris.first()) else uris.size.toString() + " files selected"
            outputText = ""
            outputBytes = null
            status = "Files ready to archive."
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
                item {
                    Column(
                        Modifier.fillMaxWidth().background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF29234F), Color(0xFF171B2A))
                            ), RoundedCornerShape(24.dp)
                        ).padding(22.dp)
                    ) {
                        Text("ONE TOOLBOX.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("A growing hub for file conversion.", color = Muted)
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { openFile.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Select a file", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Text("SELECTED FILE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = Accent)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sourceName, fontWeight = FontWeight.SemiBold)
                                Text(if (sourceUri == null) "Waiting for a file" else "Ready to convert", color = Muted, fontSize = 12.sp)
                            }
                            if (sourceUri != null) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF5DD6A0))
                        }
                    }
                }
                item {
                    Text("CONVERSION TOOLS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeToolTile("CONVERT", "Text & docs", Icons.Default.Description, selectedTool == "Text & documents") {
                                selectedTool = "Text & documents"
                                status = "Text conversion selected."
                            }
                            HomeToolTile("IMAGES", "PNG · JPG · WebP", Icons.Default.Image, selectedTool == "Images") {
                                selectedTool = "Images"
                                status = "Image conversion selected."
                            }
                            HomeToolTile("ARCHIVES", "ZIP tools", Icons.Default.Folder, selectedTool == "Archives") {
                                selectedTool = "Archives"
                                status = "Archive tools selected."
                            }
                        }
                    }
                }
                item {
                    Text("CONVERT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (selectedTool == "Text & documents") "Text & document conversions" else "$selectedTool conversions", fontWeight = FontWeight.Bold)
                            Text(if (selectedTool == "Images") "Convert an image between supported formats on your device." else "Choose a supported output format. Text conversions run on your device.", color = Muted, fontSize = 12.sp)
                            var format by remember(selectedTool) { mutableStateOf(if (selectedTool == "Images") "PNG" else "TXT") }
                            val formats = if (selectedTool == "Images") listOf("PNG", "JPG", "WEBP") else listOf("TXT", "HTML", "JSON", "CSV", "BASE64", "URL ENCODE", "URL DECODE")
                            if (selectedTool != "Archives") Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                formats.forEach { option ->
                                    FilterChip(selected = format == option, onClick = { format = option }, label = { Text(option) })
                                }
                            }
                            if (selectedTool == "Archives") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = zipAction == "Create ZIP", onClick = { zipAction = "Create ZIP" }, label = { Text("Create ZIP") })
                                    FilterChip(selected = zipAction == "Extract ZIP", onClick = { zipAction = "Extract ZIP" }, label = { Text("Extract ZIP") })
                                }
                                Text(
                                    if (zipAction == "Create ZIP") "Bundle several selected files into one compressed archive."
                                    else "Choose a ZIP file, then select its destination folder.",
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedTool == "Archives") {
                                        if (zipAction == "Create ZIP") {
                                            if (selectedUris.isEmpty()) {
                                                openMultipleFiles.launch(arrayOf("*/*"))
                                            } else {
                                                scope.launch {
                                                    isBusy = true
                                                    val result = runCatching { withContext(Dispatchers.IO) { createZip(context, selectedUris) } }
                                                    result.onSuccess {
                                                        outputBytes = it
                                                        outputText = ""
                                                        outputExtension = "zip"
                                                        status = "ZIP archive ready. Save it below."
                                                    }.onFailure { status = "ZIP failed: " + (it.message ?: "Unknown error") }
                                                    isBusy = false
                                                }
                                            }
                                        } else if (sourceUri == null) {
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
                                            if (selectedTool == "Images") {
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
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text(if (selectedTool == "Archives") if (zipAction == "Create ZIP") "Create archive" else "Extract archive" else if (selectedTool == "Images") "Convert image" else "Convert file")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, null)
                            }
                            if (outputText.isNotEmpty() || outputBytes != null) {
                                Text(status, color = Color(0xFF5DD6A0), fontSize = 12.sp)
                                OutlinedButton(onClick = { saveFile.launch("hollow-convert-output.$outputExtension") }, modifier = Modifier.fillMaxWidth()) {
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

