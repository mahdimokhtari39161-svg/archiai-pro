package com.archiaipro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.archiaipro.data.DesignEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

private data class PlanWall(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val kind: String)

@Composable
fun PlanEditorScreen(projectId: Long, initial: DesignEntity?, onBack: () -> Unit, onSave: (String) -> Unit) {
    val existing = remember(projectId, initial?.payloadJson) {
        runCatching { JSONObject(initial?.payloadJson ?: "{}") }.getOrDefault(JSONObject())
    }
    var length by remember(projectId, initial?.payloadJson) { mutableStateOf(existing.optString("length", "12")) }
    var width by remember(projectId, initial?.payloadJson) { mutableStateOf(existing.optString("width", "9")) }
    var tool by remember { mutableStateOf("دیوار") }
    var walls by remember(projectId, initial?.payloadJson) { mutableStateOf(parseWalls(initial?.payloadJson.orEmpty())) }
    var start by remember { mutableStateOf<Offset?>(null) }
    val roomL = (length.toFloatOrNull() ?: 12f).coerceAtLeast(0.1f)
    val roomW = (width.toFloatOrNull() ?: 9f).coerceAtLeast(0.1f)

    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8)).padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("بازگشت به پرونده پروژه") }
        Text("ویرایشگر پلان دوبعدی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF142238))
        Text("نقطه شروع و سپس نقطه پایان را روی شبکه لمس کنید. خطوط به شبکه ۵٪ می‌چسبند.", color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(length, { length = it }, label = { Text("طول (متر)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(width, { width = it }, label = { Text("عرض (متر)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("دیوار", "در", "پنجره").forEach { name ->
                FilterChip(selected = tool == name, onClick = { tool = name; start = null }, label = { Text(name) })
            }
        }
        Text(if (start == null) "نقطه شروع را انتخاب کنید" else "نقطه پایان را انتخاب کنید", color = Color(0xFFB08A4D))
        Canvas(
            Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .pointerInput(tool, walls, start, roomL, roomW) {
                    detectTapGestures { point ->
                        val nx = (point.x / size.width).coerceIn(0f, 1f)
                        val ny = (point.y / size.height).coerceIn(0f, 1f)
                        val snapped = Offset((nx*20).toInt()/20f, (ny*20).toInt()/20f)
                        val first = start
                        if (first == null) start = snapped else {
                            if (hypot((snapped.x-first.x).toDouble(), (snapped.y-first.y).toDouble()) > 0.015) {
                                walls = walls + PlanWall(first.x, first.y, snapped.x, snapped.y, tool)
                            }
                            start = null
                        }
                    }
                }
        ) {
            val stepX = size.width/20f; val stepY = size.height/20f
            for (i in 0..20) {
                drawLine(Color(0xFFE7EBEF), Offset(i*stepX,0f), Offset(i*stepX,size.height), 1f)
                drawLine(Color(0xFFE7EBEF), Offset(0f,i*stepY), Offset(size.width,i*stepY), 1f)
            }
            drawRect(Color(0xFF516175), style = Stroke(4f))
            walls.forEach { wall ->
                val color = when(wall.kind) { "در" -> Color(0xFFC58C40); "پنجره" -> Color(0xFF2F879F); else -> Color(0xFF25364B) }
                val a = Offset(wall.x1*size.width, wall.y1*size.height)
                val b = Offset(wall.x2*size.width, wall.y2*size.height)
                drawLine(color, a, b, if(wall.kind=="دیوار") 7f else 4f,
                    pathEffect = if(wall.kind=="دیوار") null else PathEffect.dashPathEffect(floatArrayOf(12f,7f)))
                drawCircle(color, 4f, a); drawCircle(color, 4f, b)
            }
            start?.let { drawCircle(Color(0xFFE4A548), 9f, Offset(it.x*size.width,it.y*size.height)) }
        }
        Text("اجزای ترسیم‌شده: ${walls.size}  •  مساحت فضا: ${"%.2f".format(roomL*roomW)} مترمربع", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if(walls.isNotEmpty()) walls = walls.dropLast(1); start = null }, modifier = Modifier.weight(1f)) { Text("واگرد") }
            OutlinedButton(onClick = { walls = emptyList(); start = null }, modifier = Modifier.weight(1f)) { Text("پاک‌کردن") }
            Button(onClick = { onSave(serialize(roomL,roomW,walls)) }, modifier = Modifier.weight(1f)) { Text("ذخیره پلان") }
        }
        Text("دیوار پررنگ، در قهوه‌ای و پنجره آبی نمایش داده می‌شود.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

private fun parseWalls(json: String): List<PlanWall> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONObject(json).optJSONArray("walls") ?: JSONArray()
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            PlanWall(o.optDouble("x1").toFloat(),o.optDouble("y1").toFloat(),
                o.optDouble("x2").toFloat(),o.optDouble("y2").toFloat(),o.optString("kind","دیوار"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun serialize(length: Float, width: Float, walls: List<PlanWall>): String {
    val array = JSONArray()
    walls.forEach { w -> array.put(JSONObject().put("x1",w.x1).put("y1",w.y1).put("x2",w.x2).put("y2",w.y2).put("kind",w.kind)) }
    return JSONObject().put("length",length).put("width",width).put("walls",array).toString()
}
