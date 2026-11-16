package com.hollowshote.hollowconvert

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
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
        setContent { HollowConvertApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HollowConvertApp() {
    val context = LocalContext.current
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("No file selected") }
    var outputText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Choose a file to get started.") }
    var selectedTool by remember { mutableStateOf("Auto detect") }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected file"
            outputText = ""
            status = "File ready. Choose a conversion."
        }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && outputText.isNotEmpty()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(outputText) }
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
                        Text("HOLLOW CONVERT", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Text("YOUR FILES. YOUR FORMAT.", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        }) { padding ->
            LazyColumn(
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
                        ToolRow("Text & documents", "TXT, HTML, JSON, Markdown", Icons.Default.Description) { selectedTool = "Text & documents" }
                        ToolRow("Images", "PNG, JPG, WebP — coming next", Icons.Default.Image) { selectedTool = "Images" }
                        ToolRow("Audio", "MP3, WAV, AAC — FFmpeg module planned", Icons.Default.MusicNote) { selectedTool = "Audio" }
                        ToolRow("Video", "MP4, MOV, WebM — FFmpeg module planned", Icons.Default.VideoLibrary) { selectedTool = "Video" }
                    }
                }
                item {
                    Text("CONVERT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Text & document conversions", fontWeight = FontWeight.Bold)
                            Text("Choose a supported output format. The first build includes working text conversions.", color = Muted, fontSize = 12.sp)
                            var format by remember { mutableStateOf("TXT") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("TXT", "HTML", "JSON", "CSV").forEach { option ->
                                    FilterChip(selected = format == option, onClick = { format = option }, label = { Text(option) })
                                }
                            }
                            Button(
                                onClick = {
                                    val uri = sourceUri
                                    if (uri == null) {
                                        status = "Select a file first."
                                    } else {
                                        runCatching {
                                            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                                ?: error("Unable to read this file.")
                                            outputText = convertText(raw, format)
                                            status = "Conversion ready. Save your result below."
                                        }.onFailure { status = "Conversion failed: ${it.message}" }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text("Convert file")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, null)
                            }
                            if (outputText.isNotEmpty()) {
                                Text(status, color = Color(0xFF5DD6A0), fontSize = 12.sp)
                                OutlinedButton(onClick = { saveFile.launch("hollow-convert-output.${format.lowercase()}") }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Save converted file")
                                }
                            }
                        }
                    }
                }
                item {
                    Text(status, color = Muted, fontSize = 12.sp)
                    Text("More formats will be added in future builds. Some formats require dedicated conversion engines.", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ToolRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ArrowForward, null, tint = Muted)
        }
    }
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

