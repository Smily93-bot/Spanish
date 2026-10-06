package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.TabletProgressEntity
import com.example.data.model.CefrLevel
import com.example.data.model.HelperLanguage
import com.example.data.model.ReadingTablet
import com.example.data.model.pick
import com.example.ui.components.LoadingContent
import com.example.ui.components.ProgressBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

private data class Sector(val level: CefrLevel, val name: String, val nameAr: String, val color: Color, val planet: String)

private val SECTORS = listOf(
    Sector(CefrLevel.A1, "Sector Amanecer", "قطاع الفجر", Color(0xFF2E9F5B), "🌍"),
    Sector(CefrLevel.A2, "Cinturón de Archivos", "حزام الأرشيف", Color(0xFF1667C9), "🪐"),
    Sector(CefrLevel.B1, "Jardín de Memorias", "حديقة الذكريات", Color(0xFF7C4DDB), "🌸"),
    Sector(CefrLevel.B2, "Nebulosa Condicional", "سديم الشرط", Color(0xFFE07A10), "🌌"),
    Sector(CefrLevel.C1, "Observatorio del Tiempo", "مرصد الزمن", Color(0xFFE5484D), "🔭"),
    Sector(CefrLevel.C2, "Consejo de Mundos", "مجلس العوالم", Color(0xFF0A1633), "💎")
)

@Composable
fun AdventureMapScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val progress by viewModel.tabletProgress.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val isArabic = language == HelperLanguage.ARABIC
    val data = content ?: return LoadingContent(isArabic)

    val byId = progress.associateBy { it.tabletId }
    val completed = data.tablets.count { byId[it.id]?.isCompleted == true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(language.pick("أطلس أوربيتا", "ATLAS DE ÓRBITA"), color = DiamondCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    language.pick(
                        "ساعدي ليا ونيلو على جمع صفحات الأطلس الاثنتي عشرة للعودة إلى الوطن.",
                        "Help Lía and Nilo recover the 12 atlas pages and fly home."
                    ),
                    color = StarWhite, fontSize = 14.sp
                )
                ProgressBar(completed / data.tablets.size.toFloat(), color = SolarGold)
                Text("$completed / ${data.tablets.size} " + language.pick("صفحات", "páginas"), color = StarWhite.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }
        items(SECTORS) { sector ->
            val sectorTablets = data.tablets.filter { it.cefr == sector.level }
            SectorCard(sector, sectorTablets, data.tablets, byId, language, onOpen = { viewModel.openTablet(it.id) })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SectorCard(
    sector: Sector,
    tablets: List<ReadingTablet>,
    allTablets: List<ReadingTablet>,
    progress: Map<String, TabletProgressEntity>,
    language: HelperLanguage,
    onOpen: (ReadingTablet) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AdventureSurface,
        border = BorderStroke(1.5.dp, sector.color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sector.planet, fontSize = 30.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(sector.name, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("${sector.level.code} · ${sector.nameAr}", color = sector.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tablets.forEach { tablet ->
                    val index = allTablets.indexOf(tablet)
                    val unlocked = index == 0 || progress[allTablets[index - 1].id]?.isCompleted == true ||
                        progress[tablet.id]?.isCompleted == true
                    val done = progress[tablet.id]?.isCompleted == true
                    PlanetNode(
                        tablet = tablet,
                        color = sector.color,
                        unlocked = unlocked,
                        done = done,
                        bestScore = progress[tablet.id]?.bestScore ?: 0,
                        language = language,
                        modifier = Modifier.weight(1f),
                        onClick = { if (unlocked) onOpen(tablet) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanetNode(
    tablet: ReadingTablet,
    color: Color,
    unlocked: Boolean,
    done: Boolean,
    bestScore: Int,
    language: HelperLanguage,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AdventureSurfaceVariant)
            .clickable(enabled = unlocked, onClick = onClick)
            .alpha(if (unlocked) 1f else 0.5f)
            .padding(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.35f), color)))
                .border(2.dp, if (done) SolarGold else Color.White, CircleShape)
        ) {
            Text(
                when {
                    done -> "✓"
                    unlocked -> tablet.level
                    else -> "🔒"
                },
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (done) 22.sp else 13.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(tablet.title(language), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        Text(
            if (done) "⭐ $bestScore%" else tablet.goal(language),
            color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 3
        )
    }
}
