# Spanish Blaster 🚀🇪🇸 · Italian Blaster 🚀🇮🇹

Space-adventure Android games for learning **Spanish** or **Italian**, with Arabic or English as the helper language.

This one project builds **two separate apps** (Gradle product flavors), each with its own name, icon label,
install ID and Google Play listing:

| App | Flavor | Application ID | Content |
| --- | --- | --- | --- |
| Spanish Blaster | `spanish` | `com.bluediamond.spanishblaster.app` | `app/src/spanish/assets/` |
| Italian Blaster | `italian` | `com.bluediamond.italianblaster.app` | `app/src/italian/assets/` |

Both apps share all the game code in `app/src/main/`. Each flavor adds its own content, app name and a
`TargetLanguage.kt` (text-to-speech voice, synonym/antonym pairs and the on-screen target-language text).
**It runs fully offline. No Gemini key, no API key and no account are needed.** All lessons, words
and stories ship inside the app.

## What's inside

| Screen | What you do |
| --- | --- |
| **Command Bridge** | Home: rank, XP, word of the day with audio, quick-launch missions |
| **Adventure Map** | 6 galactic sectors (A1 → C2) with the 12 Órbita story chapters |
| **Lía's Expeditions** | Walk Lía (animated sprite) with her co-pilot **Nilo** (who follows her, jumps after her, introduces each mission and gives hints in Spanish) through each chapter world, jump for diamonds and stop at glowing word wisps for 7 missions: story, questions, a hidden-object search (tap "la llave" in the cabin), grammar console, sentence puzzle, mission report and the portal to the atlas page |
| **Meteor Blaster** | Arcade: tap the falling meteor with the right answer. Modes: Meaning, Synonyms, Antonyms. Shield, combos, personal bests |
| **Grammar Reactor** | Put shuffled words in order to rebuild real Spanish sentences |
| **Quantum Cloze** | Fill the missing word in sentences from the Frequency 5000 list before time runs out |
| **Hangar & Goals** | Spend star credits on ship upgrades (more shield), 14 milestones, Hall of Fame |
| **Cadet Logbook** | Words you've practised with mastery stars, a searchable Spanish/English/Arabic dictionary, 20 grammar guides, settings |

### Italian content
- `vocab.json`: 13 topic collections, the **Italian Frequency 5000** list (ranked from film-subtitle word counts, reduced to dictionary forms, each word with English and Arabic meanings and an Italian/English/Arabic example), 136 course phrases and 20 Italian grammar guides
- `campaign.json`: the same 12 Órbita chapters, rewritten in Italian with Italian grammar (essere, passato prossimo, imperfetto, congiuntivo, periodo ipotetico…)

### Spanish content (from the Órbita + Parliva source package)
- `app/src/main/assets/vocab.json`: 13 topic collections, the **Frequency 5000** list (with Spanish, English and Arabic examples), 136 course phrases and 20 grammar guides
- `app/src/main/assets/campaign.json`: the 12 Órbita chapters (A1.1 → C2.2), with stories, questions, grammar tables and sentence puzzles

### Offline tech
- **Pronunciation**: Android's built-in `TextToSpeech` with a Spanish voice. If the device has none, install one under
  *Settings → Text-to-speech → Install voice data → Español*.
- **Sound effects**: synthesised in code with `AudioTrack` (no audio files).
- **Progress**: stored on the device in a Room database (XP, levels, high scores, word mastery, milestones).

## Getting the APK

Every push runs **GitHub Actions → "Build Android APK"**. Open the latest run and download the
`spanish-blaster-apk` or `italian-blaster-apk` artifact. Each contains a debug APK, a release APK and a release
`.aab` bundle (for Google Play). Unzip it and install the APK on your phone (allow "install unknown apps").

### Publishing on Google Play
The two apps are separate Play listings: create one app in the Play Console for each application ID and
upload that flavor's `.aab`.

Out of the box, release builds are signed with the public test key in the repo. Before publishing, create a
private upload key and add it as repository secrets (Settings → Secrets and variables → Actions):

```bash
keytool -genkeypair -v -keystore play.keystore -alias upload -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 play.keystore   # paste the output into PLAY_KEYSTORE_BASE64
```

| Secret | Value |
| --- | --- |
| `PLAY_KEYSTORE_BASE64` | the base64 text of `play.keystore` |
| `PLAY_KEYSTORE_PASSWORD` | keystore password |
| `PLAY_KEY_ALIAS` | `upload` (or the alias you chose) |
| `PLAY_KEY_PASSWORD` | key password |

When these secrets exist, CI signs both apps' release APKs and bundles with your key. Keep `play.keystore` safe
and out of git; Google Play needs the same upload key for every update.

## Building locally
Requirements: Android Studio (Ladybug or newer) or JDK 17 plus the Android SDK (API 35).

```bash
./gradlew assembleSpanishDebug assembleItalianDebug   # app/build/outputs/apk/<flavor>/debug/
./gradlew bundleItalianRelease                          # Play bundle for Italian Blaster
./gradlew testSpanishDebugUnitTest testItalianDebugUnitTest
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
