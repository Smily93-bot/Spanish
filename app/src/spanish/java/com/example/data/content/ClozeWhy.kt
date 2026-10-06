package com.example.data.content

import com.example.data.model.HelperLanguage
import com.example.data.model.VocabWord
import com.example.data.model.pick

/**
 * A short grammar reason for a fill-the-gap answer. Only rules that always hold are used
 * (preposition + infinitive, article → gender…); otherwise it just names the word class.
 */
object ClozeWhy {
    private val PREPOSITIONS = setOf("de", "a", "para", "sin", "por", "al", "en", "con", "hasta", "tras")
    private val MODALS = setOf(
        "puedo", "puedes", "puede", "podemos", "podéis", "pueden", "podía", "podría",
        "quiero", "quieres", "quiere", "queremos", "queréis", "quieren", "quería",
        "debo", "debes", "debe", "debemos", "debéis", "deben", "debería",
        "necesito", "necesitas", "necesita", "necesitamos", "necesitan",
        "suelo", "sueles", "suele", "solemos", "suelen", "sé", "sabes", "sabe", "sabemos", "saben",
        "intento", "intenta", "prefiero", "prefiere", "espero", "decidí", "decidió"
    )
    private val MASC = setOf("el", "un", "este", "ese", "aquel", "del", "al")
    private val FEM = setOf("la", "una", "esta", "esa", "aquella")

    fun explain(word: VocabWord, sentenceBeforeGap: String, answer: String, language: HelperLanguage): String {
        val previous = sentenceBeforeGap.trim().split(Regex("[\\s¿¡\"«(]+")).lastOrNull().orEmpty().lowercase()
        val a = answer.lowercase()
        val isInfinitive = word.partOfSpeech == "verb" && Regex("(ar|er|ir|ír)$").containsMatchIn(a) && a == word.shortSpanish.lowercase()
        return when {
            isInfinitive && previous in PREPOSITIONS -> language.pick(
                "بعد حرف الجر $previous نستخدم الفعل في صيغة المصدر: $answer.",
                "After the preposition «$previous», Spanish uses the infinitive: $answer."
            )
            isInfinitive && (previous in MODALS || previous == "que") -> language.pick(
                "بعد $previous يأتي الفعل في صيغة المصدر: $answer.",
                "After «$previous», the next verb stays in the infinitive: $answer."
            )
            isInfinitive -> language.pick(
                "$answer فعل في صيغة المصدر (ينتهي بـ ${a.takeLast(2)}).",
                "$answer is a verb in the infinitive (the basic form ending in -${a.takeLast(2)})."
            )
            word.partOfSpeech == "noun" && previous in MASC && !a.endsWith("s") -> language.pick(
                "كلمة $previous تدل على أن $answer اسم مذكر.",
                "«$previous» shows that $answer is a masculine noun."
            )
            word.partOfSpeech == "noun" && previous in FEM && !a.endsWith("s") -> language.pick(
                "كلمة $previous تدل على أن $answer اسم مؤنث.",
                "«$previous» shows that $answer is a feminine noun."
            )
            word.partOfSpeech == "noun" -> language.pick("$answer اسم: شيء أو شخص أو فكرة.", "$answer is a noun: a thing, person or idea.")
            word.partOfSpeech == "adjective" -> language.pick(
                "$answer صفة: تصف الاسم وتتوافق معه.",
                "$answer is an adjective: it describes a noun and agrees with it."
            )
            word.partOfSpeech == "adverb" -> language.pick(
                "$answer ظرف: يضيف معنى إلى الفعل أو الصفة أو الجملة كلها، ولا يتغيّر.",
                "$answer is an adverb: it adds meaning to a verb, an adjective or the whole sentence, and never changes form."
            )
            word.partOfSpeech == "verb" -> language.pick("$answer فعل في هذه الجملة.", "$answer is the verb in this sentence.")
            else -> ""
        }
    }
}
