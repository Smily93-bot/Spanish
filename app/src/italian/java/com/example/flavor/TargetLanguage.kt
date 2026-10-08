package com.example.flavor

import com.example.data.content.ant
import com.example.data.content.syn
import java.util.Locale

/** Everything that makes this build Italian Blaster: the TTS voice and the synonym/antonym pairs. */
object TargetLanguage {
    /** Voices tried in order; the first one the device has is used. */
    val voiceLocales = listOf(Locale("it", "IT"), Locale("it", "CH"), Locale("it"))

    val synonyms = listOf(
        syn("bello", "carino", "pretty / nice", "جميل"),
        syn("veloce", "rapido", "fast", "سريع"),
        syn("contento", "felice", "happy", "مسرور"),
        syn("iniziare", "cominciare", "to begin", "يبدأ"),
        syn("finire", "terminare", "to finish", "يُنهي"),
        syn("casa", "abitazione", "house / home", "بيت"),
        syn("macchina", "auto", "car", "سيارة"),
        syn("guardare", "osservare", "to look / to observe", "ينظر"),
        syn("camminare", "passeggiare", "to walk", "يمشي"),
        syn("enorme", "gigantesco", "huge", "ضخم"),
        syn("intelligente", "sveglio", "clever", "ذكي"),
        syn("facile", "semplice", "easy / simple", "سهل"),
        syn("difficile", "complicato", "difficult / complicated", "صعب"),
        syn("faccia", "viso", "face", "وجه"),
        syn("tornare", "ritornare", "to return", "يعود"),
        syn("ottenere", "raggiungere", "to achieve", "يحقق"),
        syn("allievo", "studente", "student", "طالب"),
        syn("lavoro", "impiego", "job", "عمل"),
        syn("lingua", "idioma", "language", "لغة"),
        syn("magro", "snello", "thin", "نحيف"),
        syn("arrabbiato", "furioso", "angry", "غاضب"),
        syn("rispondere", "replicare", "to answer", "يجيب"),
        syn("volere", "desiderare", "to want / to wish", "يريد"),
        syn("scegliere", "selezionare", "to choose", "يختار"),
        syn("cibo", "alimento", "food", "طعام"),
        syn("anziano", "vecchio", "old (person)", "مُسن"),
        syn("nave", "imbarcazione", "ship", "سفينة"),
        syn("stanco", "esausto", "tired / exhausted", "متعب"),
        syn("subito", "immediatamente", "right away", "فورًا"),
        syn("ricco", "benestante", "rich", "غني"),
        syn("paura", "timore", "fear", "خوف"),
        syn("ragazzo", "giovane", "boy / young man", "شاب"),
        syn("parlare", "conversare", "to talk", "يتحدث"),
        syn("aiutare", "assistere", "to help", "يساعد"),
        syn("luogo", "posto", "place", "مكان"),
        syn("soldi", "denaro", "money", "مال"),
        syn("errore", "sbaglio", "mistake", "خطأ"),
        syn("capire", "comprendere", "to understand", "يفهم")
    )

