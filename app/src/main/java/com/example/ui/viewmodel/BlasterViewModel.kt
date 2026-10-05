package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectsEngine
import com.example.audio.SpeechSynthesizer
import com.example.data.content.SpanishContent
import com.example.data.database.*
import com.example.data.model.*
import com.example.data.repository.BlasterRepository
import com.example.data.repository.RewardResult
import com.example.ui.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MeteorGameState(
    val currentWord: MeteorWord? = null,
    val mode: BlasterMode = BlasterMode.TRANSLATION,
    val promptQuestion: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val score: Int = 0,
    val wordsBlasted: Int = 0,
    val maxStreak: Int = 0,
    val shield: Int = 100,
    val maxShield: Int = 100,
    val comboStreak: Int = 0,
    val isRunning: Boolean = false,
    val isGameOver: Boolean = false,
    val lastHitEffect: String? = null,
    val personalBest: Int = 0,
    val isNewPersonalBest: Boolean = false,
    /** Changes every time a new meteor spawns so the UI restarts its fall animation. */
    val roundId: Int = 0,
    val fallDurationMs: Int = 9000,
    /** Options the player already shot wrongly this round (disabled in the UI). */
    val wrongPicks: Set<Int> = emptySet(),
    /** Index of the meteor that was just destroyed, for the explosion animation. */
    val blastedIndex: Int? = null,
    val reward: RewardResult? = null
)

