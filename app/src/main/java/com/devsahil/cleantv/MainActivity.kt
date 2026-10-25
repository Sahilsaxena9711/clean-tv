package com.devsahil.cleantv

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppEntry(val pkg: String, val label: String)

private val Bg = Color(0xFF07090D)
private val Card = Color(0xFF121722)
private val CardFocused = Color(0xFF1A2130)
private val TextHi = Color(0xFFF4F6FA)
private val TextLo = Color(0xFF8B94A3)
private val Violet = Color(0xFF8B7CFF)
private val Cyan = Color(0xFF4DD4E6)
private val Hairline = Color(0x14FFFFFF)
private val FocusBrush = Brush.linearGradient(listOf(Violet, Cyan))

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("cleantv", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Root() }
    }

    private fun loadApps(): List<AppEntry> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    private fun launch(pkg: String) {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.runningAppProcesses?.forEach { p ->
            p.pkgList.forEach { if (it != pkg && it != packageName) am.killBackgroundProcesses(it) }
        }
        val pm = packageManager
        val i = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
        if (i != null) startActivity(i)
        else Toast.makeText(this, "Can't open this app", Toast.LENGTH_SHORT).show()
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun freeRamGb(): String {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return String.format(Locale.getDefault(), "%.1f GB free", info.availMem / 1073741824.0)
    }

    @Composable
    private fun Root() {
        var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
        var favs by remember {
            mutableStateOf(prefs.getString("favs", "")!!.split(",").filter { it.isNotBlank() })
        }
        var showAll by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadApps() } }

        LaunchedEffect(apps) {
            if (apps.isNotEmpty() && favs.isEmpty() && !prefs.contains("favs")) {
                val wanted = listOf(
                    "com.google.android.youtube.tv", "com.netflix.ninja",
                    "com.amazon.amazonvideo.livingroom", "com.spotify.tv.android",
                    "in.startv.hotstar", "com.jio.jioplay.tv"
                )
                favs = wanted.filter { w -> apps.any { it.pkg == w } }.take(6)
                prefs.edit().putString("favs", favs.joinToString(",")).apply()
            }
        }

        fun toggleFav(pkg: String) {
            favs = if (pkg in favs) favs - pkg else favs + pkg
            prefs.edit().putString("favs", favs.joinToString(",")).apply()
        }

        BackHandler(enabled = showAll) { showAll = false }

        Box(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0x405B4DFF), Color.Transparent),
                            center = Offset(0f, 0f), radius = size.width * 0.65f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0x2E19B6C9), Color.Transparent),
                            center = Offset(size.width, size.height), radius = size.width * 0.6f
                        )
                    )
                }
                .padding(horizontal = 64.dp, vertical = 40.dp)
        ) {
            if (showAll) AllApps(apps, favs, ::toggleFav)
            else Home(apps, favs, ::toggleFav) { showAll = true }
        }
    }

    @Composable
    private fun Home(
        apps: List<AppEntry>, favs: List<String>,
        onToggle: (String) -> Unit, onAll: () -> Unit
    ) {
        Column(Modifier.fillMaxSize()) {
            Header()
            Spacer(Modifier.weight(1f))
            Text(
                "FAVORITES", color = TextLo, fontSize = 12.sp,
                letterSpacing = 3.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(16.dp))
            val favApps = favs.mapNotNull { f -> apps.firstOrNull { it.pkg == f } }
            if (favApps.isEmpty()) {
                Text(
                    "Open All apps and hold OK on an app to pin it here.",
                    color = TextLo, fontSize = 15.sp
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 6.dp)
                ) {
                    items(favApps, key = { it.pkg }) { a ->
                        AppCard(
                            a, true,
                            onClick = { launch(a.pkg) }, onLong = { onToggle(a.pkg) },
                            modifier = Modifier.width(176.dp).height(140.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Pill("All apps", onAll)
                Pill("Settings") { openSettings() }
            }
            Spacer(Modifier.weight(1f))
            Text("Hold OK on any app to pin or unpin", color = Color(0xFF566070), fontSize = 12.sp)
        }
    }

    @Composable
    private fun Header() {
        var now by remember { mutableStateOf(Date()) }
        var ram by remember { mutableStateOf(freeRamGb()) }
        LaunchedEffect(Unit) {
            while (true) { now = Date(); ram = freeRamGb(); delay(30_000) }
        }
        val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(greeting, color = TextLo, fontSize = 18.sp)
                Text(
                    SimpleDateFormat("h:mm", Locale.getDefault()).format(now),
                    color = TextHi, fontSize = 84.sp, fontWeight = FontWeight.Light
                )
                Text(
                    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now),
                    color = TextLo, fontSize = 16.sp
                )
            }
            Row(
                Modifier
                    .background(Card, RoundedCornerShape(50))
                    .border(1.dp, Hairline, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(Cyan, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(ram, color = TextHi, fontSize = 13.sp)
            }
        }
    }

    @Composable
    private fun AllApps(apps: List<AppEntry>, favs: List<String>, onToggle: (String) -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Text("All apps", color = TextHi, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(4.dp))
            Text("Hold OK to pin or unpin. Back returns home.", color = TextLo, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                items(apps, key = { it.pkg }) { a ->
                    AppCard(
                        a, a.pkg in favs,
                        onClick = { launch(a.pkg) }, onLong = { onToggle(a.pkg) },
                        modifier = Modifier.fillMaxWidth().height(124.dp)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun AppCard(
        app: AppEntry, pinned: Boolean,
        onClick: () -> Unit, onLong: () -> Unit, modifier: Modifier
    ) {
        var focused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(180), label = "scale")
        val bg by animateColorAsState(if (focused) CardFocused else Card, tween(180), label = "bg")
        val icon by produceState<Bitmap?>(null, app.pkg) {
            value = withContext(Dispatchers.IO) { IconCache.get(this@MainActivity, app.pkg) }
        }
        val shape = RoundedCornerShape(22.dp)
        Box(
            modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(bg, shape)
                .border(
                    if (focused) 2.5.dp else 1.dp,
                    if (focused) FocusBrush else SolidColor(Hairline), shape
                )
                .onFocusChanged { focused = it.isFocused }
                .combinedClickable(onClick = onClick, onLongClick = onLong),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(58.dp)) {
                    icon?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    app.label, color = if (focused) TextHi else TextLo, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
            if (pinned) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .size(7.dp).background(Violet, CircleShape)
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun Pill(label: String, onClick: () -> Unit) {
        var focused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(160), label = "pill")
        val bg by animateColorAsState(if (focused) CardFocused else Card, tween(160), label = "pillbg")
        val shape = RoundedCornerShape(50)
        Box(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(bg, shape)
                .border(
                    if (focused) 2.dp else 1.dp,
                    if (focused) FocusBrush else SolidColor(Hairline), shape
                )
                .onFocusChanged { focused = it.isFocused }
                .combinedClickable(onClick = onClick)
                .padding(horizontal = 28.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = if (focused) TextHi else TextLo, fontSize = 15.sp)
        }
    }
}