    val antonyms = listOf(
        ant("grande", "piccolo", "big", "small", "كبير", "صغير"),
        ant("felice", "triste", "happy", "sad", "سعيد", "حزين"),
        ant("sopra", "sotto", "above", "below", "فوق", "تحت"),
        ant("veloce", "lento", "fast", "slow", "سريع", "بطيء"),
        ant("nuovo", "vecchio", "new", "old", "جديد", "قديم"),
        ant("facile", "difficile", "easy", "difficult", "سهل", "صعب"),
        ant("entrare", "uscire", "to enter", "to leave", "يدخل", "يخرج"),
        ant("aprire", "chiudere", "to open", "to close", "يفتح", "يغلق"),
        ant("alto", "basso", "tall", "short", "طويل", "قصير"),
        ant("vicino", "lontano", "near", "far", "قريب", "بعيد"),
        ant("caldo", "freddo", "hot", "cold", "ساخن", "بارد"),
        ant("dentro", "fuori", "inside", "outside", "داخل", "خارج"),
        ant("giorno", "notte", "day", "night", "نهار", "ليل"),
        ant("comprare", "vendere", "to buy", "to sell", "يشتري", "يبيع"),
        ant("prima", "dopo", "before", "after", "قبل", "بعد"),
        ant("sempre", "mai", "always", "never", "دائمًا", "أبدًا"),
        ant("buono", "cattivo", "good", "bad", "جيد", "سيئ"),
        ant("molto", "poco", "a lot", "a little", "كثير", "قليل"),
        ant("vincere", "perdere", "to win", "to lose", "يفوز", "يخسر"),
        ant("ricco", "povero", "rich", "poor", "غني", "فقير"),
        ant("pulito", "sporco", "clean", "dirty", "نظيف", "متسخ"),
        ant("pieno", "vuoto", "full", "empty", "ممتلئ", "فارغ"),
        ant("chiaro", "scuro", "light", "dark", "فاتح", "مظلم"),
        ant("forte", "debole", "strong", "weak", "قوي", "ضعيف"),
        ant("salire", "scendere", "to go up", "to go down", "يصعد", "ينزل"),
        ant("accendere", "spegnere", "to switch on", "to switch off", "يُشغّل", "يُطفئ"),
        ant("ricordare", "dimenticare", "to remember", "to forget", "يتذكر", "ينسى"),
        ant("accettare", "rifiutare", "to accept", "to reject", "يقبل", "يرفض"),
        ant("giovane", "anziano", "young", "elderly", "شاب", "مُسن"),
        ant("presto", "tardi", "early", "late", "باكرًا", "متأخرًا"),
        ant("verità", "bugia", "truth", "lie", "حقيقة", "كذبة"),
        ant("amore", "odio", "love", "hate", "حب", "كره"),
        ant("domandare", "rispondere", "to ask", "to answer", "يسأل", "يجيب"),
        ant("largo", "stretto", "wide", "narrow", "عريض", "ضيق"),
        ant("caro", "economico", "expensive", "cheap", "غالٍ", "رخيص"),
        ant("primo", "ultimo", "first", "last", "أول", "آخر"),
        ant("migliore", "peggiore", "better", "worse", "أفضل", "أسوأ"),
        ant("rumore", "silenzio", "noise", "silence", "ضجيج", "صمت"),
        ant("arrivare", "partire", "to arrive", "to depart", "يصل", "يغادر"),
        ant("guerra", "pace", "war", "peace", "حرب", "سلام")
    )
}

/**
 * On-screen text in the target language. The shared code is written with the Spanish text as the
 * key; this build swaps in the Italian version (or leaves the text as it is when no entry exists).
 */
fun tl(text: String): String = ITALIAN[text] ?: text

