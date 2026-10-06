package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.engagement.EngagementStore
import com.example.data.engagement.LiaMood

/**
 * Home-screen widget: streak flame, today's XP goal, words waiting for review and a word of the day.
 * Reads the small snapshot [EngagementStore] keeps in SharedPreferences, so it never loads the course.
 */
class StreakWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, StreakWidget::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = EngagementStore.prefs(context)
            val today = EngagementStore.today()
            val streak = EngagementStore.readStreak(prefs)
            val arabic = EngagementStore.helperArabic(prefs)
            val xp = streak.xpOn(today)
            val goal = streak.dailyGoal
            val live = streak.liveStreak(today)
            val due = EngagementStore.dueCount(prefs, today)
            val word = EngagementStore.wordOfDay(prefs, today)

            val mood = streak.liaMood(today)
            val away = streak.daysAway(today)

            fun t(ar: String, en: String) = if (arabic) ar else en

            return RemoteViews(context.packageName, R.layout.widget_streak).apply {
                setImageViewResource(R.id.widget_lia, if (mood == LiaMood.SAD) R.drawable.lia_sad else R.drawable.lia_happy)
                setTextViewText(R.id.widget_streak_count, live.toString())
                setTextViewText(R.id.widget_streak_label, t("يوم متتالي", "day streak"))
                setTextViewText(
                    R.id.widget_flame,
                    when (mood) {
                        LiaMood.HAPPY -> "🔥"
                        LiaMood.WAITING -> "🕯️"
                        LiaMood.SAD -> "💧"
                    }
                )
                setTextViewText(
                    R.id.widget_goal,
                    when (mood) {
                        LiaMood.HAPPY -> t("✅ أنجزتِ هدف اليوم! ليا سعيدة", "✅ Goal done! Lía is happy")
                        LiaMood.SAD -> t("😢 ليا حزينة… $away أيام بلا دراسة", "😢 Lía misses you… $away days away")
                        LiaMood.WAITING -> t("الهدف: $xp / $goal نقطة", "Goal: $xp / $goal XP")
                    }
                )
                setProgressBar(R.id.widget_progress, goal, xp.coerceAtMost(goal), false)
                setTextViewText(
                    R.id.widget_due,
                    when {
                        due > 0 -> t("📚 $due كلمة للمراجعة", "📚 $due words to review")
                        EngagementStore.learnedCount(prefs) == 0 -> t("🌌 ابدئي مجرة الكلمات", "🌌 Start the Word Galaxy")
                        else -> t("✨ لا مراجعات الآن", "✨ No reviews due")
                    }
                )
                if (word != null) {
                    setViewVisibility(R.id.widget_word, View.VISIBLE)
                    setTextViewText(R.id.widget_word, "${word.first} · ${word.second}")
                } else {
                    setViewVisibility(R.id.widget_word, View.GONE)
                }
                val open = Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_GALAXY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context, 0, open,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }
}
