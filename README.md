# Spanish Blaster 🚀🇪🇸

A space-adventure Android game for learning Spanish, with Arabic or English as the helper language.
**It runs fully offline. No Gemini key, no API key and no account are needed.** All lessons, words
and stories ship inside the app.

## What's inside

| Screen | What you do |
| --- | --- |
| **Command Bridge** | Home: rank, XP, word of the day with audio, quick-launch missions |
| **Adventure Map** | 6 galactic sectors (A1 → C2) with the 12 Órbita story chapters |
| **Reading Tablets** | Read a story (tap any sentence to hear it), answer written questions, repair a conjugation table, rebuild a sentence, finish the mission |
| **Meteor Blaster** | Arcade: tap the falling meteor with the right answer. Modes: Meaning, Synonyms, Antonyms. Shield, combos, personal bests |
| **Grammar Reactor** | Put shuffled words in order to rebuild real Spanish sentences |
| **Quantum Cloze** | Fill the missing word in sentences from the Frequency 5000 list before time runs out |
| **Hangar & Goals** | Spend star credits on ship upgrades (more shield), 14 milestones, Hall of Fame |
| **Cadet Logbook** | Words you've practised with mastery stars, a searchable Spanish/English/Arabic dictionary, 20 grammar guides, settings |

### Content (from the Órbita + Parliva source package)
- `app/src/main/assets/vocab.json`: 13 topic collections, the **Frequency 5000** list (with Spanish, English and Arabic examples), 136 course phrases and 20 grammar guides
- `app/src/main/assets/campaign.json`: the 12 Órbita chapters (A1.1 → C2.2), with stories, questions, grammar tables and sentence puzzles

### Offline tech
- **Pronunciation**: Android's built-in `TextToSpeech` with a Spanish voice. If the device has none, install one under
  *Settings → Text-to-speech → Install voice data → Español*.
- **Sound effects**: synthesised in code with `AudioTrack` (no audio files).
- **Progress**: stored on the device in a Room database (XP, levels, high scores, word mastery, milestones).

## Getting the APK

Every push runs **GitHub Actions → "Build Android APK"**. Open the latest run and download the
`spanish-blaster-apk` artifact. It contains a debug APK and a release APK. Unzip it and install the APK on
your phone (allow "install unknown apps").

> The release APK is signed with the debug key so it installs directly. Before publishing on Google Play,
> create your own keystore and replace `signingConfig` in `app/build.gradle.kts`.

## Building locally
Requirements: Android Studio (Ladybug or newer) or JDK 17 plus the Android SDK (API 35).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
```

## Project structure
```
app/src/main/java/com/example/
├── MainActivity.kt                  // Entry point, HUD top bar, bottom navigation, theme
├── audio/                           // SoundEffectsEngine (synth SFX), SpeechSynthesizer (TTS)
├── data/
│   ├── content/SpanishContent.kt    // Loads bundled JSON, builds meteor/cloze questions, synonym & antonym lists
│   ├── database/                    // Room: entities, DAO, database
│   ├── model/Models.kt              // Domain models (tablets, words, ranks, ships…)
│   └── repository/BlasterRepository.kt  // Scoring, XP/levels, milestones, persistence
└── ui/
    ├── navigation/  viewmodel/  components/  theme/
    └── screens/                     // The 8 screens listed above
```