private val ITALIAN = mapOf(
    // App title and language names
    "SPANISH BLASTER" to "ITALIAN BLASTER",
    "مستكشف الإسبانية" to "مستكشف الإيطالية",
    "¡Hola! Bienvenida a Spanish Blaster." to "Ciao! Benvenuta in Italian Blaster.",
    "Build this sentence in Spanish:" to "Build this sentence in Italian:",
    "ابني هذه الجملة بالإسبانية:" to "ابني هذه الجملة بالإيطالية:",
    "Escribe en español" to "Scrivi in italiano",
    "اكتبي الإجابة بالإسبانية" to "اكتبي الإجابة بالإيطالية",
    "No Spanish voice installed on this device." to "No Italian voice installed on this device.",
    "لا يوجد صوت إسباني مثبت على الجهاز." to "لا يوجد صوت إيطالي مثبت على الجهاز.",
    "Search in Spanish or English…" to "Search in Italian or English…",
    "ابحثي بالإسبانية أو العربية…" to "ابحثي بالإيطالية أو العربية…",
    "Spanish word → its meaning" to "Italian word → its meaning",
    "كلمة إسبانية ← معناها" to "كلمة إيطالية ← معناها",
    "Pick the Spanish synonym" to "Pick the Italian synonym",
    "اختاري الكلمة الإسبانية المرادفة" to "اختاري الكلمة الإيطالية المرادفة",
    "Pick the Spanish opposite" to "Pick the Italian opposite",
    "اختاري الكلمة الإسبانية المضادة" to "اختاري الكلمة الإيطالية المضادة",
    "🔈 No Spanish voice found. Install one in Settings → Text-to-speech → Install voice data → Español." to
        "🔈 No Italian voice found. Install one in Settings → Text-to-speech → Install voice data → Italiano.",
    "🔈 لا يوجد صوت إسباني على جهازك. ثبّتيه من: الإعدادات ← تحويل النص إلى كلام ← تثبيت بيانات الصوت ← Español." to
        "🔈 لا يوجد صوت إيطالي على جهازك. ثبّتيه من: الإعدادات ← تحويل النص إلى كلام ← تثبيت بيانات الصوت ← Italiano.",

    // Game modes, ranks, ships
    "Significado" to "Significato",
    "Sinónimos" to "Sinonimi",
    "Antónimos" to "Contrari",
    "Todos" to "Tutti",
    "Huecos" to "Spazi vuoti",
    "Frases" to "Frasi",
    "Cadete" to "Cadetta",
    "Exploradora" to "Esploratrice",
    "Piloto" to "Pilota",
    "Capitana" to "Capitana",
    "Comandante" to "Comandante",
    "Almirante galáctica" to "Ammiraglia galattica",
    "Colibrí" to "Colibrì",
    "Halcón" to "Falco",
    "Cóndor" to "Condor",
    "Quetzal" to "Quetzal",
    "Estrella Azul" to "Stella Azzurra",

    // Milestones
    "Primer vuelo" to "Primo volo",
    "Piloto constante" to "Pilota costante",
    "Leyenda del arcade" to "Leggenda dell'arcade",
    "Lluvia de meteoros" to "Pioggia di meteore",
    "Rango S+" to "Grado S+",
    "Primera señal" to "Primo segnale",
    "Mitad del atlas" to "Metà dell'atlante",
    "Vuelo a casa" to "Volo verso casa",
    "Léxico en marcha" to "Lessico in marcia",
    "Cien palabras" to "Cento parole",
    "Diccionario viviente" to "Dizionario vivente",
    "Nivel 5" to "Livello 5",
    "Nivel 10" to "Livello 10",
    "Nave mejorada" to "Nave potenziata",

    // Shared components
    "Escuchar" to "Ascolta",
    "Cargando la galaxia…" to "Caricamento della galassia…",
    "Rango: " to "Grado: ",
    "🏆 ¡Nuevo récord personal!" to "🏆 Nuovo record personale!",
    "🚀 ¡Subiste al nivel %d! (+50 ⭐)" to "🚀 Sei salita al livello %d! (+50 ⭐)",
    "Continuar" to "Continua",
    "SALÓN DE RÉCORDS LOCALES" to "SALA DEI RECORD LOCALI",
    "TU RÉCORD PERSONAL MÁXIMO" to "IL TUO RECORD PERSONALE",
    "Sin récord" to "Nessun record",
    "🚀 ¡Supera tu récord en tu próxima partida!" to "🚀 Batti il tuo record nella prossima partita!",
    "✨ ¡Juega una partida para registrar tu primera marca!" to "✨ Gioca una partita per registrare il tuo primo record!",
    "Aún no hay partidas en este modo." to "Ancora nessuna partita in questa modalità.",
    "palabras" to "parole",
    "Racha" to "Serie",
    "JUGAR (NUEVO RÉCORD)" to "GIOCA (NUOVO RECORD)",
    "Cerrar" to "Chiudi",

    // Adventure map
    "Sector Amanecer" to "Settore Alba",
    "Cinturón de Archivos" to "Cintura degli Archivi",
    "Jardín de Memorias" to "Giardino dei Ricordi",
    "Nebulosa Condicional" to "Nebulosa Condizionale",
    "Observatorio del Tiempo" to "Osservatorio del Tempo",
    "Consejo de Mundos" to "Consiglio dei Mondi",
    "ATLAS DE ÓRBITA" to "ATLANTE DI ORBITA",
    "páginas" to "pagine",

    // Cadet logbook
    "Léxico" to "Lessico",
    "Diccionario" to "Dizionario",
    "Gramática" to "Grammatica",
    "Ajustes" to "Impostazioni",
    "BITÁCORA DE CADETE" to "DIARIO DI BORDO",
    "Nivel" to "Livello",
    "Probar voz" to "Prova la voce",

    // Command bridge
    "¡Hola, exploradora!" to "Ciao, esploratrice!",
    "NIVEL" to "LIVELLO",
    "Nv" to "Liv",
    "Créditos" to "Crediti",
    "Tablillas" to "Tavolette",
    "Palabras" to "Parole",
    "Récord" to "Record",
    "PALABRA DEL DÍA" to "PAROLA DEL GIORNO",
    "MISIONES" to "MISSIONI",
    "Reactor Gramatical" to "Reattore Grammaticale",
    "Cloze Cuántico" to "Cloze Quantistico",
    "Mapa galáctico" to "Mappa galattica",

    // Expeditions
    "Transmisión" to "Trasmissione",
    "Primeras preguntas" to "Prime domande",
    "Búsqueda" to "Ricerca",
    "Consola gramatical" to "Console grammaticale",
    "Mensaje roto" to "Messaggio rotto",
    "Misión" to "Missione",
    "🌀 ¡El portal está abierto! Camina hacia él" to "🌀 Il portale è aperto! Cammina verso di esso",
    "✨ ¡Bien hecho! Sigue caminando ▶" to "✨ Ben fatto! Continua a camminare ▶",
    "PÁGINA" to "PAGINA",
    "SALTO" to "SALTA",
    "📡 Transmisión entrante" to "📡 Trasmissione in arrivo",
    "🛰️ Misión" to "🛰️ Missione",
    "📖 Final del capítulo" to "📖 Fine del capitolo",
    "Recompensa: " to "Ricompensa: ",
    "Abrir el portal 🌀" to "Apri il portale 🌀",
    "¡Hecho! Seguir caminando ▶" to "Fatto! Continua a camminare ▶",

    // Grammar reactor and quantum cloze
    "⚛️ Reactor Gramatical" to "⚛️ Reattore Grammaticale",
    "⚛️ ¡Reactor cargado!" to "⚛️ Reattore carico!",
    "Elige tu nivel" to "Scegli il tuo livello",
    "Activar reactor" to "Attiva il reattore",
    "Ver la solución" to "Vedi la soluzione",
    "Siguiente ›" to "Avanti ›",
    "Terminar" to "Termina",
    "🌀 Cloze Cuántico" to "🌀 Cloze Quantistico",
    "🌀 ¡Campo cuántico estabilizado!" to "🌀 Campo quantistico stabilizzato!",
    "✅ ¡Correcto!" to "✅ Corretto!",
    "⏱️ ¡Tiempo!" to "⏱️ Tempo scaduto!",
    "❌ Respuesta: " to "❌ Risposta: ",

    // Hangar
    "Mejorar a" to "Potenzia a",
    "💎 ¡Tu nave está al máximo!" to "💎 La tua nave è al massimo!",
    "SALÓN DE LA FAMA" to "HALL OF FAME",
    "Ver récords locales" to "Vedi i record locali",
    "PARTIDAS RECIENTES" to "PARTITE RECENTI",
    "LOGROS" to "TRAGUARDI",
    "desbloqueados" to "sbloccati",

    // Meteor blaster
    "Salón de récords" to "Sala dei record",
    "FIN DE LA PARTIDA" to "FINE PARTITA",
    "Rango " to "Grado ",
    "🏆 ¡NUEVO RÉCORD PERSONAL!" to "🏆 NUOVO RECORD PERSONALE!",
    "Tu récord: " to "Il tuo record: ",
    "🚀 ¡Nivel %d!" to "🚀 Livello %d!",
    "JUGAR OTRA VEZ" to "GIOCA ANCORA",
    "🏆 Récords" to "🏆 Record",
    "Cambiar modo" to "Cambia modalità",
    "¡COMBO x%d!" to "COMBO x%d!",
    "¡Impacto!" to "Colpito!",
    "¡Fallaste! −20 🛡️" to "Mancato! −20 🛡️",

    // Reading tablets
    "📜 ¡Página del atlas recuperada!" to "📜 Pagina dell'atlante recuperata!",
    "EXPEDICIONES DE LÍA" to "SPEDIZIONI DI LÍA",
    "COLECCIONES DE VOCABULARIO" to "RACCOLTE DI VOCABOLARIO",
    "Toca una frase para escucharla" to "Tocca una frase per ascoltarla",
    "🔧 Repara la consola gramatical" to "🔧 Ripara la console grammaticale",
    "Escribe las formas que faltan" to "Scrivi le forme mancanti",
    "Comprobar" to "Verifica",
    "Ver respuesta" to "Vedi la risposta",
    "📡 Reconstruye la transmisión" to "📡 Ricostruisci la trasmissione",
    "Ver el orden correcto" to "Vedi l'ordine corretto",

    // App name, onboarding, home tour, widget hint
    "Spanish Blaster" to "Italian Blaster",
    "¡Hola! Soy Lía" to "Ciao! Sono Lía",
    "مرحبًا! أنا ليا. سنتعلم الإسبانية معًا.\nHi! I'm Lía. Let's learn Spanish together." to
        "مرحبًا! أنا ليا. سنتعلم الإيطالية معًا.\nHi! I'm Lía. Let's learn Italian together.",
    "Words: learn the 5000 most-used Spanish words." to "Words: learn the 5000 most-used Italian words.",
    "الكلمات: تعلّمي أهم 5000 كلمة إسبانية." to "الكلمات: تعلّمي أهم 5000 كلمة إيطالية.",
    "📱 Add the widget: long-press your home screen → Widgets → Spanish Blaster." to
        "📱 Add the widget: long-press your home screen → Widgets → Italian Blaster.",
    "📱 أضيفي الأداة إلى الشاشة الرئيسية: اضغطي مطولًا على الشاشة ← الأدوات ← Spanish Blaster." to
        "📱 أضيفي الأداة إلى الشاشة الرئيسية: اضغطي مطولًا على الشاشة ← الأدوات ← Italian Blaster.",
    "📡 La historia" to "📡 La storia",

    // Nilo, Lía's co-pilot
    "¡Hola! Soy Nilo, el piloto de la nave." to "Ciao! Sono Nilo, il pilota della nave.",
    "¡Vamos, Lía!" to "Andiamo, Lía!",
    "¡Mira, una señal!" to "Guarda, un segnale!",
    "¡Un diamante!" to "Un diamante!",
    "Camina hacia la luz azul." to "Cammina verso la luce blu.",
    "¡El portal está abierto!" to "Il portale è aperto!",
    "¡Lo logramos! ¡Una página más!" to "Ce l'abbiamo fatta! Un'altra pagina!",
    "¡Muy bien!" to "Molto bene!",
    "¡Genial!" to "Fantastico!",
    "¡Excelente, Lía!" to "Eccellente, Lía!",
    "¡Lo lograste!" to "Ce l'hai fatta!",
    "Lía, llega un mensaje. ¡Escucha!" to "Lía, arriva un messaggio. Ascolta!",
    "Responde para abrir el camino." to "Rispondi per aprire la strada.",
    "Necesito estas cosas. ¿Me ayudas a buscarlas?" to "Mi servono queste cose. Mi aiuti a cercarle?",
    "La consola está rota. ¡Repárala!" to "La console è rotta. Riparala!",
    "El mensaje está desordenado. ¡Ordénalo!" to "Il messaggio è in disordine. Riordinalo!",
    "¡Es hora de la misión!" to "È l'ora della missione!",
    "Última prueba antes del portal." to "Ultima prova prima del portale.",
    "¡Mira dentro del círculo dorado!" to "Guarda dentro il cerchio dorato!",

    // Grammar: why?
    "🕵️ ¿Por qué?" to "🕵️ Perché?",
    "La regla de hoy:" to "La regola di oggi:",
    "¡Caso resuelto! Eres una detective." to "Caso risolto! Sei una vera detective.",
    "¡Muy bien! Casi perfecto." to "Molto bene! Quasi perfetto.",
    "Repasamos la regla y lo intentamos otra vez." to "Ripassiamo la regola e riproviamo.",
    "¡Caso resuelto!" to "Caso risolto!",

    // Word Galaxy
    "CONSTELACIONES" to "COSTELLAZIONI",
    "¡Casi! Lo repetimos luego." to "Quasi! Lo ripetiamo dopo.",
    "¡Repasa primero, así no olvidas!" to "Prima ripassa, così non dimentichi!",
    "Cada día, cinco palabras nuevas." to "Ogni giorno, cinque parole nuove.",
    "How do you say this in Spanish?" to "How do you say this in Italian?",
    "كيف نقول هذا بالإسبانية؟" to "كيف نقول هذا بالإيطالية؟",
    "Aurora" to "Aurora", "Brújula" to "Bussola", "Cometa" to "Cometa", "Delfín" to "Delfino",
    "Estrella" to "Stella", "Faro" to "Faro", "Galaxia" to "Galassia", "Isla" to "Isola",
    "Jaguar" to "Giaguaro", "Lince" to "Lince", "Luna" to "Luna", "Marea" to "Marea", "Nube" to "Nuvola",
    "Órbita" to "Orbita", "Pegaso" to "Pegaso", "Río" to "Fiume", "Sol" to "Sole", "Trueno" to "Tuono",
    "Unicornio" to "Unicorno", "Volcán" to "Vulcano", "Zafiro" to "Zaffiro", "Ancla" to "Ancora",
    "Búho" to "Gufo", "Dragón" to "Drago", "Eclipse" to "Eclissi", "Fénix" to "Fenice", "Girasol" to "Girasole",
    // Word Jump and chapter goals
    "Siguiente ▶" to "Avanti ▶",
    "¡Gran salto!" to "Gran salto!",
    "¡Ya casi lo tenemos!" to "Ci siamo quasi!",
    // Top-down ship deck
    "Sala de radio" to "Sala radio",
    "Bodega" to "Stiva",
    "Sala de máquinas" to "Sala macchine",
    "Puente de mando" to "Ponte di comando",
    "Lluvia de palabras" to "Pioggia di parole",
    "SALIDA" to "USCITA",
    "¡A por la llave! 🔑" to "Alla chiave! 🔑",
    "¡Esta puerta está cerrada!" to "Questa porta è chiusa!",
    "¡Necesitamos la llave!" to "Ci serve la chiave!",
    "¡La puerta se abrió!" to "La porta si è aperta!",
    "¡Una tarjeta de acceso!" to "Una tessera d'accesso!",
    "¡Busca la tarjeta de acceso de esta sala!" to "Cerca la tessera d'accesso di questa stanza!",
    "¡Empuja las baterías a los cargadores!" to "Spingi le batterie sui caricatori!",
    "¡Bien! Las baterías están cargando." to "Brava! Le batterie si stanno caricando.",
    "¡Gracias! ¡Es justo lo que necesitaba!" to "Grazie! È proprio quello che mi serviva!",
    "Necesito" to "Mi serve",
    "Escucha la historia frase por frase. Toca una palabra para ver qué significa." to "Ascolta la storia frase per frase. Tocca una parola per vedere cosa significa.",
    "Escribe la palabra que falta. La pista te ayuda." to "Scrivi la parola che manca. Il suggerimento ti aiuta.",
    "Escucha cada palabra y búscala en la imagen." to "Ascolta ogni parola e cercala nell'immagine.",
    "Lee la regla y completa la tabla." to "Leggi la regola e completa la tabella.",
    "Ordena las palabras para formar la frase." to "Metti in ordine le parole per formare la frase.",
    "Camina con las flechas, salta a la respuesta correcta y pulsa Elegir." to "Cammina con le frecce, salta sulla risposta giusta e premi Scegli.",
    "Toca el meteorito con la respuesta correcta antes de que caiga." to "Tocca il meteorite con la risposta giusta prima che cada.",
    "No, eso no es" to "No, questo non è",
    "¡Cuidado con el robot de seguridad!" to "Attenta al robot di sicurezza!",
    "¡Mira, la llave!" to "Guarda, la chiave!",
    "¡Tenemos la llave! ¡A la salida!" to "Abbiamo la chiave! All'uscita!",
)

