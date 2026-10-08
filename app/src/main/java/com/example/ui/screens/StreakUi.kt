package com.example.ui.screens

import com.example.flavor.tl
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.engagement.LiaMood
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engagement.EngagementStore
import com.example.data.engagement.ReminderSettings
import com.example.data.engagement.StreakEvent
import com.example.data.engagement.StreakState
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.components.AdventureCard
import com.example.ui.components.ProgressBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Flame + streak count in the top bar; lit once today's goal is met. */
@Composable
fun StreakChip(viewModel: BlasterViewModel) {
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val today = EngagementStore.today()
    val lit = streak.goalMetOn(today)
    Surface(
        onClick = { viewModel.navigateTo(Screen.CommandBridge) },
        shape = RoundedCornerShape(8.dp),
        color = if (lit) SolarAmber.copy(alpha = 0.15f) else AdventureSurfaceVariant,
        border = BorderStroke(1.dp, if (lit) SolarAmber else AdventureCardBorder),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (lit) "🔥" else "🕯️", fontSize = 13.sp)
            Spacer(Modifier.width(3.dp))
            Text(
                "${streak.liveStreak(today)}",
                color = if (lit) SolarAmber else TextSecondary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}

/** Streak card on the Me tab: Lía's mood, streak, this week's calendar and today's goal. */
@Composable
fun StreakCard(viewModel: BlasterViewModel, language: HelperLanguage) {
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val today = EngagementStore.today()
    val xp = streak.xpOn(today)
    val live = streak.liveStreak(today)
    val done = streak.goalMetOn(today)

    AdventureCard(borderColor = SolarAmber.copy(alpha = 0.6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val mood = streak.liaMood(today)
            Image(
                painter = painterResource(if (mood == LiaMood.SAD) R.drawable.lia_sad else R.drawable.lia_happy),
                contentDescription = "Lía",
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    language.pick("سلسلة $live يوم", if (live == 1) "1-day streak" else "$live-day streak"),
                    color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                )
                Text(
                    when {
                        done -> language.pick("أنجزتِ هدف اليوم! عودي غدًا 💪", "Goal done today! Come back tomorrow 💪")
                        streak.liaMood(today) == LiaMood.SAD -> language.pick(
                            "ليا حزينة، غبتِ ${streak.daysAway(today)} أيام. أكملي هدف اليوم لتفرح من جديد!",
                            "Lía is sad, you were away ${streak.daysAway(today)} days. Hit today's goal to cheer her up!"
                        )
                        live > 0 -> language.pick("أكملي هدف اليوم حتى لا تنطفئ الشعلة", "Hit today's goal to keep the flame alive")
                        else -> language.pick("أكملي هدف اليوم لتبدئي سلسلة", "Hit today's goal to start a streak")
                    },
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("❄️ ${streak.freezes}", color = DiamondCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(language.pick("أفضل: ${streak.bestStreak}", "Best: ${streak.bestStreak}"), color = TextSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        WeekRow(streak, today, language)
        Spacer(Modifier.height(10.dp))
        ProgressBar(progress = xp / streak.dailyGoal.toFloat(), color = SolarAmber)
        Text(
            language.pick("هدف اليوم: ${minOf(xp, streak.dailyGoal)} / ${streak.dailyGoal} نقطة", "Today: ${minOf(xp, streak.dailyGoal)} / ${streak.dailyGoal} XP"),
            color = TextSecondary, fontSize = 12.sp
        )
    }
}

/** Daily goal, reminder and streak-freeze settings (shown on the Me tab). */
@Composable
fun StreakSettings(viewModel: BlasterViewModel, language: HelperLanguage) {
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(language.pick("الهدف اليومي", "Daily goal"), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StreakState.GOAL_CHOICES.forEach { goal ->
                FilterChip(
                    selected = streak.dailyGoal == goal,
                    onClick = { viewModel.setDailyGoal(goal) },
                    label = { Text("$goal XP", fontSize = 12.sp) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(language.pick("تذكير يومي", "Daily reminder"), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    language.pick("نذكّرك فقط إذا لم تنجزي هدفك", "Only if you haven't reached your goal"),
                    color = TextSecondary, fontSize = 11.sp
                )
            }
            Switch(
                checked = reminder.enabled,
                onCheckedChange = { on ->
                    if (on) requestNotificationPermission(context)
                    viewModel.setReminder(reminder.copy(enabled = on))
                }
            )
        }
        if (reminder.enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(9, 13, 18, 20).forEach { hour ->
                    FilterChip(
                        selected = reminder.hour == hour,
                        onClick = { viewModel.setReminder(ReminderSettings(true, hour)) },
                        label = { Text("%02d:00".format(hour), fontSize = 12.sp) }
                    )
                }
            }
        }
        Text(
            language.pick(
                "❄️ تجميد السلسلة يحميها إذا فاتك يوم. تحصلين على واحد كل 7 أيام.",
                "❄️ A streak freeze saves you if you miss a day. Earn one every 7 days."
            ),
            color = TextSecondary, fontSize = 11.sp
        )
        Text(
            language.pick(
                tl("📱 أضيفي الأداة إلى الشاشة الرئيسية: اضغطي مطولًا على الشاشة ← الأدوات ← Spanish Blaster."),
                tl("📱 Add the widget: long-press your home screen → Widgets → Spanish Blaster.")
            ),
            color = TextSecondary, fontSize = 11.sp
        )
    }
}

@Composable
private fun WeekRow(streak: StreakState, today: Long, language: HelperLanguage) {
    val locale = if (language == HelperLanguage.ARABIC) Locale("ar") else Locale.ENGLISH
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        (6 downTo 0).forEach { back ->
            val day = today - back
            val met = day in streak.goalDays
            val frozen = day in streak.frozenDays
            val isToday = back == 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    color = if (isToday) SolarAmber else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                met -> SolarAmber
                                frozen -> DiamondCyan.copy(alpha = 0.3f)
                                isToday -> AdventureSurfaceVariant
                                else -> AdventureSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                ) {
                    Text(
                        when {
                            met -> "🔥"
                            frozen -> "❄️"
                            else -> ""
                        },
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun requestNotificationPermission(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < 33) return
    val activity = context as? Activity ?: return
    if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
    }
}

/** Celebration when today's goal is reached. */
@Composable
fun StreakCelebration(viewModel: BlasterViewModel, language: HelperLanguage) {
    val event by viewModel.streakEvent.collectAsStateWithLifecycle()
    val e = event ?: return
    AlertDialog(
        onDismissRequest = { viewModel.dismissStreakEvent() },
        containerColor = AdventureSurface,
        title = null,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🔥", fontSize = 64.sp)
                Text(
                    language.pick("سلسلة ${e.streak} يوم!", "${e.streak}-day streak!"),
                    color = SolarAmber, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    language.pick("أنجزتِ هدف اليوم. نراك غدًا!", "Daily goal reached. See you tomorrow!"),
                    color = TextPrimary, textAlign = TextAlign.Center
                )
                if (e.milestone != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        language.pick("🏅 إنجاز: ${e.milestone} يومًا متتاليًا!", "🏅 Milestone: ${e.milestone} days in a row!"),
                        color = NebulaPurple, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                }
                if (e.earnedFreeze) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        language.pick("❄️ ربحتِ تجميدًا للسلسلة!", "❄️ You earned a streak freeze!"),
                        color = DiamondCyan, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.dismissStreakEvent() },
                colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
            ) { Text(language.pick("رائع!", tl("¡Genial!")), fontWeight = FontWeight.Bold, color = Color.White) }
        }
    )
}
