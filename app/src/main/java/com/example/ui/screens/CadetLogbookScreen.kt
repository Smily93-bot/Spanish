package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HelperLanguage
import com.example.data.model.Rank
import com.example.data.model.normalizeAnswer
import com.example.data.model.pick
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

private enum class LogTab(val es: String, val ar: String, val en: String) {
    LEXICON("Léxico", "معجمي", "My words"),
    DICTIONARY("Diccionario", "القاموس", "Dictionary"),
    GRAMMAR("Gramática", "القواعد", "Grammar"),
    SETTINGS("Ajustes", "الإعدادات", "Settings")
}

@Composable
fun CadetLogbookScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val mastery by viewModel.wordMastery.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val isArabic = language == HelperLanguage.ARABIC
    val data = content ?: return LoadingContent(isArabic)

    var tab by rememberSaveable { mutableStateOf(LogTab.LEXICON) }
    var query by rememberSaveable { mutableStateOf("") }
    val level = progress?.level ?: 1
    val rank = Rank.forLevel(level)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AdventureCard(borderColor = NebulaPurple.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rank.emoji, fontSize = 36.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(language.pick("سجل المستكشفة", "BITÁCORA DE CADETE"), color = NebulaPurple, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${rank.spanish} · ${rank.label(language)}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            language.pick("المستوى $level · ${mastery.size} كلمة تمت مواجهتها", "Nivel $level · ${mastery.size} words encountered"),
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Rank.entries.forEach { r ->
                        LevelChip("${r.emoji} ${r.spanish}", if (level >= r.minLevel) NebulaPurple else TextSecondary)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                LogTab.entries.forEach { t ->
                    val selected = t == tab
                    Surface(
                        onClick = { tab = t; query = "" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) NebulaPurple else AdventureSurface,
                        border = BorderStroke(1.dp, if (selected) NebulaPurple else AdventureCardBorder)
                    ) {
                        Text(
                            "${t.es} · ${language.pick(t.ar, t.en)}",
                            color = if (selected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        when (tab) {
            LogTab.LEXICON -> {
                item { SearchField(query, { query = it }, language) }
                val filtered = mastery.filter { query.isBlank() || normalizeAnswer(it.spanishWord).contains(normalizeAnswer(query)) || it.englishWord.contains(query, true) }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            language.pick("العبي أي لعبة لتبدأ الكلمات بالظهور هنا.", "Play any game and the words you meet will appear here."),
                            color = TextSecondary, fontSize = 13.sp
                        )
                    }
                }
                items(filtered.take(300), key = { it.spanishWord }) { w ->
                    val entry = data.lookup(w.spanishWord)
                    AdventureCard(onClick = { viewModel.speakSpanish(w.spanishWord) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(w.spanishWord, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(entry?.meaning(language) ?: language.pick("", w.englishWord), color = ExplorerBlue, fontSize = 12.sp)
                                Text("✓ ${w.timesCorrect}/${w.timesEncountered}", color = TextSecondary, fontSize = 11.sp)
                            }
                            Text("★".repeat(w.masteryLevel) + "☆".repeat(5 - w.masteryLevel), color = SolarGold, fontSize = 14.sp)
                        }
                    }
                }
            }
            LogTab.DICTIONARY -> {
                item { SearchField(query, { query = it }, language) }
                val q = normalizeAnswer(query)
                val results = if (q.length < 2) data.topicWords.take(30) else
                    (data.topicWords + data.frequency).filter {
                        normalizeAnswer(it.spanish).contains(q) || normalizeAnswer(it.meaning(language)).contains(q)
                    }.distinctBy { it.spanish }.take(60)
                items(results) { w ->
                    AdventureCard(onClick = { viewModel.speakSpanish(w.shortSpanish) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(w.spanish, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(Modifier.width(6.dp))
                                    LevelChip(w.level)
                                }
                                Text(w.meaning(language), color = ExplorerBlue, fontSize = 12.sp)
                                if (w.exampleEs.isNotBlank()) Text(w.exampleEs, color = TextSecondary, fontSize = 12.sp)
                            }
                            Text("🔊", fontSize = 16.sp)
                        }
                    }
                }
            }
            LogTab.GRAMMAR -> {
                items(data.grammar, key = { it.id }) { g ->
                    var open by remember { mutableStateOf(false) }
                    AdventureCard(borderColor = if (open) ExplorerBlue else AdventureCardBorder, onClick = { open = !open }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LevelChip(g.level, ExplorerBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick(g.titleAr, g.title), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(if (open) "▲" else "▼", color = TextSecondary)
                        }
                        if (open) {
                            Spacer(Modifier.height(6.dp))
                            Text(language.pick(g.arabic, g.english), color = TextPrimary, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(g.formula, color = NebulaPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            g.examples.forEach { ex ->
                                Text(
                                    "🔊 $ex",
                                    color = ExplorerBlue,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 2.dp).clickable { viewModel.speakSpanish(ex) }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("⚠️ " + language.pick(g.mistakeAr, g.mistakeEn), color = SolarAmber, fontSize = 12.sp)
                        }
                    }
                }
            }
            LogTab.SETTINGS -> {
                item {
                    AdventureCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(language.pick("🔊 المؤثرات الصوتية", "🔊 Sound effects"), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(checked = progress?.soundEnabled ?: true, onCheckedChange = { viewModel.setSoundEnabled(it) })
                        }
                    }
                }
                item {
                    AdventureCard {
                        val speed = progress?.speechSpeed ?: 0.95f
                        var sliderValue by remember(speed) { mutableFloatStateOf(speed) }
                        Text(language.pick("🗣️ سرعة النطق", "🗣️ Speech speed") + "  ${"%.2f".format(sliderValue)}x", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { viewModel.setSpeechSpeed(sliderValue) },
                            valueRange = 0.5f..1.3f,
                            steps = 7
                        )
                        BlasterCyberButton(language.pick("جرّبي الصوت", "Probar voz"), { viewModel.speakSpanish("¡Hola! Bienvenida a Spanish Blaster.") })
                        if (viewModel.speechEngine.spanishVoiceMissing) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                language.pick("لا يوجد صوت إسباني مثبت على الجهاز.", "No Spanish voice installed on this device."),
                                color = MeteorRed, fontSize = 12.sp
                            )
                        }
                    }
                }
                item {
                    AdventureCard {
                        Text(language.pick("ℹ️ عن التطبيق", "ℹ️ About"), color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            language.pick(
                                "يعمل التطبيق بالكامل دون إنترنت ودون أي مفتاح API. المحتوى: قصص أوربيتا (A1–C2)، بنك Parliva للمفردات، وقائمة أكثر 5000 كلمة استخدامًا.",
                                "Works fully offline with no API key. Content: Órbita stories (A1–C2), the Parliva vocabulary bank and the Frequency 5000 list."
                            ),
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, language: HelperLanguage) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(language.pick("ابحثي بالإسبانية أو العربية…", "Search in Spanish or English…")) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
