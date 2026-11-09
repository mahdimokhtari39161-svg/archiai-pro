
package com.archiaipro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF142238)
private val NavyLight = Color(0xFF243B59)
private val Gold = Color(0xFFD8B477)
private val Background = Color(0xFFF3F5F8)
private val Ink = Color(0xFF1C293B)
private val Muted = Color(0xFF8290A2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                ArchiAIApp()
            }
        }
    }
}

@Composable
fun ArchiAIApp() {
    var selectedFeature by remember { mutableStateOf<String?>(null) }

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
            if (selectedFeature == null) {
                DashboardScreen(
                    onOpenFeature = { selectedFeature = it }
                )
            } else {
                FeatureScreen(
                    title = selectedFeature ?: "",
                    onBack = { selectedFeature = null }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(onOpenFeature: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ArchiAI",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "PRO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }
                Text(
                    text = "استودیوی هوشمند معماری",
                    fontSize = 12.sp,
                    color = Muted
                )
            }

            Surface(
                color = Navy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        color = Gold,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Hero panel
        Surface(
            color = Navy,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "طراحی معماری،\nبا دیدی فراتر از امروز",
                    fontSize = 23.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "فضا را خلق کن، متریال را انتخاب کن و ایده‌هایت را به پروژه تبدیل کن.",
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFFD0D8E3)
                )

                Spacer(Modifier.height(19.dp))

                Button(
                    onClick = { onOpenFeature("طراحی نمای سه‌بعدی") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Navy
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(
                        text = "شروع طراحی",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("←")
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "OFFLINE FIRST  •  ARCHITECTURE STUDIO",
                    fontSize = 9.sp,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Projects section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "فضای کاری من",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "مدیریت پروژه‌ها",
                fontSize = 11.sp,
                color = Muted
            )
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenFeature("مدیریت پروژه‌ها")
                }
        ) {
            Row(
                modifier = Modifier.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF1E8D9),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("▤", fontSize = 25.sp, color = Navy)
                    }
                }

                Spacer(Modifier.width(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "پروژه‌های معماری",
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ایجاد، ذخیره و مدیریت طرح‌ها",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }

                Text("←", fontSize = 19.sp, color = Gold)
            }
        }

        Spacer(Modifier.height(25.dp))

        // Tools section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ابزارهای تخصصی",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "۸ ابزار",
                fontSize = 11.sp,
                color = Muted
            )
        }

        Spacer(Modifier.height(13.dp))

        // First row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(
                title = "طراحی نمای سه‌بعدی",
                subtitle = "مدل‌سازی و انتخاب سنگ",
                symbol = "◇",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("طراحی نمای سه‌بعدی") }

            FeatureCard(
                title = "طراحی داخلی",
                subtitle = "چیدمان و دکوراسیون",
                symbol = "⌂",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("طراحی داخلی") }
        }

        Spacer(Modifier.height(12.dp))

        // Second row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(
                title = "کتابخانه متریال",
                subtitle = "سنگ، چوب، شیشه و...",
                symbol = "▧",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("کتابخانه متریال") }

            FeatureCard(
                title = "پلان و اندازه‌گذاری",
                subtitle = "ترسیم و اندازه‌ها",
                symbol = "▦",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("پلان و اندازه‌گذاری") }
        }

        Spacer(Modifier.height(12.dp))

        // Third row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(
                title = "محاسبات مصالح",
                subtitle = "مساحت و حجم",
                symbol = "∑",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("محاسبات مصالح") }

            FeatureCard(
                title = "برآورد هزینه",
                subtitle = "مدیریت هزینه پروژه",
                symbol = "₮",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("برآورد هزینه") }
        }

        Spacer(Modifier.height(12.dp))

        // Fourth row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(
                title = "گزارش و خروجی",
                subtitle = "PDF و تصاویر",
                symbol = "▤",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("گزارش و خروجی") }

            FeatureCard(
                title = "دستیار هوشمند",
                subtitle = "ایده‌پردازی معماری",
                symbol = "✦",
                modifier = Modifier.weight(1f)
            ) { onOpenFeature("دستیار هوشمند") }
        }

        Spacer(Modifier.height(24.dp))

        // Footer
        Surface(
            color = Color(0xFFE9EDF3),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ARCHIAI PRO",
                    color = Navy,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "استودیوی طراحی حرفه‌ای معماری",
                    fontSize = 11.sp,
                    color = Muted
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(19.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .heightIn(min = 140.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                color = Color(0xFFF1E9DC),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = symbol,
                        fontSize = 24.sp,
                        color = Navy
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Muted
            )
        }
    }
}

@Composable
fun FeatureScreen(
    title: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("بازگشت به داشبورد")
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = title,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Spacer(Modifier.height(14.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "به فضای کاری $title خوش آمدید.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "ساختار این بخش آماده است. ابزارها و امکانات تخصصی آن در مراحل بعدی به برنامه اضافه خواهند شد.",
                    fontSize = 13.sp,
                    lineHeight = 23.sp,
                    color = Muted
                )
            }
        }
    }
}
