package com.archiaipro.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.archiaipro.data.DesignDao
import com.archiaipro.data.ProjectDao
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(projects: ProjectDao, designs: DesignDao, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                message = try {
                    val count = restoreBackup(context, uri, projects, designs)
                    "بازیابی انجام شد؛ $count پروژه پردازش شد."
                } catch (error: Exception) {
                    "بازیابی انجام نشد: ${error.message ?: "خطای ناشناخته"}"
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        OutlinedButton(onClick = onBack) { Text("بازگشت") }
        Text("پشتیبان‌گیری و بازیابی", style = MaterialTheme.typography.headlineSmall)
        Text("فایل پشتیبان شامل پرونده پروژه‌ها، پلان‌ها، نماها و تنظیمات طراحی داخلی است.")
        Spacer(Modifier.height(18.dp))
        Button(onClick = {
            scope.launch {
                message = try {
                    shareBackup(context, projects, designs)
                    "فایل پشتیبان آماده اشتراک‌گذاری است."
                } catch (error: Exception) {
                    "ساخت پشتیبان انجام نشد: ${error.message ?: "خطای ناشناخته"}"
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("ساخت فایل پشتیبان") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { picker.launch("application/json") }, modifier = Modifier.fillMaxWidth()) {
            Text("انتخاب فایل و بازیابی اطلاعات")
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Text("بازیابی، اطلاعات دارای شناسه یکسان را جایگزین می‌کند و پروژه‌های دیگر را حفظ می‌کند.", style = MaterialTheme.typography.bodySmall)
    }
}
