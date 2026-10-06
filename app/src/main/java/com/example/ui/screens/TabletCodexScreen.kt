package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.content.SpanishContent
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

@Composable
fun TabletCodexScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedTabletId.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val reward by viewModel.practiceReward.collectAsStateWithLifecycle()
    val isArabic = language == HelperLanguage.ARABIC
    val data = content ?: return LoadingContent(isArabic)

    val tablet = data.tablets.firstOrNull { it.id == selectedId }
    if (tablet == null) {
        CodexIndex(data, viewModel, language)
    } else {
        key(tablet.id) { TabletReader(tablet, viewModel, language) }
    }

    reward?.let {
        RewardDialog(it, isArabic, language.pick("📜 اكتملت صفحة الأطلس!", "📜 ¡Página del atlas recuperada!")) {
            viewModel.dismissPracticeReward()
            viewModel.closeTablet()
        }
    }
}

// --------------------------------------------------------------------------- Index

@Composable
private fun CodexIndex(data: SpanishContent, viewModel: BlasterViewModel, language: HelperLanguage) {
    val progress by viewModel.tabletProgress.collectAsStateWithLifecycle()
    val done = progress.filter { it.isCompleted }.associateBy { it.tabletId }
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { SectionHeader(language.pick("ألواح القراءة", "TABLILLAS DE LECTURA"), language.pick("قصص قصيرة مع أسئلة وقواعد", "Short stories with questions and grammar")) }
        items(data.tablets) { tablet ->
            val index = data.tablets.indexOf(tablet)
            val unlocked = index == 0 || done.containsKey(data.tablets[index - 1].id) || done.containsKey(tablet.id)
            AdventureCard(
                borderColor = if (done.containsKey(tablet.id)) SolarGold else AdventureCardBorder,
                onClick = if (unlocked) ({ viewModel.openTablet(tablet.id) }) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelChip(tablet.level)
                    Spacer(Modifier.width(8.dp))
                    Text(tablet.title, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            done.containsKey(tablet.id) -> "⭐ ${done[tablet.id]?.bestScore}%"
                            unlocked -> "▶"
                            else -> "🔒"
                        },
                        color = SolarAmber, fontWeight = FontWeight.Bold
                    )
                }
                Text(language.pick(tablet.goalAr, tablet.goal), color = TextSecondary, fontSize = 12.sp)
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            SectionHeader(language.pick("مجموعات المفردات", "COLECCIONES DE VOCABULARIO"), language.pick("اضغطي على كلمة لسماعها", "Tap a word to hear it"))
        }
        items(data.categories) { category ->
            val expanded = expandedCategory == category.id
            AdventureCard(onClick = { expandedCategory = if (expanded) null else category.id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.icon, fontSize = 18.sp, color = ExplorerBlue)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(language.pick(category.arabic, category.english) + " · ${category.words.size}", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(if (expanded) "▲" else "▼", color = TextSecondary)
                }
                AnimatedVisibility(expanded) {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        category.words.forEach { word ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AdventureSurfaceVariant)
                                    .clickable { viewModel.speakSpanish(word.spanish.replace("/", ",")) }
                                    .padding(8.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(word.spanish, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(word.meaning(language), color = ExplorerBlue, fontSize = 12.sp)
                                    if (word.exampleEs.isNotBlank()) Text(word.exampleEs, color = TextSecondary, fontSize = 11.sp)
                                }
                                Text("🔊", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------- Reader

private enum class TabletStep { STORY, OPENING, LESSON, ORDER, MISSION, ENDING }

@Composable
private fun TabletReader(tablet: ReadingTablet, viewModel: BlasterViewModel, language: HelperLanguage) {
    var step by rememberSaveable { mutableStateOf(TabletStep.STORY) }
    // key → solved on first try (true) / solved after mistakes or revealed (false)
    val results = remember { mutableStateMapOf<String, Boolean>() }
    val tableCells = remember(tablet.id) {
        tablet.table.rows.indices.flatMap { r -> (1 until tablet.table.headers.size).map { c -> r to c } }
            .shuffled().take(4).toSet()
    }
    val totalQuestions = tablet.allQuestions.size + tableCells.size + 1
    val scroll = rememberScrollState()
    LaunchedEffect(step) { scroll.scrollTo(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.closeTablet() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("${tablet.level} · ${tablet.title}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(tablet.expeditionGoal, color = TextSecondary, fontSize = 11.sp)
            }
        }
        ProgressBar((step.ordinal + 1) / TabletStep.entries.size.toFloat(), color = SolarAmber, height = 8.dp)

        when (step) {
            TabletStep.STORY -> {
                StoryCard(language.pick("📡 رسالة واردة", "📡 Transmisión entrante"), tablet.story, tablet.storyAr, viewModel, language)
                BlasterCyberButton(language.pick("ابدئي المهمة", "Empezar la misión"), { step = TabletStep.OPENING }, Modifier.fillMaxWidth())
            }
            TabletStep.OPENING -> {
                QuestionGroup("opening", tablet.opening, results, viewModel, language)
                NextButton(tablet.opening.indices.all { "opening-$it" in results }, language) { step = TabletStep.LESSON }
            }
            TabletStep.LESSON -> {
                LessonCard(tablet, viewModel, language)
                GrammarTableQuiz(tablet.table, tableCells, results, viewModel, language)
                NextButton(tableCells.all { "table-${it.first}-${it.second}" in results }, language) { step = TabletStep.ORDER }
            }
            TabletStep.ORDER -> {
                OrderPuzzle(tablet.order, tablet.orderEn, results, viewModel, language)
                NextButton("order" in results, language) { step = TabletStep.MISSION }
            }
            TabletStep.MISSION -> {
                AdventureCard(borderColor = NebulaPurple) {
                    Text(language.pick("🛰️ المهمة", "🛰️ Misión"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(language.pick(tablet.missionAr, tablet.mission), color = TextPrimary, fontSize = 14.sp)
                }
                QuestionGroup("fields", tablet.fields, results, viewModel, language)
                NextButton(tablet.fields.indices.all { "fields-$it" in results }, language) { step = TabletStep.ENDING }
            }
            TabletStep.ENDING -> {
                StoryCard(language.pick("📖 خاتمة الفصل", "📖 Final del capítulo"), tablet.ending, null, viewModel, language)
                QuestionGroup("gate", tablet.gate, results, viewModel, language)
                val allDone = tablet.gate.indices.all { "gate-$it" in results }
                if (allDone) {
                    AdventureCard(borderColor = SuccessGreen) {
                        Text("🏁 " + tablet.expeditionPayoff, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        Text(language.pick("المكافأة: ", "Recompensa: ") + tablet.reward, color = TextSecondary, fontSize = 12.sp)
                    }
                }
                BlasterCyberButton(
                    text = language.pick("استلمي صفحة الأطلس", "Recoger la página del atlas"),
                    onClick = { viewModel.completeTablet(tablet, results.values.count { it }, totalQuestions) },
                    modifier = Modifier.fillMaxWidth(),
                    color = SuccessGreen,
                    enabled = allDone
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NextButton(enabled: Boolean, language: HelperLanguage, onClick: () -> Unit) {
    BlasterCyberButton(language.pick("التالي ›", "Siguiente ›"), onClick, Modifier.fillMaxWidth(), enabled = enabled)
}

@Composable
private fun StoryCard(title: String, story: String, translation: String?, viewModel: BlasterViewModel, language: HelperLanguage) {
    var showTranslation by remember { mutableStateOf(false) }
    val sentences = remember(story) { story.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() } }
    AdventureCard(borderColor = SolarGold) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            AudioButton(onClick = { viewModel.speakSpanish(story) })
        }
        Spacer(Modifier.height(8.dp))
        Text(language.pick("اضغطي على أي جملة لسماعها", "Toca una frase para escucharla"), color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        sentences.forEach { sentence ->
            Text(
                sentence,
                color = TextPrimary,
                fontSize = 17.sp,
                lineHeight = 25.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.speakSpanish(sentence) }
                    .padding(vertical = 3.dp, horizontal = 4.dp)
            )
        }
        if (!translation.isNullOrBlank()) {
            TextButton(onClick = { showTranslation = !showTranslation }) {
                Text(
                    if (showTranslation) language.pick("إخفاء الترجمة", "Ocultar traducción") else language.pick("عرض الترجمة العربية", "Ver traducción (árabe)"),
                    color = ExplorerBlue
                )
            }
            AnimatedVisibility(showTranslation) {
                Text(translation, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Start)
            }
        }
    }
}

@Composable
private fun LessonCard(tablet: ReadingTablet, viewModel: BlasterViewModel, language: HelperLanguage) {
    AdventureCard(borderColor = ExplorerBlue) {
        Text("📘 " + tablet.lesson.title, color = ExplorerBlue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(language.pick(tablet.lesson.arabic, tablet.lesson.english), color = TextPrimary, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tablet.lesson.example, color = NebulaPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            AudioButton(onClick = { viewModel.speakSpanish(tablet.lesson.example) }, size = 32.dp, tint = NebulaPurple)
        }
    }
}

@Composable
private fun GrammarTableQuiz(
    table: GrammarTable,
    hidden: Set<Pair<Int, Int>>,
    results: MutableMap<String, Boolean>,
    viewModel: BlasterViewModel,
    language: HelperLanguage
) {
    AdventureCard {
        Text(language.pick("🔧 أصلحي لوحة القواعد", "🔧 Repara la consola gramatical"), color = TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(language.pick("اكتبي التصريفات الناقصة", "Escribe las formas que faltan"), color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row {
            table.headers.forEach { header ->
                Text(header, color = ExplorerBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider(color = AdventureCardBorder, modifier = Modifier.padding(vertical = 4.dp))
        table.rows.forEachIndexed { r, row ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                row.forEachIndexed { c, cell ->
                    Box(Modifier.weight(1f).padding(end = 4.dp)) {
                        if ((r to c) in hidden) {
                            CellInput(cell, "table-$r-$c", results, viewModel)
                        } else {
                            Text(
                                cell,
                                color = if (c == 0) TextSecondary else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (c == 0) FontWeight.Normal else FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = c > 0) { viewModel.speakSpanish(cell) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellInput(answer: String, key: String, results: MutableMap<String, Boolean>, viewModel: BlasterViewModel) {
    var text by rememberSaveable(key) { mutableStateOf("") }
    var mistakes by rememberSaveable(key) { mutableIntStateOf(0) }
    val solved = key in results
    val accepted = answer.split("/").map { normalizeAnswer(it) }
    fun check() {
        if (solved) return
        if (normalizeAnswer(text) in accepted || normalizeAnswer(text) == normalizeAnswer(answer)) {
            results[key] = mistakes == 0
            viewModel.soundEngine.hit()
            viewModel.speakSpanish(answer)
        } else {
            mistakes++
            viewModel.soundEngine.error()
            if (mistakes >= 2) {
                text = answer
                results[key] = false
            }
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { if (!solved) text = it },
        singleLine = true,
        isError = mistakes > 0 && !solved,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (solved) SuccessGreen else TextPrimary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { check() }),
        trailingIcon = {
            if (solved) Text("✓", color = SuccessGreen)
            else Text("↵", color = ExplorerBlue, modifier = Modifier.clickable { check() })
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun QuestionGroup(
    prefix: String,
    fields: List<TabletField>,
    results: MutableMap<String, Boolean>,
    viewModel: BlasterViewModel,
    language: HelperLanguage
) {
    fields.forEachIndexed { index, field ->
        QuestionCard(field, "$prefix-$index", results, viewModel, language)
    }
}

@Composable
private fun QuestionCard(
    field: TabletField,
    key: String,
    results: MutableMap<String, Boolean>,
    viewModel: BlasterViewModel,
    language: HelperLanguage
) {
    var text by rememberSaveable(key) { mutableStateOf("") }
    var mistakes by rememberSaveable(key) { mutableIntStateOf(0) }
    val solved = key in results
    fun check() {
        if (solved || text.isBlank()) return
        if (field.answers.any { normalizeAnswer(it) == normalizeAnswer(text) }) {
            results[key] = mistakes == 0
            viewModel.soundEngine.hit()
            viewModel.speakSpanish(field.answers.first())
        } else {
            mistakes++
            viewModel.soundEngine.error()
        }
    }
    AdventureCard(borderColor = if (solved) SuccessGreen else if (mistakes > 0) MeteorRed.copy(alpha = 0.6f) else AdventureCardBorder) {
        Text(field.label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { if (!solved) text = it },
            singleLine = true,
            placeholder = { Text(language.pick("اكتبي الإجابة بالإسبانية", "Escribe en español")) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { check() }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        when {
            solved -> Text(
                "✅ " + field.answers.first() + if (results[key] == true) "  +XP" else "",
                color = SuccessGreen, fontWeight = FontWeight.Bold
            )
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BlasterCyberButton(language.pick("تحقّق", "Comprobar"), { check() })
                if (mistakes >= 2) {
                    OutlinedButton(onClick = {
                        text = field.answers.first()
                        results[key] = false
                    }) { Text(language.pick("أظهري الإجابة", "Ver respuesta"), color = TextSecondary) }
                }
            }
        }
        if (mistakes > 0 && !solved) {
            Spacer(Modifier.height(6.dp))
            Text("💡 " + language.pick(field.hintAr, field.hint), color = SolarAmber, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OrderPuzzle(
    fragments: List<String>,
    translation: String,
    results: MutableMap<String, Boolean>,
    viewModel: BlasterViewModel,
    language: HelperLanguage
) {
    val shuffled = remember(fragments) {
        var s = fragments.withIndex().shuffled()
        if (fragments.size > 1) while (s.map { it.index } == fragments.indices.toList()) s = s.shuffled()
        s
    }
    val placed = remember(fragments) { mutableStateListOf<IndexedValue<String>>() }
    var mistakes by remember(fragments) { mutableIntStateOf(0) }
    val solved = "order" in results

    AdventureCard(borderColor = if (solved) SuccessGreen else NebulaPurple) {
        Text(language.pick("📡 أعيدي بناء الرسالة", "📡 Reconstruye la transmisión"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold)
        Text("“$translation”", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        // Answer line
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AdventureSurfaceVariant)
                .padding(8.dp)
        ) {
            placed.forEach { frag ->
                Chip(frag.value, if (solved) SuccessGreen else ExplorerBlue) { if (!solved) placed.remove(frag) }
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shuffled.filter { it !in placed }.forEach { frag ->
                Chip(frag.value, TextSecondary) {
                    viewModel.soundEngine.click()
                    placed.add(frag)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (solved) {
            Text("✅ " + fragments.joinToString(" "), color = SuccessGreen, fontWeight = FontWeight.Bold)
        } else if (placed.size == fragments.size) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BlasterCyberButton(language.pick("تحقّق", "Comprobar"), {
                    if (placed.map { it.value } == fragments) {
                        results["order"] = mistakes == 0
                        viewModel.soundEngine.hit()
                        viewModel.speakSpanish(fragments.joinToString(" "))
                    } else {
                        mistakes++
                        viewModel.soundEngine.error()
                        placed.clear()
                    }
                })
            }
        }
        if (mistakes >= 2 && !solved) {
            TextButton(onClick = {
                placed.clear()
                placed.addAll(fragments.withIndex())
                results["order"] = false
            }) { Text(language.pick("أظهري الترتيب الصحيح", "Ver el orden correcto"), color = TextSecondary) }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}
