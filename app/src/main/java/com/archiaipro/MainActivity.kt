package com.archiaipro

import android.content.Intent
import android.content.Context
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiaipro.data.ArchiDatabase
import com.archiaipro.backup.BackupScreen
import com.archiaipro.data.DesignEntity
import com.archiaipro.data.ProjectEntity
import com.archiaipro.reports.shareProjectPdf
import com.archiaipro.threeD.Facade3DView
import com.archiaipro.ui.PlanEditorScreen
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val Navy = Color(0xFF142238)
private val Gold = Color(0xFFD8B477)
private val Background = Color(0xFFF3F5F8)
private val Ink = Color(0xFF1C293B)
private val Muted = Color(0xFF8290A2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Rtl
            ) { ArchiAIApp() }
        }
    }
}

@Composable
fun ArchiAIApp() {
    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences("archiai_settings", Context.MODE_PRIVATE) }
    var language by remember { mutableStateOf(settings.getString("language", "fa") ?: "fa") }
    val database = remember { ArchiDatabase.get(context) }
    val dao = database.projectDao()
    val designDao = database.designDao()
    val projects by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf("خانه") }
    var activeProjectId by remember { mutableStateOf<Long?>(null) }
    val activeProject = projects.firstOrNull { it.id == activeProjectId }
    val designId = activeProjectId ?: -1L
    val planDesign by designDao.observe(designId, "plan").collectAsState(initial = null)
    val facadeDesign by designDao.observe(designId, "facade").collectAsState(initial = null)
    val interiorDesign by designDao.observe(designId, "interior").collectAsState(initial = null)

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Navy, secondary = Gold, background = Background, surface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Background) {
            when (page) {
                "خانه" -> DashboardScreen(projects.size, language) { destination ->
                    when (destination) {
                        "زبان برنامه" -> page = "زبان برنامه"
                        "پشتیبان‌گیری" -> page = "پشتیبان‌گیری"
                        "طراحی نمای سه‌بعدی", "طراحی داخلی", "پلان و اندازه‌گذاری" -> {
                            if (activeProjectId == null) activeProjectId = projects.firstOrNull()?.id
                            page = if (projects.isEmpty()) "مدیریت پروژه‌ها" else destination
                        }
                        else -> page = destination
                    }
                }
                "زبان برنامه" -> LanguageSettingsScreen(
                    language = language,
                    onSelect = { selected ->
                        language = selected
                        settings.edit().putString("language", selected).apply()
                    },
                    onBack = { page = "خانه" }
                )
                "پشتیبان‌گیری" -> BackupScreen(
                    projects = dao,
                    designs = designDao,
                    onBack = { page = "خانه" }
                )
                "مدیریت پروژه‌ها" -> ProjectsScreen(
                    projects = projects,
                    onBack = { page = "خانه" },
                    onSave = { project -> scope.launch {
                        if (project.id == 0L) dao.insert(project) else dao.update(project)
                    } },
                    onDelete = { project -> scope.launch { dao.delete(project) } },
                    onOpen = { project -> activeProjectId = project.id; page = "پرونده پروژه" }
                )
                "پرونده پروژه" -> activeProject?.let { project ->
                    ProjectWorkspaceScreen(
                        project = project,
                        onBack = { page = "مدیریت پروژه‌ها" },
                        onPlan = { page = "پلان و اندازه‌گذاری" },
                        onFacade = { page = "طراحی نمای سه‌بعدی" },
                        onInterior = { page = "طراحی داخلی" },
                        onReport = { page = "گزارش و خروجی" }
                    )
                } ?: run { page = "مدیریت پروژه‌ها" }
                "پلان و اندازه‌گذاری" -> activeProject?.let { project ->
                    PlanEditorScreen(
                        projectId = project.id,
                        initial = planDesign,
                        onBack = { page = "پرونده پروژه" },
                        onSave = { json -> scope.launch {
                            designDao.save(DesignEntity(
                                projectId = project.id, kind = "plan",
                                title = "پلان ${project.name}", payloadJson = json
                            ))
                        } }
                    )
                } ?: run { page = "مدیریت پروژه‌ها" }
                "طراحی نمای سه‌بعدی" -> activeProject?.let { project ->
                    FacadeDesignerScreen(
                        project = project,
                        initial = facadeDesign,
                        onBack = { page = "پرونده پروژه" },
                        onSave = { json -> scope.launch {
                            designDao.save(DesignEntity(
                                projectId = project.id, kind = "facade",
                                title = "نمای ${project.name}", payloadJson = json
                            ))
                        } }
                    )
                } ?: run { page = "مدیریت پروژه‌ها" }
                "طراحی داخلی" -> activeProject?.let { project ->
                    InteriorDesignerScreen(
                        project = project,
                        initial = interiorDesign,
                        onBack = { page = "پرونده پروژه" },
                        onSave = { json -> scope.launch {
                            designDao.save(DesignEntity(
                                projectId = project.id, kind = "interior",
                                title = "طراحی داخلی ${project.name}", payloadJson = json
                            ))
                        } }
                    )
                } ?: run { page = "مدیریت پروژه‌ها" }
                "محاسبات مصالح", "برآورد هزینه" -> CalculatorScreen(page) { page = "خانه" }
                "کتابخانه متریال" -> MaterialsScreen { page = "خانه" }
                "گزارش و خروجی" -> PdfReportScreen(projects) {
                    page = if (activeProject != null) "پرونده پروژه" else "خانه"
                }
                "دستیار هوشمند" -> AssistantScreen { page = "خانه" }
                else -> DashboardScreen(projects.size, language) { page = it }
            }
        }
    }
}

