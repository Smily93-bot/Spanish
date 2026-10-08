package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

private data class PracticeTile(
    val emoji: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val color: Color,
    val open: (BlasterViewModel) -> Unit
)

private val TILES = listOf(
    PracticeTile("📚", "الدورة", "Course", "تعلّمي من الصفر", "Learn from zero", SuccessGreen) { it.navigateTo(Screen.Course) },
    PracticeTile("🔗", "التوصيل", "Matching", "صِلي الكلمة بصورتها", "Match words to pictures", ExplorerBlue) { it.navigateTo(Screen.MatchGame) },
    PracticeTile("🔠", "الكلمات المتقاطعة", "Crossword", "الصورة هي التلميح", "Pictures are the clues", NebulaPurple) { it.navigateTo(Screen.Crossword) },
    PracticeTile("🌌", "الكلمات", "Words", "أهم 5000 كلمة", "Top 5000 words", DiamondCyan) { it.navigateTo(Screen.WordGalaxy) },
    PracticeTile("🕵️", "القواعد: لماذا؟", "Grammar: why?", "افهمي سبب كل إجابة", "The reason behind answers", NebulaPurple) { it.navigateTo(Screen.GrammarLab) },
    PracticeTile("🗺️", "المغامرة", "Adventure", "قصة ليا ومهماتها", "Lía's story missions", SolarAmber) { it.navigateTo(Screen.AdventureMap) },
    PracticeTile("☄️", "النيازك", "Meteors", "اختاري المعنى بسرعة", "Pick the meaning fast", MeteorRed) { it.navigateTo(Screen.MeteorBlaster) },
    PracticeTile("🐸", "القفز", "Word Jump", "اقفزي على الكلمة الصحيحة", "Jump on the right word", SolarGold) { it.navigateTo(Screen.WordJump) },
    PracticeTile("🔀", "الجمل", "Sentences", "رتّبي الكلمات", "Put words in order", ExplorerBlue) { it.navigateTo(Screen.GrammarReactor) },
    PracticeTile("✏️", "الفراغات", "Fill the gap", "أكملي الجملة", "Complete the sentence", SuccessGreen) { it.navigateTo(Screen.QuantumCloze) }
)

/** All ways to practise, as big tiles with one short line each. */
@Composable
fun PracticeScreen(viewModel: BlasterViewModel) {
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(language.pick("ماذا تريدين أن تلعبي؟", "What do you want to play?"), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        TILES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { tile -> TileCard(tile, language, Modifier.weight(1f)) { tile.open(viewModel) } }
            }
        }
    }
}

@Composable
private fun TileCard(tile: PracticeTile, language: HelperLanguage, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = AdventureSurface,
        border = BorderStroke(1.5.dp, tile.color.copy(alpha = 0.45f)),
        modifier = modifier.height(150.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tile.color.copy(alpha = 0.15f))
            ) { Text(tile.emoji, fontSize = 32.sp) }
            Spacer(Modifier.height(8.dp))
            Text(language.pick(tile.titleAr, tile.titleEn), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
            Text(language.pick(tile.subtitleAr, tile.subtitleEn), color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}