/** Same word with the wrong gender or number, used as tempting wrong answers: rossa → rosso, rosse. */
fun wordEndingVariants(word: String): List<String> {
    if (word.contains(' ')) return emptyList()
    val stem = word.dropLast(1)
    return when (word.lastOrNull()) {
        'a' -> listOf(stem + "o", stem + "e")
        'o' -> listOf(stem + "a", stem + "i")
        'e' -> listOf(stem + "i", stem + "a")
        'i' -> listOf(stem + "o", stem + "e")
        else -> emptyList()
    }
}

/** Common irregular forms and article contractions → the dictionary word ("sono" → "essere"), for tap-to-translate. */
private val FORMS: Map<String, String> = listOf(
    "essere" to "sono sei è siamo siete era erano stato stata fu sia",
    "avere" to "ho hai ha abbiamo avete hanno aveva avuto abbia",
    "andare" to "vado vai va andiamo andate vanno andato",
    "fare" to "faccio fai fa facciamo fate fanno fatto",
    "potere" to "posso puoi può possiamo potete possono",
    "volere" to "voglio vuoi vuole vogliamo volete vogliono",
    "dire" to "dico dici dice diciamo dite dicono detto",
    "vedere" to "vedo vedi vede vediamo vedono visto",
    "sapere" to "so sai sa sappiamo sanno",
    "venire" to "vengo vieni viene veniamo vengono venuto",
    "stare" to "sto stai sta stiamo stanno",
    "dare" to "do dai dà diamo danno",
    "aprire" to "aperto aperta aperti aperte",
    "perdere" to "perso persa",
    "il" to "lo la i gli le l",
    "uno" to "un una",
    "di" to "del dello della dei degli delle dell",
    "a" to "al allo alla ai agli alle all",
    "in" to "nel nello nella nei negli nelle nell",
    "da" to "dal dallo dalla dagli dalle dall",
    "su" to "sul sullo sulla sui sugli sulle sull",
    "questo" to "questa questi queste",
    "quello" to "quella quelli quelle quel",
    "mio" to "mia miei mie",
    "tuo" to "tua tuoi tue",
    "suo" to "sua suoi sue",
    "nostro" to "nostra nostri nostre",
).flatMap { (lemma, forms) -> forms.split(" ").map { it to lemma } }.toMap()

