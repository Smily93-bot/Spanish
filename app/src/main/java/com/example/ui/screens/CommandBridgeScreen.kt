package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.engagement.EngagementStore
import com.example.data.engagement.LiaMood
import com.example.data.engagement.PathStep
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.components.LoadingContent
import com.example.ui.components.ProgressBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

/**
 * Home: Lía, today's progress and one big START button. Everything else lives in Practice and Me,
 * so a first-time player only has to make one decision.
 */
@Composable
fun CommandBridgeScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val done by viewModel.pathDone.collectAsStateWithLifecycle()
    val tablets by viewModel.tabletProgress.collectAsStateWithLifecycle()
    val data = content ?: return LoadingContent(language == HelperLanguage.ARABIC)
    val showTour by viewModel.showTour.collectAsStateWithLifecycle()

    val today = EngagementStore.today()
    val mood = streak.liaMood(today)
    val next = viewModel.nextStep(done)
    val completedIds = tablets.filter { it.isCompleted }.map { it.tabletId }.toSet()
    val chapter = data.tablets.firstOrNull { it.id !in completedIds }
    val grammarStars by viewModel.grammarStars.collectAsStateWithLifecycle()
    val nextTopic = remember(data, grammarStars) { viewModel.nextGrammarTopic() }
    val courseStars by viewModel.courseStars.collectAsStateWithLifecycle()
    val lessonUnit = data.course.getOrNull(nextCourseUnit(data.course, courseStars))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Lía and what she says, and ❔ to read the welcome tour again.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(if (mood == LiaMood.SAD) R.drawable.lia_sad else R.drawable.lia_happy),
                contentDescription = "Lía",
                modifier = Modifier.size(76.dp)
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                color = AdventureSurface,
                border = BorderStroke(1.5.dp, AdventureCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    when {
                        mood == LiaMood.SAD -> language.pick("اشتقتُ إليك! لنبدأ من جديد.", "I missed you! Let's start again.")
                        next == null -> language.pick("أنهيت تمارين اليوم! 🎉 هيا إلى المغامرة.", "Today's practice is done! 🎉 On to the adventure.")
                        else -> language.pick("أهلًا يا بطل! من أين نبدأ؟", "Hi! Where shall we start?")
                    },
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = { viewModel.showTourAgain() },
                shape = CircleShape,
                color = SpaceNavy,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("❔", fontSize = 20.sp) }
            }
        }

        // 1. The main game: Lía's adventure.
        HomeChoice(
            emoji = "🚀",
            title = language.pick("مغامرة ليا", "Lía's adventure"),
            subtitle = chapter?.let { "${it.level} · ${it.title(language)}" } ?: language.pick("أعد أي فصل", "Replay any chapter"),
            action = language.pick("العب ▶", "PLAY ▶"),
            colors = listOf(SpaceDeep, Color(0xFF2A1F6B)),
            accent = SolarGold
        ) { viewModel.startFromHome(PathStep.STORY) }

        // 2. Today's practice: lesson, words, grammar.
        val todayDone = PathStep.TODAY.count { it in done }
        HomeChoice(
            emoji = "📅",
            title = language.pick("تمارين اليوم", "Today's practice"),
            subtitle = if (next == null) language.pick("أنهيتها كلها! العب ما تشاء", "All done! Play anything you like")
            else "$todayDone/${PathStep.TODAY.size} · " + stepTitle(next, language),
            action = language.pick("ابدأ ▶", "START ▶"),
            colors = listOf(SolarAmber, Color(0xFFF59E2B)),
            accent = Color.White
        ) { if (next == null) viewModel.navigateTo(Screen.Practice) else viewModel.startFromHome(next) }

        // Today's goal
        val xp = streak.xpOn(today)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (streak.goalMetOn(today)) "🔥" else "🕯️", fontSize = 20.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    language.pick("${streak.liveStreak(today)} يوم متتالي", "${streak.liveStreak(today)}-day streak"),
                    color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    language.pick("${minOf(xp, streak.dailyGoal)} / ${streak.dailyGoal} نقطة اليوم", "${minOf(xp, streak.dailyGoal)} / ${streak.dailyGoal} XP today"),
                    color = TextSecondary, fontSize = 13.sp
                )
            }
            ProgressBar(progress = xp / streak.dailyGoal.toFloat(), color = SolarAmber, height = 12.dp)
        }

        // Today's path: the three practice steps.
        Text(language.pick("مسار اليوم", "Today's path"), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        PathStep.TODAY.forEachIndexed { i, step ->
            PathRow(
                number = i + 1,
                title = stepTitle(step, language),
                subtitle = when (step) {
                    PathStep.LESSON -> lessonUnit?.let { "${language.pick("المستوى", "Level")} ${data.course.indexOf(it) + 1} · ${it.title} · ${it.helperTitle(language)}" }
                        ?: language.pick("٨ كلمات بالصور والألعاب", "8 words with pictures and games")
                    PathStep.WORDS -> language.pick("5 كلمات جديدة أو مراجعة سريعة", "5 new words or a quick review")
                    PathStep.GRAMMAR -> nextTopic?.title(language)
                        ?: language.pick("قاعدة واحدة خطوة بخطوة", "One rule, step by step")
                    PathStep.STORY -> chapter?.title(language).orEmpty()
                },
                emoji = stepEmoji(step),
                done = step in done,
                current = step == next
            ) { viewModel.startFromHome(step) }
        }
        Spacer(Modifier.height(4.dp))
    }

    if (showTour) WelcomeTour(language) { viewModel.dismissTour() }
}

