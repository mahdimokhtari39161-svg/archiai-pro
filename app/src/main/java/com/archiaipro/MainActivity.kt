
package com.archiaipro

import android.content.Context
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF142238)
private val Gold = Color(0xFFD8B477)
private val Background = Color(0xFFF3F5F8)
private val Ink = Color(0xFF1C293B)
private val Muted = Color(0xFF8290A2)

data class Project(
    val id: Long,
    val name: String,
    val type: String,
    val description: String
)

private fun loadProjects(context: Context): List<Project> {
    val prefs = context.getSharedPreferences("archiai_data", Context.MODE_PRIVATE)
    val result = mutableListOf<Project>()

    return try {
        val array = JSONArray(prefs.getString("projects", "[]"))
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Project(
                    id = item.optLong("id"),
                    name = item.optString("name"),
                    type = item.optString("type"),
                    description = item.optString("description")
                )
            )
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveProjects(context: Context, projects: List<Project>) {
    val array = JSONArray()

    projects.forEach { project ->
        val item = JSONObject()
        item.put("id", project.id)
        item.put("name", project.name)
        item.put("type", project.type)
        item.put("description", project.description)
        array.put(item)
    }

    context.getSharedPreferences("archiai_data", Context.MODE_PRIVATE)
        .edit()
        .putString("projects", array.toString())
        .apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Rtl
            ) {
                ArchiAIApp()
            }
        }
    }
}

@Composable
fun ArchiAIApp() {
    val context = LocalContext.current
    val projects = remember { mutableStateListOf<Project>() }

    var selectedFeature by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        projects.addAll(loadProjects(context))
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            secondary = Gold,
            background = Background,
            surface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {
            when (selectedFeature) {
                null -> DashboardScreen(
                    projectCount = projects.size,
                    onOpen = { selectedFeature = it }
                )

                "مدیریت پروژه‌ها" -> ProjectManagerScreen(
                    projects = projects,
                    onBack = { selectedFeature = null },
                    onSave = { project ->
                        val index = projects.indexOfFirst { it.id == project.id }
                        if (index >= 0) {
                            projects[index] = project
                        } else {
                            projects.add(project)
                        }
                        saveProjects(context, projects)
                    },
                    onDelete = { project ->
                        projects.removeAll { it.id == project.id }
                        saveProjects(context, projects)
                    }
                )

                else -> FeatureScreen(
                    title = selectedFeature ?: "",
                    onBack = { selectedFeature = null }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    projectCount: Int,
    onOpen: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ArchiAI",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("PRO", color = Gold, fontWeight = FontWeight.Bold)
                }
                Text("استودیوی هوشمند معماری", color = Muted, fontSize = 12.sp)
            }

            Surface(
                color = Navy,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("A", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Surface(
            color = Navy,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    "طراحی معماری،\nبا دیدی فراتر از امروز",
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "پروژه‌های معماری خود را ایجاد کن، ذخیره کن و توسعه بده.",
                    color = Color(0xFFD0D8E3),
                    fontSize = 13.sp,
                    lineHeight = 23.sp
                )
                Spacer(Modifier.height(17.dp))
                Button(
                    onClick = { onOpen("مدیریت پروژه‌ها") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Navy
                    )
                ) {
                    Text("شروع یک پروژه جدید", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "OFFLINE FIRST  •  ARCHITECTURE STUDIO",
                    color = Gold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("فضای کاری من", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen("مدیریت پروژه‌ها") }
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▤", fontSize = 30.sp, color = Gold)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("مدیریت پروژه‌ها", fontWeight = FontWeight.Bold, color = Ink)
                    Text("$projectCount پروژه ذخیره‌شده", color = Muted, fontSize = 12.sp)
                }
                Text("←", color = Gold, fontSize = 22.sp)
            }
        }

        Spacer(Modifier.height(25.dp))
        Text("ابزارهای تخصصی", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(12.dp))

        val features = listOf(
            "طراحی نمای سه‌بعدی" to "مدل‌سازی و انتخاب سنگ",
            "طراحی داخلی" to "چیدمان و دکوراسیون",
            "کتابخانه متریال" to "سنگ، چوب، شیشه و فلز",
            "پلان و اندازه‌گذاری" to "ترسیم و اندازه‌ها",
            "محاسبات مصالح" to "مساحت و حجم",
            "برآورد هزینه" to "مدیریت هزینه پروژه",
            "گزارش و خروجی" to "گزارش پروژه",
            "دستیار هوشمند" to "ایده‌پردازی معماری"
        )

        features.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { feature ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .clickable { onOpen(feature.first) }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("◇", color = Gold, fontSize = 25.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(feature.first, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(feature.second, color = Muted, fontSize = 10.sp)
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "ARCHIAI PRO • استودیوی طراحی حرفه‌ای معماری",
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun ProjectManagerScreen(
    projects: List<Project>,
    onBack: () -> Unit,
    onSave: (Project) -> Unit,
    onDelete: (Project) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var deletingProject by remember { mutableStateOf<Project?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("بازگشت")
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "مدیریت پروژه‌ها",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                editingProject = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Navy)
        ) {
            Text("+ ایجاد پروژه جدید", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        if (projects.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▤", fontSize = 48.sp, color = Gold)
                    Spacer(Modifier.height(12.dp))
                    Text("هنوز پروژه‌ای ثبت نشده است", color = Ink, fontWeight = FontWeight.Bold)
                    Text("برای شروع، پروژه جدید بسازید.", color = Muted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(projects, key = { it.id }) { project ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(project.name, fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("نوع ساختمان: ${project.type}", color = Muted, fontSize = 12.sp)
                            if (project.description.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(project.description, color = Ink, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingProject = project
                                        showDialog = true
                                    }
                                ) {
                                    Text("ویرایش")
                                }
                                OutlinedButton(
                                    onClick = { deletingProject = project },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ProjectDialog(
            project = editingProject,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }

    deletingProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            title = { Text("حذف پروژه") },
            text = { Text("آیا از حذف پروژه «${project.name}» مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(project)
                    deletingProject = null
                }) {
                    Text("حذف", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProject = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun ProjectDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit
) {
    var name by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var type by remember(project?.id) { mutableStateOf(project?.type ?: "مسکونی") }
    var description by remember(project?.id) { mutableStateOf(project?.description ?: "") }

    val types = listOf("مسکونی", "تجاری", "اداری", "ویلایی", "داخلی", "سایر")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project == null) "ایجاد پروژه" else "ویرایش پروژه") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام پروژه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text("نوع ساختمان", fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(6.dp))

                types.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { type = option }
                            .padding(vertical = 3.dp)
                    ) {
                        RadioButton(
                            selected = type == option,
                            onClick = { type = option }
                        )
                        Text(option, color = Ink)
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات پروژه") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Project(
                            id = project?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            type = type,
                            description = description.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun FeatureScreen(title: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("بازگشت به داشبورد")
        }
        Spacer(Modifier.height(25.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
        Spacer(Modifier.height(15.dp))
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("این ابزار در حال توسعه است.", fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "امکانات تخصصی این بخش در مراحل بعدی به برنامه اضافه خواهند شد.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 23.sp
                )
            }
        }
    }
}