class BlasterViewModel(
    application: Application,
    private val repository: BlasterRepository
) : AndroidViewModel(application) {

    val soundEngine = SoundEffectsEngine()
    val speechEngine = SpeechSynthesizer(application)

    private val prefs = application.getSharedPreferences("spanish_blaster", Context.MODE_PRIVATE)

    val userProgress: StateFlow<UserProgressEntity?> = repository.userProgress
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tabletProgress: StateFlow<List<TabletProgressEntity>> = repository.tabletProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentArcadeScores: StateFlow<List<ArcadeScoreEntity>> = repository.recentArcadeScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topArcadeScores: StateFlow<List<ArcadeScoreEntity>> = repository.topArcadeHighScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalBestScore: StateFlow<Int> = repository.overallPersonalBest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val arcadeStats: StateFlow<List<ArcadeModeStatsEntity>> = repository.arcadeStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordMastery: StateFlow<List<WordMasteryEntity>> = repository.wordMastery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val milestones: StateFlow<List<MilestoneGoalEntity>> = repository.milestones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _content = MutableStateFlow<SpanishContent?>(null)
    /** Bundled offline course content; null while it loads from assets on first launch. */
    val content: StateFlow<SpanishContent?> = _content.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.CommandBridge)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedTabletId = MutableStateFlow<String?>(null)
    val selectedTabletId: StateFlow<String?> = _selectedTabletId.asStateFlow()

    private val _helperLanguage = MutableStateFlow(
        if (prefs.getString(KEY_HELPER, "ARABIC") == "ENGLISH") HelperLanguage.ENGLISH else HelperLanguage.ARABIC
    )
    val helperLanguage: StateFlow<HelperLanguage> = _helperLanguage.asStateFlow()

    private val _meteorState = MutableStateFlow(MeteorGameState())
    val meteorState: StateFlow<MeteorGameState> = _meteorState.asStateFlow()

    private val _localHighScores = MutableStateFlow<List<ArcadeScoreEntity>>(emptyList())
    val localHighScores: StateFlow<List<ArcadeScoreEntity>> = _localHighScores.asStateFlow()

    private val _selectedHighScoreFilter = MutableStateFlow("ALL")
    val selectedHighScoreFilter: StateFlow<String> = _selectedHighScoreFilter.asStateFlow()

    private val _showHighScoresDialog = MutableStateFlow(false)
    val showHighScoresDialog: StateFlow<Boolean> = _showHighScoresDialog.asStateFlow()

    private val _arcadePersonalBest = MutableStateFlow(0)
    val arcadePersonalBest: StateFlow<Int> = _arcadePersonalBest.asStateFlow()

    /** Reward popup shared by tablets, cloze and reactor runs. */
    private val _practiceReward = MutableStateFlow<RewardResult?>(null)
    val practiceReward: StateFlow<RewardResult?> = _practiceReward.asStateFlow()

    private var impactJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            _content.value = repository.loadContent(application)
        }
        viewModelScope.launch {
            userProgress.filterNotNull().collect {
                soundEngine.enabled = it.soundEnabled
                speechEngine.speechRate = it.speechSpeed
            }
        }
    }

    // ------------------------------------------------------------------ High scores

    /**
     * Fetches and displays local high scores from the Vocabulary Arcade database,
     * showing the user's personal best to encourage replayability.
     */
    fun fetchLocalHighScores(mode: String? = null) {
        val filterMode = mode ?: _selectedHighScoreFilter.value
        _selectedHighScoreFilter.value = filterMode
        viewModelScope.launch {
            val scores = repository.fetchLocalHighScores(filterMode)
            _localHighScores.value = scores
            val pb = repository.fetchPersonalBest(filterMode)
            _arcadePersonalBest.value = pb
            _showHighScoresDialog.value = true
        }
    }

    fun fetchAndDisplayHighScores(mode: String? = null) = fetchLocalHighScores(mode)

    fun dismissHighScoresDialog() {
        _showHighScoresDialog.value = false
    }

    fun setHighScoreFilter(mode: String) {
        _selectedHighScoreFilter.value = mode
        fetchLocalHighScores(mode)
    }

    // ------------------------------------------------------------------ Navigation & settings

    fun toggleHelperLanguage() {
        _helperLanguage.value = if (_helperLanguage.value == HelperLanguage.ARABIC) {
            HelperLanguage.ENGLISH
        } else {
            HelperLanguage.ARABIC
        }
        prefs.edit().putString(KEY_HELPER, _helperLanguage.value.name).apply()
    }

    fun navigateTo(screen: Screen) {
        if (screen != Screen.MeteorBlaster) pauseMeteorGame()
        soundEngine.click()
        _currentScreen.value = screen
    }

    fun openTablet(tabletId: String?) {
        _selectedTabletId.value = tabletId
        navigateTo(Screen.TabletCodex)
    }

    fun closeTablet() {
        _selectedTabletId.value = null
    }

    /** Returns false when already on the home screen so the system can close the app. */
    fun navigateBack(): Boolean {
        if (_currentScreen.value == Screen.TabletCodex && _selectedTabletId.value != null) {
            _selectedTabletId.value = null
            return true
        }
        if (_currentScreen.value != Screen.CommandBridge) {
            navigateTo(Screen.CommandBridge)
            return true
        }
        return false
    }

    fun speakSpanish(text: String) {
        speechEngine.speakSpanish(text)
    }

    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { repository.setSoundEnabled(enabled) }

    fun setSpeechSpeed(speed: Float) = viewModelScope.launch { repository.setSpeechSpeed(speed) }

    fun upgradeShip() = viewModelScope.launch {
        if (repository.upgradeShip()) soundEngine.powerUp() else soundEngine.error()
    }

    // ------------------------------------------------------------------ Practice modes

    fun recordWord(word: VocabWord, correct: Boolean) = viewModelScope.launch {
        repository.recordWordResult(word.shortSpanish, word.english, word.category, correct)
    }

    fun recordWord(spanish: String, english: String, category: String, correct: Boolean) = viewModelScope.launch {
        repository.recordWordResult(spanish, english, category, correct)
    }

    fun completeTablet(tablet: ReadingTablet, correct: Int, total: Int) = viewModelScope.launch {
        soundEngine.fanfare()
        _practiceReward.value = repository.completeTablet(tablet, correct, total)
    }

    /** Saves a Quantum Cloze or Grammar Reactor run to the arcade table. */
    fun finishPracticeRun(gameMode: String, score: Int, correctCount: Int, bestStreak: Int) = viewModelScope.launch {
        soundEngine.fanfare()
        _practiceReward.value = repository.recordArcadeRun(gameMode, score, correctCount, bestStreak)
    }

    fun dismissPracticeReward() {
        _practiceReward.value = null
    }

    // ------------------------------------------------------------------ Meteor Blaster

    fun startMeteorGame(mode: BlasterMode) {
        val content = _content.value ?: return
        impactJob?.cancel()
        val maxShield = shipTier(userProgress.value?.shipTier ?: 1).maxShield
        viewModelScope.launch {
            val pb = repository.fetchPersonalBest(mode.name)
            _meteorState.value = MeteorGameState(
                mode = mode,
                shield = maxShield,
                maxShield = maxShield,
                isRunning = true,
                personalBest = pb
            )
            nextMeteor(content)
        }
    }

    fun shootOption(index: Int) {
        val state = _meteorState.value
        val word = state.currentWord ?: return
        if (!state.isRunning || state.isGameOver || state.blastedIndex != null || index in state.wrongPicks) return
        soundEngine.laser()
        if (index == state.correctIndex) {
            impactJob?.cancel()
            soundEngine.hit()
            val combo = state.comboStreak + 1
            val points = 10 * userLevelMultiplier() + combo * 5 + if (state.wrongPicks.isEmpty()) 10 else 0
            _meteorState.value = state.copy(
                score = state.score + points,
                wordsBlasted = state.wordsBlasted + 1,
                comboStreak = combo,
                maxStreak = maxOf(state.maxStreak, combo),
                blastedIndex = index,
                lastHitEffect = if (combo >= 3) "¡COMBO x$combo! +$points" else "¡Impacto! +$points"
            )
            recordWord(word.spanishToSpeak, word.englishMeaning, word.category, state.wrongPicks.isEmpty())
            speechEngine.speakSpanish(word.spanishToSpeak)
            val content = _content.value ?: return
            viewModelScope.launch {
                delay(650)
                if (_meteorState.value.isRunning) nextMeteor(content)
            }
        } else {
            soundEngine.error()
            damageShield(20, state.copy(wrongPicks = state.wrongPicks + index, comboStreak = 0, lastHitEffect = "¡Fallaste! −20 🛡️"))
        }
    }

    fun pauseMeteorGame() {
        if (_meteorState.value.isRunning && !_meteorState.value.isGameOver) {
            impactJob?.cancel()
            _meteorState.value = MeteorGameState(mode = _meteorState.value.mode)
        }
    }

    fun exitMeteorGame() {
        impactJob?.cancel()
        _meteorState.value = MeteorGameState(mode = _meteorState.value.mode)
    }

    private fun userLevelMultiplier() = 1 + (userProgress.value?.level ?: 1) / 5

    private fun nextMeteor(content: SpanishContent) {
        val state = _meteorState.value
        val word = content.meteorRound(state.mode, _helperLanguage.value, userProgress.value?.level ?: 1)
        val duration = (9000 - state.wordsBlasted * 180).coerceAtLeast(3800)
        val prompt = when (state.mode) {
            BlasterMode.TRANSLATION -> _helperLanguage.value.pick("ما معنى هذه الكلمة؟", "What does this mean?")
            BlasterMode.SYNONYM -> _helperLanguage.value.pick("اختاري المرادف", "Blast the synonym")
            BlasterMode.ANTONYM -> _helperLanguage.value.pick("اختاري الضد", "Blast the opposite")
        }
        _meteorState.value = state.copy(
            currentWord = word,
            promptQuestion = prompt,
            options = word.options,
            correctIndex = word.options.indexOf(word.answer),
            roundId = state.roundId + 1,
            fallDurationMs = duration,
            wrongPicks = emptySet(),
            blastedIndex = null
        )
        impactJob?.cancel()
        impactJob = viewModelScope.launch {
            delay(duration.toLong())
            onMeteorImpact()
        }
    }

    private fun onMeteorImpact() {
        val state = _meteorState.value
        val word = state.currentWord ?: return
        if (!state.isRunning) return
        soundEngine.impact()
        recordWord(word.spanishToSpeak, word.englishMeaning, word.category, false)
        val updated = damageShield(
            30,
            state.copy(comboStreak = 0, lastHitEffect = "💥 ${word.prompt} = ${word.answer}")
        )
        if (!updated.isGameOver) {
            val content = _content.value ?: return
            viewModelScope.launch {
                delay(1200)
                if (_meteorState.value.isRunning && !_meteorState.value.isGameOver) nextMeteor(content)
            }
        }
    }

    private fun damageShield(amount: Int, state: MeteorGameState): MeteorGameState {
        val shield = (state.shield - amount).coerceAtLeast(0)
        if (shield > 0) {
            return state.copy(shield = shield).also { _meteorState.value = it }
        }
        impactJob?.cancel()
        val over = state.copy(shield = 0, isGameOver = true, isRunning = false)
        _meteorState.value = over
        viewModelScope.launch {
            val reward = repository.recordArcadeRun(over.mode.name, over.score, over.wordsBlasted, over.maxStreak)
            if (reward.isNewPersonalBest) soundEngine.fanfare()
            _meteorState.value = _meteorState.value.copy(
                isNewPersonalBest = reward.isNewPersonalBest,
                personalBest = maxOf(over.personalBest, over.score),
                reward = reward
            )
        }
        return over
    }

    override fun onCleared() {
        impactJob?.cancel()
        speechEngine.shutdown()
        soundEngine.release()
        super.onCleared()
    }

    private companion object {
        const val KEY_HELPER = "helper_language"
    }
}