@Composable
private fun PageFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        OutlinedButton(onClick = onBack) { Text("بازگشت") }
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun DashboardScreen(projectCount: Int, language: String = "fa", onOpen: (String) -> Unit) {
    val english = language == "en"
    val arabic = language == "ar"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ArchiAI", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                    Spacer(Modifier.width(6.dp))
                    Text("PRO", color = Gold, fontWeight = FontWeight.Bold)
                }
                Text(if (english) "Professional architecture studio" else if (arabic) "استوديو معماري احترافي" else "استودیوی هوشمند معماری", color = Muted, fontSize = 12.sp)
            }
            Surface(color = Navy, shape = RoundedCornerShape(15.dp), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("A", color = Gold, fontSize = 29.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(22.dp))
        Surface(color = Navy, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text(if (english) "Turn ideas into projects" else if (arabic) "حوّل أفكارك إلى مشاريع" else "ایده را به پروژه تبدیل کن", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("ابزارهای طراحی، محاسبه و مدیریت پروژه؛ با ذخیره‌سازی آفلاین روی دستگاه.", color = Color(0xFFD0D8E3), fontSize = 13.sp, lineHeight = 23.sp)
                Spacer(Modifier.height(15.dp))
                Button(onClick = { onOpen("مدیریت پروژه‌ها") }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy)) {
                    Text("مدیریت پروژه‌ها  •  $projectCount", fontWeight = FontWeight.Bold)
                }
                Text("OFFLINE WORKSPACE", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (english) "Workspace" else if (arabic) "مساحة العمل" else "فضای کاری", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        CardAction("مدیریت پروژه‌ها", "ایجاد، ویرایش و نگهداری پروژه‌ها", "▤") { onOpen("مدیریت پروژه‌ها") }
        Spacer(Modifier.height(20.dp))
        Text(if (english) "Design tools" else if (arabic) "أدوات التصميم" else "ابزارهای تخصصی", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        val tools = listOf(
            "طراحی نمای سه‌بعدی" to "پیش‌نمایش نما و انتخاب متریال",
            "طراحی داخلی" to "چیدمان اولیه فضای داخلی",
            "کتابخانه متریال" to "مرجع متریال‌های ساختمانی",
            "پلان و اندازه‌گذاری" to "ابعاد و محاسبه مساحت فضا",
            "محاسبات مصالح" to "مقدار مصالح و پرت اجرا",
            "برآورد هزینه" to "محاسبه هزینه بر اساس نرخ ورودی",
            "گزارش و خروجی" to "ساخت گزارش قابل اشتراک",
            "دستیار هوشمند" to "راهنمای مراحل طراحی",
            "زبان برنامه" to "فارسی، عربی و انگلیسی",
            "پشتیبان‌گیری" to "خروجی و بازیابی فایل پروژه"
        )
        tools.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    Surface(color = Color.White, shape = RoundedCornerShape(17.dp), modifier = Modifier.weight(1f).height(116.dp).clickable { onOpen(item.first) }) {
                        Column(Modifier.padding(13.dp)) {
                            Text("◇", color = Gold, fontSize = 24.sp)
                            Text(item.first, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(item.second, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Text("ArchiAI Pro  •  ابزارهای پایه بدون نیاز به اینترنت", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun CardAction(title: String, subtitle: String, symbol: String, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, fontSize = 30.sp, color = Gold)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Ink)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Text("←", color = Gold, fontSize = 22.sp)
        }
    }
}

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>, onBack: () -> Unit, onSave: (ProjectEntity) -> Unit,
    onDelete: (ProjectEntity) -> Unit, onOpen: (ProjectEntity) -> Unit
) {
    var editing by remember { mutableStateOf<ProjectEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProjectEntity?>(null) }
    PageFrame("مدیریت پروژه‌ها", onBack) {
        Button(onClick = { editing = null; showDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("+ پروژه جدید") }
        Spacer(Modifier.height(12.dp))
        if (projects.isEmpty()) {
            Text("هنوز پروژه‌ای ثبت نشده است. اولین پروژه را ایجاد کنید.", color = Muted)
        } else {
            projects.forEach { p ->
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Text(p.name, color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${p.type}  •  ${p.area} مترمربع", color = Muted, fontSize = 12.sp)
                        if (p.location.isNotBlank()) Text(p.location, color = Muted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpen(p) }) { Text("بازکردن") }
                            TextButton(onClick = { editing = p; showDialog = true }) { Text("ویرایش") }
                            TextButton(onClick = { deleting = p }) { Text("حذف", color = Color.Red) }
                        }
                    }
                }
            }
        }
    }
    if (showDialog) ProjectEditor(editing, { showDialog = false }) { onSave(it); showDialog = false }
    deleting?.let { p ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("حذف پروژه") },
            text = { Text("پروژه «${p.name}» حذف شود؟") },
            confirmButton = { TextButton(onClick = { onDelete(p); deleting = null }) { Text("حذف", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } })
    }
}

@Composable
private fun ProjectEditor(project: ProjectEntity?, onDismiss: () -> Unit, onSave: (ProjectEntity) -> Unit) {
    var name by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var type by remember(project?.id) { mutableStateOf(project?.type ?: "مسکونی") }
    var location by remember(project?.id) { mutableStateOf(project?.location ?: "") }
    var area by remember(project?.id) { mutableStateOf(if (project == null) "" else project.area.toString()) }
    var description by remember(project?.id) { mutableStateOf(project?.description ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (project == null) "پروژه جدید" else "ویرایش پروژه") },
        text = { Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { name = it }, label = { Text("نام پروژه") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Text("نوع پروژه")
            listOf("مسکونی", "تجاری", "اداری", "ویلایی", "طراحی داخلی", "سایر").forEach {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = it }) {
                    RadioButton(type == it, { type = it }); Text(it)
                }
            }
            OutlinedTextField(location, { location = it }, label = { Text("موقعیت / آدرس") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(area, { area = it }, label = { Text("زیربنا (مترمربع)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        } },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = {
            onSave(ProjectEntity(id = project?.id ?: 0, name = name.trim(), type = type, location = location.trim(),
                area = area.toDoubleOrNull() ?: 0.0, description = description.trim(),
                createdAt = project?.createdAt ?: System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
fun CalculatorScreen(title: String, onBack: () -> Unit) {
    var length by remember { mutableStateOf("10") }
    var width by remember { mutableStateOf("8") }
    var height by remember { mutableStateOf("3") }
    var openings by remember { mutableStateOf("12") }
    var waste by remember { mutableStateOf("10") }
    var unitPrice by remember { mutableStateOf("0") }
    val area = ((length.toDoubleOrNull() ?: 0.0) * (height.toDoubleOrNull() ?: 0.0) * 2 +
        (width.toDoubleOrNull() ?: 0.0) * (height.toDoubleOrNull() ?: 0.0) * 2 -
        (openings.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
    val quantity = area * (1 + ((waste.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) / 100))
    val total = quantity * (unitPrice.toDoubleOrNull() ?: 0.0)
    PageFrame(title, onBack) {
        Text("ابعاد را بر حسب متر وارد کنید. این محاسبه، برآورد اولیه سطوح چهار دیوار است.", color = Muted, fontSize = 12.sp)
        NumberField("طول فضا (متر)", length) { length = it }
        NumberField("عرض فضا (متر)", width) { width = it }
        NumberField("ارتفاع دیوار (متر)", height) { height = it }
        NumberField("مجموع بازشوها (مترمربع)", openings) { openings = it }
        NumberField("درصد پرت مصالح", waste) { waste = it }
        NumberField("قیمت هر مترمربع (اختیاری)", unitPrice) { unitPrice = it }
        Spacer(Modifier.height(15.dp))
        ResultTile("مساحت خالص دیوارها", "${fmt(area)} مترمربع")
        ResultTile("مقدار با احتساب پرت", "${fmt(quantity)} مترمربع")
        ResultTile("برآورد هزینه", "${fmt(total)}")
        Text("توجه: این خروجی برآورد مقدماتی است؛ شکستگی، قرنیز، ابزار، زیرسازی و جزئیات اجرایی جداگانه محاسبه شوند.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
}

private fun fmt(value: Double): String = NumberFormat.getNumberInstance(Locale("fa", "IR")).apply { maximumFractionDigits = 2 }.format(value)

@Composable
private fun ResultTile(label: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 9.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Muted, fontSize = 12.sp)
            Text(value, color = Navy, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MaterialsScreen(onBack: () -> Unit) {
    val groups = listOf(
        "سنگ طبیعی" to listOf("تراورتن سفید", "تراورتن کرم", "مرمریت", "گرانیت", "چینی / کریستال", "اسلب بوک‌مچ"),
        "پوشش و کف" to listOf("سرامیک پرسلان", "موزاییک", "پارکت", "لمینت", "میکروسمنت"),
        "چوب و فلز" to listOf("چوب طبیعی", "ترمووود", "MDF", "آلومینیوم", "فولاد رنگ‌شده"),
        "شفاف و رنگ" to listOf("شیشه سکوریت", "شیشه لمینیت", "رنگ اکریلیک", "رنگ روغنی", "پوشش میکروتکسچر")
    )
    PageFrame("کتابخانه متریال", onBack) {
        Text("فهرست پایه برای انتخاب متریال. مشخصات فنی و قیمت هر محصول باید با کاتالوگ سازنده تکمیل شود.", color = Muted, fontSize = 12.sp)
        groups.forEach { group ->
            Text(group.first, color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(7.dp))
            group.second.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("◆", color = Gold); Spacer(Modifier.width(10.dp)); Text(item, color = Ink)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun FacadeScreen(onBack: () -> Unit) {
    val palette = listOf(Color(0xFFE9E3D8), Color(0xFFB9A58D), Color(0xFF777C82), Color(0xFF3F4A54), Color(0xFFD8C6A5))
    val labels = listOf("سفید روشن", "کرم", "طوسی", "ذغالی", "بژ")
    var selected by remember { mutableIntStateOf(0) }
    var part by remember { mutableIntStateOf(0) }
    PageFrame("طراحی نمای ساختمان", onBack) {
        Text("روی هر قسمت نما بزنید و سپس متریال آن قسمت را تغییر دهید.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.fillMaxWidth().height(310.dp).background(Color(0xFFE7ECF1), RoundedCornerShape(16.dp))
            .clickable { part = (part + 1) % 5 }) {
            val w = size.width; val h = size.height
            drawRect(Color(0xFFBAC3CC), Offset(w*.12f,h*.12f), Size(w*.76f,h*.78f))
            drawRect(palette[if(part==0) selected else 0], Offset(w*.16f,h*.17f), Size(w*.68f,h*.68f))
            drawRect(Color(0xFF26364A), Offset(w*.43f,h*.55f), Size(w*.14f,h*.30f))
            drawRect(Color(0xFF8DA7B8), Offset(w*.22f,h*.27f), Size(w*.15f,h*.17f))
            drawRect(Color(0xFF8DA7B8), Offset(w*.63f,h*.27f), Size(w*.15f,h*.17f))
            drawRect(palette[if(part==1) selected else 1], Offset(w*.12f,h*.12f), Size(w*.09f,h*.78f))
            drawRect(palette[if(part==2) selected else 1], Offset(w*.79f,h*.12f), Size(w*.09f,h*.78f))
            drawRect(palette[if(part==3) selected else 2], Offset(w*.16f,h*.17f), Size(w*.68f,h*.07f))
            drawRect(palette[if(part==4) selected else 2], Offset(w*.16f,h*.78f), Size(w*.68f,h*.07f))
        }
        Spacer(Modifier.height(10.dp))
        Text("بخش انتخاب‌شده: ${listOf("بدنه اصلی", "ستون چپ", "ستون راست", "تاج نما", "پایین نما")[part]}", color = Navy, fontWeight = FontWeight.Bold)
        Text("انتخاب متریال", color = Ink, fontWeight = FontWeight.Bold)
        labels.forEachIndexed { index, label ->
            Row(Modifier.fillMaxWidth().clickable { selected = index }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == index, { selected = index })
                Box(Modifier.size(25.dp).background(palette[index], RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(9.dp)); Text(label)
            }
        }
        Text("این نسخه یک پیش‌نمایش دوبعدی پارامتریک است؛ مدل سه‌بعدی واقعی و ذخیره طرح نما در مرحله توسعه بعدی اضافه می‌شود.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun InteriorScreen(onBack: () -> Unit) {
    var room by remember { mutableStateOf("نشیمن") }
    var floor by remember { mutableStateOf("چوب روشن") }
    var wall by remember { mutableStateOf("سفید گرم") }
    PageFrame("طراحی داخلی", onBack) {
        Text("فضا", fontWeight = FontWeight.Bold)
        listOf("نشیمن", "خواب", "آشپزخانه", "اداری").forEach { Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(room == it, { room = it }); Text(it)
        } }
        Text("رنگ دیوار")
        listOf("سفید گرم", "طوسی روشن", "کرم", "سبز ملایم").forEach { TextButton(onClick = { wall = it }) { Text(if(wall==it) "✓ $it" else it) } }
        Text("کف‌پوش")
        listOf("چوب روشن", "سنگ کرم", "سرامیک طوسی", "پارکت تیره").forEach { TextButton(onClick = { floor = it }) { Text(if(floor==it) "✓ $it" else it) } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
            Text("پیش‌نمایش ترکیب", color = Navy, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(135.dp).background(when(wall) {
                "طوسی روشن" -> Color(0xFFD7DADF); "کرم" -> Color(0xFFE8D8BC); "سبز ملایم" -> Color(0xFFD3E0D2); else -> Color(0xFFF2EFE8)
            })) {
                Box(Modifier.fillMaxWidth().height(32.dp).align(Alignment.BottomCenter).background(when(floor) {
                    "سنگ کرم" -> Color(0xFFD0C1A7); "سرامیک طوسی" -> Color(0xFF9FA6AE); "پارکت تیره" -> Color(0xFF70513B); else -> Color(0xFFB58A61)
                }))
            }
            Text("$room  •  دیوار: $wall  •  کف: $floor", color = Muted, fontSize = 11.sp)
        } }
        Text("چیدمان مبلمان و مدل سه‌بعدی در فاز بعدی توسعه افزوده می‌شود.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun PlanScreen(onBack: () -> Unit) {
    var length by remember { mutableStateOf("12") }
    var width by remember { mutableStateOf("9") }
    val area = (length.toDoubleOrNull() ?: 0.0) * (width.toDoubleOrNull() ?: 0.0)
    PageFrame("پلان و اندازه‌گذاری", onBack) {
        NumberField("طول پلان (متر)", length) { length = it }
        NumberField("عرض پلان (متر)", width) { width = it }
        ResultTile("مساحت مستطیل", "${fmt(area)} مترمربع")
        Canvas(Modifier.fillMaxWidth().height(250.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp)) {
            val scale = minOf(size.width / 14f, size.height / 11f)
            val x = (size.width - (length.toFloatOrNull() ?: 0f) * scale) / 2
            val y = (size.height - (width.toFloatOrNull() ?: 0f) * scale) / 2
            drawRect(Color(0xFF35465C), Offset(x,y), Size((length.toFloatOrNull() ?: 0f)*scale,(width.toFloatOrNull() ?: 0f)*scale), style = androidx.compose.ui.graphics.drawscope.Stroke(width=4f))
        }
        Text("در این نسخه ترسیم و اندازه‌گذاری پایه مستطیلی فعال است؛ ابزار ترسیم دیوار، در و پنجره در توسعه بعدی تکمیل می‌شود.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun ReportScreen(projects: List<ProjectEntity>, onBack: () -> Unit) {
    val context = LocalContext.current
    PageFrame("گزارش و خروجی", onBack) {
        Text("گزارش خلاصه پروژه‌ها را می‌توانید با برنامه‌های موجود در گوشی به اشتراک بگذارید.", color = Muted)
        Spacer(Modifier.height(12.dp))
        val report = buildString {
            append("گزارش ArchiAI Pro\nتعداد پروژه‌ها: ${projects.size}\n\n")
            projects.forEachIndexed { i, p -> append("${i+1}. ${p.name}\nنوع: ${p.type}\nزیربنا: ${p.area} مترمربع\nموقعیت: ${p.location}\nتوضیحات: ${p.description}\n\n") }
        }
        ResultTile("پروژه‌های ثبت‌شده", projects.size.toString())
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "گزارش ArchiAI Pro"); putExtra(Intent.EXTRA_TEXT, report) }
            context.startActivity(Intent.createChooser(intent, "اشتراک گزارش"))
        }, modifier = Modifier.fillMaxWidth()) { Text("اشتراک گزارش متنی") }
        Text("خروجی PDF صفحه‌آرایی‌شده در نسخه بعدی اضافه می‌شود.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun AssistantScreen(onBack: () -> Unit) {
    var question by remember { mutableStateOf("") }
    PageFrame("دستیار معماری", onBack) {
        Text("راهنمای آفلاین اولیه برای شروع طراحی. پاسخ‌های مولد هوش مصنوعی و اتصال آنلاین هنوز فعال نیست.", color = Muted)
        OutlinedTextField(question, { question = it }, label = { Text("موضوع یا پرسش خود را بنویسید") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Spacer(Modifier.height(10.dp))
        Button(onClick = { question = "" }, enabled = question.isNotBlank()) { Text("پاک‌کردن پرسش") }
        Text("برای طراحی دقیق، ابتدا نوع ساختمان، اقلیم، ابعاد، بودجه و نیازهای کارفرما را مشخص کنید. سپس پلان، سازه، پوسته و تأسیسات را هماهنگ پیش ببرید.", color = Ink, lineHeight = 25.sp)
    }
}

@Composable
private fun ProjectWorkspaceScreen(
    project: ProjectEntity,
    onBack: () -> Unit,
    onPlan: () -> Unit,
    onFacade: () -> Unit,
    onInterior: () -> Unit,
    onReport: () -> Unit
) {
    PageFrame("پرونده پروژه", onBack) {
        Text(project.name, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("${project.type}  •  ${project.area} مترمربع", color = Muted)
        if (project.location.isNotBlank()) Text(project.location, color = Muted)
        if (project.description.isNotBlank()) Text(project.description, color = Ink)
        Spacer(Modifier.height(12.dp))
        WorkspaceAction("ویرایشگر پلان دوبعدی", "ترسیم دیوار، در و پنجره و ذخیره در همین پروژه", onPlan)
        WorkspaceAction("مدل سه‌بعدی نمای ساختمان", "چرخاندن مدل و تغییر متریال اجزای نما", onFacade)
        WorkspaceAction("طراحی داخلی", "انتخاب فضای اتاق، رنگ دیوار و کف‌پوش", onInterior)
        WorkspaceAction("گزارش PDF", "خروجی پرونده پروژه‌ها", onReport)
    }
}

@Composable
private fun WorkspaceAction(title: String, detail: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = Navy, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FacadeDesignerScreen(
    project: ProjectEntity,
    initial: DesignEntity?,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val model = remember { Facade3DView(context) }
    val sections = listOf("بدنه اصلی", "ستون چپ", "ستون راست", "تاج نما", "پایین نما")
    val materials = listOf("سفید", "کرم", "طوسی", "ذغالی", "بژ")
    var section by remember { mutableIntStateOf(0) }
    var material by remember { mutableIntStateOf(0) }
    LaunchedEffect(initial?.payloadJson) {
        val saved = runCatching { JSONObject(initial?.payloadJson ?: "{}") }.getOrNull()
        if (saved != null) {
            for (i in 0..4) model.setSectionMaterial(i, saved.optInt("color$i", 0))
            material = saved.optInt("color0", 0)
        }
    }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        OutlinedButton(onClick = onBack) { Text("بازگشت به پرونده پروژه") }
        Text("مدل سه‌بعدی نمای ساختمان", color = Navy, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        Text(project.name, color = Muted)
        AndroidView(factory = { model }, modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp))
        Text("مدل را با کشیدن انگشت بچرخانید.", color = Muted, fontSize = 11.sp)
        Text("بخش نما", color = Ink, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            sections.forEachIndexed { i, label ->
                FilterChip(selected = section == i, onClick = { section = i }, label = { Text(label, fontSize = 9.sp) })
            }
        }
        Text("متریال", color = Ink, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            materials.forEachIndexed { i, label ->
                FilterChip(selected = material == i, onClick = {
                    material = i
                    model.setSectionMaterial(section, i)
                }, label = { Text(label, fontSize = 10.sp) })
            }
        }
        Button(onClick = {
            val json = JSONObject()
            for (i in 0..4) {
                val savedColor = runCatching { JSONObject(initial?.payloadJson ?: "{}").optInt("color$i", 0) }.getOrDefault(0)
                json.put("color$i", if (i == section) material else savedColor)
            }
            onSave(json.toString())
        }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره طرح نما در پروژه") }
        Text("مدل حجمی سه‌بعدی و قابل چرخش است. بافت تصویری سنگ و جزئیات اجرایی پیشرفته هنوز تکمیل نشده‌اند.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun InteriorDesignerScreen(
    project: ProjectEntity,
    initial: DesignEntity?,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    val saved = remember(initial?.payloadJson) {
        runCatching { JSONObject(initial?.payloadJson ?: "{}") }.getOrDefault(JSONObject())
    }
    var room by remember(initial?.payloadJson) { mutableStateOf(saved.optString("room", "نشیمن")) }
    var wall by remember(initial?.payloadJson) { mutableStateOf(saved.optString("wall", "سفید گرم")) }
    var floor by remember(initial?.payloadJson) { mutableStateOf(saved.optString("floor", "چوب روشن")) }
    var furniture by remember(initial?.payloadJson) { mutableStateOf(saved.optBoolean("furniture", true)) }
    val wallColors = mapOf(
        "سفید گرم" to Color(0xFFF0ECE4), "طوسی روشن" to Color(0xFFD4D8DC),
        "کرم" to Color(0xFFE9D7B9), "سبز ملایم" to Color(0xFFD4E0D2),
        "آبی خاکستری" to Color(0xFFD5E0E8)
    )
    val floorColors = mapOf(
        "چوب روشن" to Color(0xFFB78C62), "سنگ کرم" to Color(0xFFD0C3AE),
        "سرامیک طوسی" to Color(0xFF9DA5AD), "پارکت تیره" to Color(0xFF60432F)
    )
    PageFrame("طراحی داخلی پروژه", onBack) {
        Text(project.name, color = Muted)
        Text("فضای مورد نظر", color = Ink, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("نشیمن", "خواب", "آشپزخانه", "اداری").forEach {
                FilterChip(selected = room == it, onClick = { room = it }, label = { Text(it, fontSize = 10.sp) })
            }
        }
        Text("رنگ دیوار", color = Ink, fontWeight = FontWeight.Bold)
        listOf("سفید گرم", "طوسی روشن", "کرم", "سبز ملایم", "آبی خاکستری").forEach {
            FilterChip(selected = wall == it, onClick = { wall = it }, label = { Text(it) })
        }
        Text("کف‌پوش", color = Ink, fontWeight = FontWeight.Bold)
        listOf("چوب روشن", "سنگ کرم", "سرامیک طوسی", "پارکت تیره").forEach {
            FilterChip(selected = floor == it, onClick = { floor = it }, label = { Text(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = furniture, onCheckedChange = { furniture = it })
            Text("نمایش مبلمان شماتیک")
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("پیش‌نمایش اتاق", color = Navy, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth().height(190.dp).background(wallColors[wall] ?: Color.White)) {
                    Box(Modifier.fillMaxWidth().height(42.dp).align(Alignment.BottomCenter).background(floorColors[floor] ?: Color.LightGray))
                    if (furniture) {
                        Box(Modifier.width(125.dp).height(42.dp).align(Alignment.BottomCenter).padding(bottom = 5.dp).background(Color(0xFF9A7658), RoundedCornerShape(8.dp)))
                        Box(Modifier.width(38.dp).height(70.dp).align(Alignment.BottomStart).padding(start = 10.dp, bottom = 45.dp).background(Color(0xFF8A9AA5), RoundedCornerShape(5.dp)))
                    }
                }
                Text("$room  •  دیوار: $wall  •  کف: $floor", color = Muted, fontSize = 11.sp)
            }
        }
        Button(onClick = {
            onSave(JSONObject().put("room", room).put("wall", wall).put("floor", floor).put("furniture", furniture).toString())
        }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره طراحی داخلی") }
        Text("انتخاب‌ها در پرونده همین پروژه ذخیره می‌شوند؛ چیدمان دقیق و مدل سه‌بعدی مبلمان در توسعه بعدی قرار دارد.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun PdfReportScreen(projects: List<ProjectEntity>, onBack: () -> Unit) {
    val context = LocalContext.current
    PageFrame("گزارش PDF", onBack) {
        Text("گزارش PDF از مشخصات ثبت‌شده پروژه‌ها تولید می‌شود و می‌توانید آن را ذخیره یا ارسال کنید.", color = Muted)
        ResultTile("تعداد پروژه‌ها", projects.size.toString())
        Button(onClick = { shareProjectPdf(context, projects) }, modifier = Modifier.fillMaxWidth()) {
            Text("ساخت و اشتراک PDF")
        }
        Text("در حال حاضر گزارش شامل مشخصات اصلی پروژه‌هاست؛ نقشه‌ها و تصاویر طراحی در PDF قرار نمی‌گیرند.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun LanguageSettingsScreen(language: String, onSelect: (String) -> Unit, onBack: () -> Unit) {
    PageFrame("زبان برنامه", onBack) {
        Text("زبان صفحه اصلی برنامه را انتخاب کنید. انتخاب شما روی دستگاه ذخیره می‌شود.", color = Muted)
        listOf("fa" to "فارسی", "ar" to "العربية", "en" to "English").forEach { pair ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(pair.first) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = language == pair.first, onClick = { onSelect(pair.first) })
                Text(pair.second, color = Ink)
            }
        }
        Text("ترجمه صفحه اصلی فعال است؛ برخی فرم‌ها و ابزارهای تخصصی هنوز فارسی هستند.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun ProjectDetailScreen(title: String, projects: List<ProjectEntity>, onBack: () -> Unit, onReport: () -> Unit) {
    val project = projects.firstOrNull { it.name == title }
    PageFrame("پرونده پروژه", onBack) {
        Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (project != null) {
            ResultTile("نوع پروژه", project.type)
            ResultTile("زیربنا", "${project.area} مترمربع")
            ResultTile("موقعیت", project.location.ifBlank { "ثبت نشده" })
            Text(project.description)
        } else Text("اطلاعات پروژه در دسترس نیست.", color = Muted)
        Button(onClick = onReport) { Text("رفتن به گزارش پروژه‌ها") }
    }
}
