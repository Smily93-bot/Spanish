package com.example.data.engagement

/**
 * Daily streak rules, kept free of Android types so they can be unit-tested.
 *
 * A day "counts" once the player earns [dailyGoal] XP. Missing a day uses a streak freeze when one is
 * available; otherwise the streak resets. Days are epoch days in the player's local time zone.
 */
data class StreakState(
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** Last day the goal was met (or covered by a freeze); -1 when never. */
    val lastGoalDay: Long = -1,
    /** Day [todayXp] belongs to. */
    val xpDay: Long = -1,
    val todayXp: Int = 0,
    val dailyGoal: Int = DEFAULT_GOAL,
    val freezes: Int = 1,
    /** Recent days the goal was met. */
    val goalDays: Set<Long> = emptySet(),
    /** Recent days a freeze saved the streak. */
    val frozenDays: Set<Long> = emptySet()
) {
    fun xpOn(day: Long) = if (xpDay == day) todayXp else 0
    fun goalMetOn(day: Long) = lastGoalDay == day && day in goalDays

    /** Streak to show on [day]: still alive if the goal was met today or yesterday. */
    fun liveStreak(day: Long): Int = if (lastGoalDay >= day - 1 || coverableBy(day)) streak else 0

    private fun coverableBy(day: Long): Boolean {
        val missed = day - 1 - lastGoalDay
        return lastGoalDay >= 0 && missed in 1..freezes.toLong()
    }

    /** Applies missed days (freezes or reset) so that [lastGoalDay] is today or yesterday. */
    fun rollTo(day: Long): StreakState {
        if (lastGoalDay < 0 || lastGoalDay >= day - 1) return this
        val missed = (day - 1 - lastGoalDay).toInt()
        return if (missed <= freezes) {
            val saved = (1..missed).map { lastGoalDay + it }
            copy(
                freezes = freezes - missed,
                lastGoalDay = day - 1,
                frozenDays = (frozenDays + saved).recent(day)
            )
        } else {
            copy(streak = 0, lastGoalDay = -1)
        }
    }

    /** Adds earned XP on [day]; returns the new state and what happened. */
    fun addXp(day: Long, xp: Int): Pair<StreakState, StreakEvent> {
        val rolled = rollTo(day)
        val before = rolled.xpOn(day)
        val after = before + xp.coerceAtLeast(0)
        var next = rolled.copy(xpDay = day, todayXp = after)
        if (before >= dailyGoal || after < dailyGoal || rolled.lastGoalDay == day) {
            return next to StreakEvent.None
        }
        val newStreak = rolled.streak + 1
        val earnedFreeze = newStreak % FREEZE_EVERY == 0 && rolled.freezes < MAX_FREEZES
        next = next.copy(
            streak = newStreak,
            bestStreak = maxOf(rolled.bestStreak, newStreak),
            lastGoalDay = day,
            freezes = if (earnedFreeze) rolled.freezes + 1 else rolled.freezes,
            goalDays = (rolled.goalDays + day).recent(day)
        )
        val milestone = STREAK_MILESTONES.firstOrNull { it == newStreak }
        return next to StreakEvent.GoalReached(newStreak, earnedFreeze, milestone)
    }

    companion object {
        const val DEFAULT_GOAL = 50
        const val FREEZE_EVERY = 7
        const val MAX_FREEZES = 2
        val GOAL_CHOICES = listOf(20, 50, 100, 150)
        val STREAK_MILESTONES = listOf(3, 7, 14, 30, 50, 100, 200, 365)

        private fun Set<Long>.recent(day: Long) = filter { it > day - 60 }.toSet()
    }
}

sealed interface StreakEvent {
    data object None : StreakEvent
    data class GoalReached(val streak: Int, val earnedFreeze: Boolean, val milestone: Int?) : StreakEvent
}
