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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Lía and what she says
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(if (mood == LiaMood.SAD) R.drawable.lia_sad else R.drawable.lia_happy),
                contentDescription = "Lía",
                modifier = Modifier.size(84.dp)
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
                        next == null -> language.pick("أنجزتِ كل شيء اليوم! 🎉", "You finished today's path! 🎉")
                        mood == LiaMood.SAD -> language.pick("اشتقتُ إليكِ! لنبدأ من جديد.", "I missed you! Let's start again.")
                        done.isEmpty() -> language.pick("هل أنتِ مستعدة؟ اضغطي على زر ابدئي.", "Ready? Tap START.")
                        else -> language.pick("أحسنتِ! لنكمل.", "Nice work! Let's keep going.")
                    },
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (showTour) WelcomeTour(language) { viewModel.dismissTour() }

        // The beginner course comes first: it teaches the words the games and the adventure use.
        val courseStars by viewModel.courseStars.collectAsStateWithLifecycle()
        if (data.course.isNotEmpty()) {
            val unitIndex = nextCourseUnit(data.course, courseStars)
            val unit = data.course[unitIndex]
            val allDone = data.course.all { (courseStars[it.id] ?: 0) > 0 }
            Surface(
                onClick = { viewModel.navigateTo(Screen.Course) },
                shape = RoundedCornerShape(22.dp),
                color = SuccessGreen.copy(alpha = 0.12f),
                border = BorderStroke(2.dp, SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                    Text(if (allDone) "🎓" else unit.emoji, fontSize = 34.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (allDone) language.pick("📚 أنهيتِ الدورة! راجعي متى شئتِ", "📚 Course finished! Review any time")
                            else language.pick("📚 الدورة · الوحدة ${unitIndex + 1}", "📚 Course · Unit ${unitIndex + 1}"),
                            color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp
                        )
                        if (!allDone) {
                            Text(unit.title, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(unit.helperTitle(language), color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    Text("▶", color = SuccessGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

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

        // The one big button
        val startStep = next ?: PathStep.WORDS
        Surface(
            onClick = { if (next == null) viewModel.navigateTo(Screen.Practice) else viewModel.startFromHome(startStep) },
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .background(Brush.horizontalGradient(listOf(SolarAmber, Color(0xFFF59E2B))))
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (next == null) language.pick("العبي أكثر ▶", "PLAY MORE ▶") else language.pick("ابدئي ▶", "START ▶"),
                        color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (next == null) language.pick("اختاري أي لعبة", "Pick any game") else stepTitle(startStep, language),
                        color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Today's path
        Text(language.pick("مسار اليوم", "Today's path"), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        PathStep.entries.forEachIndexed { i, step ->
            PathRow(
                number = i + 1,
                title = stepTitle(step, language),
                subtitle = when (step) {
                    PathStep.WORDS -> language.pick("5 كلمات جديدة أو مراجعة سريعة", "5 new words or a quick review")
                    PathStep.STORY -> chapter?.let { it.title(language) } ?: language.pick("أعيدي أي فصل", "Replay any chapter")
                    PathStep.GRAMMAR -> nextTopic?.title(language)
                        ?: language.pick("قاعدة واحدة وسبب كل إجابة", "One rule and the reason behind each answer")
                },
                emoji = stepEmoji(step),
                done = step in done,
                current = step == next
            ) { viewModel.startFromHome(step) }
        }
        Spacer(Modifier.height(4.dp))
    }
}

fun stepTitle(step: PathStep, language: HelperLanguage) = when (step) {
    PathStep.WORDS -> language.pick("تعلّمي كلمات", "Learn words")
    PathStep.STORY -> language.pick("مغامرة ليا", "Lía's adventure")
    PathStep.GRAMMAR -> language.pick("القواعد: لماذا؟", "Grammar: why?")
}

private fun stepEmoji(step: PathStep) = when (step) {
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

/** One-time card for new players: what each part of the app is for. */
@Composable
private fun WelcomeTour(language: HelperLanguage, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NebulaPurple.copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, NebulaPurple.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(language.pick("👋 أهلًا بكِ! هذا ما في التطبيق:", "👋 Welcome! Here's what's inside:"), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            TourLine("▶", language.pick("زر ابدئي يختار لكِ درس اليوم خطوة بخطوة.", "START picks today's lesson for you, step by step."))
            TourLine("🌌", language.pick(tl("الكلمات: تعلّمي أهم 5000 كلمة إسبانية."), tl("Words: learn the 5000 most-used Spanish words.")))
            TourLine("🕵️", language.pick("القواعد: افهمي لماذا تكون الإجابة صحيحة.", "Grammar: understand why each answer is right."))
            TourLine("🗺️", language.pick("المغامرة: امشي مع ليا ونيلو وأنجزي المهمات.", "Adventure: walk with Lía and Nilo and finish missions."))
            TourLine("🎮", language.pick("تمارين في الأسفل: كل الألعاب في مكان واحد.", "Practice (bottom bar): every game in one place."))
            TourLine("👤", language.pick("أنا في الأسفل: تقدّمك وجوائزك والإعدادات.", "Me: your progress, trophies and settings."))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(language.pick("فهمت!", "Got it!"), fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun TourLine(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp, modifier = Modifier.width(34.dp))
        Text(text, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}
