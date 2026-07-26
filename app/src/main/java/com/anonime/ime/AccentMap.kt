package com.anonime.ime

/**
 * Long-press accent map.
 *
 * For each base Latin letter that commonly has accented variants in
 * European languages (Spanish / French / German / Portuguese / Italian /
 * Nordic / Slavic Latin), we list the most useful accents in priority
 * order. The first item in the list is shown closest to the user's thumb
 * on the popup.
 *
 * ── What this is NOT ─────────────────────────────────────────────────────────
 *  - Not a dictionary. We never look up "what words start with this letter
 *    in the user's language" — that would require a dictionary and would
 *    leak typing patterns.
 *  - Not personalized. The list is static and identical for every user.
 *  - Not exhaustive. We picked the most common ~3-8 variants per letter;
 *    rarer diacritics (e.g. Esperanto ĉ, Latvian ķ) are omitted to keep
 *    the popup small. Add them later if requested.
 *
 * ── Anonymous-typing guarantee ──────────────────────────────────────────────
 * This is a pure static lookup. No state, no I/O, no network. Adding or
 * removing entries has zero impact on the privacy properties of the IME.
 */
object AccentMap {

    private val map: Map<Char, List<String>> = mapOf(
        'a' to listOf("á", "à", "ä", "â", "ã", "å", "æ", "ā", "ą"),
        'e' to listOf("é", "è", "ë", "ê", "ę", "ē", "ė"),
        'i' to listOf("í", "ì", "ï", "î", "ī", "į"),
        'o' to listOf("ó", "ò", "ö", "ô", "õ", "ø", "œ", "ō"),
        'u' to listOf("ú", "ù", "ü", "û", "ū"),
        'c' to listOf("ç", "ć", "č"),
        'n' to listOf("ñ", "ń"),
        's' to listOf("ś", "š", "ß"),
        'y' to listOf("ý", "ÿ"),
        'z' to listOf("ź", "ż", "ž"),
        'l' to listOf("ł"),
        'g' to listOf("ĝ", "ğ"),
        'd' to listOf("ð", "đ"),
        'r' to listOf("ř"),
        't' to listOf("þ", "ț"),
        'h' to listOf("ħ"),
    )

    /**
     * Returns the list of accent variants for [char], or null if the letter
     * has no accents defined (digits, punctuation, or letters not in the map).
     *
     * Lookup is case-insensitive: passing 'A' returns the same variants as
     * passing 'a'. The renderer uppercases the variants itself when shift
     * is on, so callers don't need to worry about case here.
     */
    fun accentsFor(char: Char): List<String>? {
        val key = char.lowercaseChar()
        return map[key]
    }

    /** True iff the given key label is a single Latin letter with accents. */
    fun hasAccents(label: String): Boolean {
        if (label.length != 1) return false
        val c = label[0]
        if (!c.isLetter()) return false
        return map.containsKey(c.lowercaseChar())
    }
}
