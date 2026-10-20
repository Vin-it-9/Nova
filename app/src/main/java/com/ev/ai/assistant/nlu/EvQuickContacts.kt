package com.ev.ai.assistant.nlu

/**
 * Hardcoded "speed dial" relationships that are resolved *before* the address book.
 *
 * Why this exists: the device contact search is fuzzy and happily matches a name
 * that merely *contains* the spoken word — so "call dad" could ring "Omkar Dada
 * PICT pune", and "shruti" could ring "shruti shinde pict". For the handful of
 * people the user calls every day, an explicit, unambiguous mapping is both faster
 * and far safer than guessing. These aliases win over the fuzzy search.
 *
 * Pure Kotlin (no Android imports) so it is trivially unit-testable and can be
 * reused by the call / SMS / WhatsApp executors alike.
 */
object EvQuickContacts {

    data class QuickContact(val name: String, val number: String)

    // Indian mobile numbers are 10 digits and always start with 9/8/7; prefixing
    // +91 makes the `tel:` URI dial correctly regardless of the SIM's home network.
    private val mom = QuickContact("Mom", "+919623458823")
    private val dad = QuickContact("Dad", "+919623458894")
    private val shruti = QuickContact("Shruti Didi", "+919623440700")
    private val pushkar = QuickContact("Pushkar", "+918956176605")
    private val rushikesh = QuickContact("Rushikesh Phalke", "+917775910607")

    /** alias (lowercase) -> contact. Keys are single words or short phrases. */
    private val MAP: Map<String, QuickContact> = buildMap {
        // ── Mom ── (Marathi "aai" आई + common Indian/English variants)
        listOf("mom", "mum", "mummy", "mumma", "mamma", "maa", "ma", "aai", "aaee", "ammi", "amma")
            .forEach { put(it, mom) }

        // ── Dad ── (Marathi "baba" बाबा + common variants)
        listOf("dad", "daddy", "papa", "pappa", "baba", "appa", "abba", "pitaji", "babaji")
            .forEach { put(it, dad) }

        // ── Shruti Didi (elder sister) ── (Marathi "tai" ताई, "bahin" बहिण)
        listOf("sister", "didi", "shruti", "tai", "bahin", "bahen", "vahini")
            .forEach { put(it, shruti) }

        // ── Pushkar (brother / friend) ──
        listOf("pushkar", "bro", "brother", "bhai", "bhaiya", "bhau", "dost", "friend", "yaar", "yar", "virat")
            .forEach { put(it, pushkar) }

        // ── Rushikesh Phalke (jiju / daji) ── "rushiii"/"rushhii" are how he is
        // saved on the phone, but the user has two "rushhii" and only ever means
        // Rushikesh Phalke, so every rushi-variant resolves here.
        listOf("rushikesh", "rushi", "rushiii", "rushhii", "daji", "jiju", "phalke")
            .forEach { put(it, rushikesh) }
    }

    /** Words that carry no name meaning and are stripped before lookup. */
    private val FILLER = setOf(
        "my", "the", "a", "an", "please", "to", "call", "phone", "ring", "dial",
        "contact", "dear", "sir", "madam", "ji", "sahab", "gar", "wala", "ke", "ko"
    )

    /** Every alias, exposed so callers (e.g. biasing) can reuse the vocabulary. */
    val aliases: Set<String> get() = MAP.keys

    /**
     * Resolve a spoken name to a quick contact.
     *
     * Matching is deliberately conservative so it never hijacks a genuinely
     * different contact name:
     *  1. exact match of the whole (filler-stripped) phrase, or
     *  2. the first meaningful word is an alias AND every remaining word is also an
     *     alias (e.g. "shruti didi" -> Shruti, "dad ji" -> Dad).
     *
     * A phrase whose first word is an alias but that is followed by a *different*
     * name ("friend ananya") returns null so the real address book is consulted.
     */
    fun resolve(query: String?): QuickContact? {
        if (query.isNullOrBlank()) return null
        val cleaned = query.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return null

        val words = cleaned.split(' ').filter { it.isNotEmpty() && it !in FILLER }
        if (words.isEmpty()) return null

        // 1. Whole phrase is an alias.
        MAP[words.joinToString(" ")]?.let { return it }

        // 2. First word is an alias and the rest are also aliases (or nothing).
        val first = MAP[words.first()] ?: return null
        val rest = words.drop(1)
        return if (rest.all { it in MAP }) first else null
    }
}