/**
 * Dictionary words a form found in a sentence may come from, most likely first: "è" → "essere",
 * "l'atlante" → "atlante", "chiavi" → "chiave", "rossa" → "rosso", "parliamo" → "parlare".
 */
fun lemmaCandidates(word: String): List<String> = buildList {
    val w = word.substringAfterLast('\'')
    FORMS[w]?.let { add(it) }
    add(w)
    if (w.endsWith("i")) { add(w.dropLast(1) + "o"); add(w.dropLast(1) + "e") }
    if (w.endsWith("e")) { add(w.dropLast(1) + "a"); add(w.dropLast(1) + "o") }
    if (w.endsWith("a")) add(w.dropLast(1) + "o")
    if (w.endsWith("che")) add(w.dropLast(3) + "ca")
    for (ending in VERB_ENDINGS) {
        if (w.endsWith(ending) && w.length > ending.length + 1) {
            val stem = w.dropLast(ending.length)
            add(stem + "are"); add(stem + "ere"); add(stem + "ire")
        }
    }
}

private val VERB_ENDINGS = listOf(
    "iamo", "ate", "ete", "ite", "ano", "ono", "ato", "ata", "ati", "uto", "uta", "ito", "ita",
    "ava", "avano", "eva", "evano", "o", "i", "a", "e"
)