/** One of the two big choices on Home. */
@Composable
private fun HomeChoice(emoji: String, title: String, subtitle: String, action: String, colors: List<Color>, accent: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(Brush.horizontalGradient(colors)).padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            }
            Text(action, color = accent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

fun stepTitle(step: PathStep, language: HelperLanguage) = when (step) {
    PathStep.LESSON -> language.pick("درس اليوم", "Today's lesson")
    PathStep.WORDS -> language.pick("تعلّم كلمات", "Learn words")
    PathStep.STORY -> language.pick("مغامرة ليا", "Lía's adventure")
    PathStep.GRAMMAR -> language.pick("القواعد: لماذا؟", "Grammar: why?")
}

private fun stepEmoji(step: PathStep) = when (step) {
    PathStep.LESSON -> "📚"
    PathStep.WORDS -> "🌌"
    PathStep.STORY -> "🗺️"
    PathStep.GRAMMAR -> "🕵️"
}

@Composable
private fun PathRow(
    number: Int,
    title: String,
    subtitle: String,
    emoji: String,
    done: Boolean,
    current: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (current) SolarAmber.copy(alpha = 0.10f) else AdventureSurface,
        border = BorderStroke(if (current) 2.dp else 1.dp, if (current) SolarAmber else AdventureCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (done) SuccessGreen else AdventureSurfaceVariant)
            ) {
                Text(if (done) "✓" else emoji, color = Color.White, fontSize = if (done) 22.sp else 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("$number. $title", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(subtitle, color = TextSecondary, fontSize = 13.sp, maxLines = 1)
            }
            Text("›", color = if (current) SolarAmber else TextSecondary, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
    }
}

/** Welcome tour: a pop-up the first time, and again from the ❔ button on Home. */
@Composable
private fun WelcomeTour(language: HelperLanguage, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceNavy,
        shape = RoundedCornerShape(24.dp),
        title = { Text(language.pick("👋 أهلًا بك! هذا ما في التطبيق", "👋 Welcome! Here's what's inside"), color = SolarGold, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TourLine("🚀", language.pick("مغامرة ليا: اللعبة الأساسية. امشِ في السفينة وحلّ الألغاز وتعلّم.", "Lía's adventure: the main game. Walk the ship, solve puzzles, learn."))
                TourLine("📅", language.pick("تمارين اليوم: درس، ثم كلمات، ثم قاعدة.", "Today's practice: a lesson, then words, then a rule."))
                TourLine("🎮", language.pick("تمارين (في الأسفل): كل الألعاب في مكان واحد.", "Practice (bottom bar): every game in one place."))
                TourLine("👤", language.pick("أنا (في الأسفل): تقدّمك وجوائزك والإعدادات.", "Me (bottom bar): your progress, trophies and settings."))
                TourLine("❔", language.pick("اضغط ❔ في الأعلى لتقرأ هذا مرة أخرى.", "Tap ❔ at the top to read this again."))
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                shape = RoundedCornerShape(14.dp)
            ) { Text(language.pick("فهمت!", "Got it!"), fontWeight = FontWeight.ExtraBold) }
        }
    )
}

@Composable
private fun TourLine(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp, modifier = Modifier.width(34.dp))
        Text(text, color = StarWhite, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
    }
}
