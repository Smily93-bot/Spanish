package com.example.data.content

import com.example.data.model.HelperLanguage
import com.example.data.model.VocabWord
import com.example.data.model.pick

/**
 * A short grammar reason for a fill-the-gap answer (Italian build). Only rules that always hold are used
 * (preposition + infinitive, article → gender…); otherwise it just names the word class.
 */
object ClozeWhy {
    private val PREPOSITIONS = setOf("di", "a", "da", "per", "senza", "prima", "dopo")
    private val MODALS = setOf(
        "posso", "puoi", "può", "possiamo", "potete", "possono", "potevo", "potrei",
        "voglio", "vuoi", "vuole", "vogliamo", "volete", "vogliono", "volevo", "vorrei",
        "devo", "devi", "deve", "dobbiamo", "dovete", "devono", "dovrei",
        "so", "sai", "sa", "sappiamo", "sanno", "preferisco", "preferisce", "bisogna"
    )
    private val MASC = setOf("il", "lo", "un", "uno", "questo", "quel", "quello", "del", "al", "nel", "sul", "dal")
    private val FEM = setOf("la", "una", "questa", "quella", "della", "alla", "nella", "sulla", "dalla")

    fun explain(word: VocabWord, sentenceBeforeGap: String, answer: String, language: HelperLanguage): String {
        val previous = sentenceBeforeGap.trim().split(Regex("[\\s\"«(']+")).lastOrNull().orEmpty().lowercase()
        val a = answer.lowercase()
        val isInfinitive = word.partOfSpeech.startsWith("verb") && Regex("(are|ere|ire|rre)$").containsMatchIn(a) &&
            a == word.shortSpanish.lowercase()
        val isNoun = word.partOfSpeech.startsWith("noun")
        return when {
            isInfinitive && previous in PREPOSITIONS -> language.pick(
                "بعد حرف الجر $previous نستخدم الفعل في صيغة المصدر: $answer.",
                "After the preposition «$previous», Italian uses the infinitive: $answer."
            )
            isInfinitive && previous in MODALS -> language.pick(
                "بعد $previous يأتي الفعل في صيغة المصدر: $answer.",
                "After «$previous», the next verb stays in the infinitive: $answer."
            )
            isInfinitive -> language.pick(
                "$answer فعل في صيغة المصدر (ينتهي بـ ${a.takeLast(3)}).",
                "$answer is a verb in the infinitive (the basic form ending in -${a.takeLast(3)})."
            )
            isNoun && previous in MASC -> language.pick(
                "كلمة $previous تدل على أن $answer اسم مذكر مفرد.",
                "«$previous» shows that $answer is a masculine singular noun."
            )
            isNoun && previous in FEM -> language.pick(
                "كلمة $previous تدل على أن $answer اسم مؤنث مفرد.",
                "«$previous» shows that $answer is a feminine singular noun."
            )
            isNoun -> language.pick("$answer اسم: شيء أو شخص أو فكرة.", "$answer is a noun: a thing, person or idea.")
            word.partOfSpeech.startsWith("adjective") -> language.pick(
                "$answer صفة: تصف الاسم وتتوافق معه.",
                "$answer is an adjective: it describes a noun and agrees with it."
            )
            word.partOfSpeech.startsWith("adverb") -> language.pick(
                "$answer ظرف: يضيف معنى إلى الفعل أو الصفة أو الجملة كلها، ولا يتغيّر.",
                "$answer is an adverb: it adds meaning to a verb, an adjective or the whole sentence, and never changes form."
            )
            word.partOfSpeech.startsWith("verb") -> language.pick("$answer فعل في هذه الجملة.", "$answer is the verb in this sentence.")
            else -> ""
        }
    }
}
