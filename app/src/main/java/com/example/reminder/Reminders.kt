package com.example.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.R
import com.example.data.engagement.EngagementStore
import com.example.widget.StreakWidget
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules the once-a-day "keep your streak" reminder. */
object Reminders {
    private const val CHANNEL = "daily_streak"
    private const val NOTIFICATION_ID = 7001

    fun schedule(context: Context) {
        val settings = EngagementStore.reminderSettings(EngagementStore.prefs(context))
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        if (!settings.enabled) return
        val now = LocalDateTime.now()
        var at = now.toLocalDate().atTime(settings.hour, 0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact on purpose: no exact-alarm permission is needed and a few minutes' drift is fine.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun notifyIfNeeded(context: Context) {
        val prefs = EngagementStore.prefs(context)
        val today = LocalDate.now().toEpochDay()
        val streak = EngagementStore.readStreak(prefs)
        if (streak.goalMetOn(today)) return
        val arabic = EngagementStore.helperArabic(prefs)
        val live = streak.liveStreak(today)
        val due = EngagementStore.dueCount(prefs, today)
        val sad = streak.liaMood(today) == com.example.data.engagement.LiaMood.SAD
        val title = when {
            sad && arabic -> "😢 ليا حزينة، تفتقدك!"
            sad -> "😢 Lía is sad, she misses you!"
            live > 0 && arabic -> "🔥 حافظ على سلسلة $live يوم!"
            live > 0 -> "🔥 Keep your $live-day streak alive!"
            arabic -> "🚀 ليا ونيلو بانتظارك"
            else -> "🚀 Lía and Nilo are waiting"
        }
        val body = when {
            due > 0 && arabic -> "لديك $due كلمة للمراجعة. بضع دقائق تكفي."
            due > 0 -> "You have $due words to review. A few minutes is enough."
            arabic -> "تعلّم 5 كلمات جديدة اليوم مع نيلو."
            else -> "Learn 5 new words with Nilo today."
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                if (arabic) "تذكير السلسلة اليومية" else "Daily streak reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_GALAXY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/** Fires at the reminder hour: notifies when today's goal isn't met, then schedules tomorrow. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.notifyIfNeeded(context)
        StreakWidget.requestUpdate(context)
        Reminders.schedule(context)
    }
}

/** Alarms are cleared on reboot and app update, so set the reminder again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context)
        StreakWidget.requestUpdate(context)
    }
}
