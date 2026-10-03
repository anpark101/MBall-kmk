package eu.kanade.tachiyomi.extension.all.mangaball

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

/**
 * Komikku instantiates this class and calls [createSources], which produces one source per
 * language the site offers. Each source filters chapters to its own language and can filter
 * the catalogue locally by language as well.
 */
class MangaBallFactory : SourceFactory {

    override fun createSources(): List<Source> =
        MangaBallLanguages.SOURCES.map<Pair<String, String>, Source> { (code, label) ->
            MangaBallSource(code, label)
        }
}

internal object MangaBallLanguages {

    /** Site language code to the label shown in the app. */
    val SOURCES: List<Pair<String, String>> = listOf(
        "en" to "English",
        "es" to "Español",
        "pt-BR" to "Português (Brasil)",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "nl" to "Nederlands",
        "ru" to "Русский",
        "uk" to "Українська",
        "pl" to "Polski",
        "cs" to "Čeština",
        "sk" to "Slovenčina",
        "ro" to "Română",
        "hu" to "Magyar",
        "bg" to "Български",
        "sr" to "Српски",
        "tr" to "Türkçe",
        "ar" to "العربية",
        "he" to "עברית",
        "fa" to "فارسی",
        "el" to "Ελληνικά",
        "id" to "Indonesia",
        "ms" to "Melayu",
        "vi" to "Tiếng Việt",
        "th" to "ไทย",
        "hi" to "हिन्दी",
        "bn" to "বাংলা",
        "ta" to "தமிழ்",
        "ml" to "മലയാളം",
        "ne" to "नेपाली",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文",
        "da" to "Dansk",
        "fi" to "Suomi",
        "no" to "Norsk",
        "sv" to "Svenska",
        "is" to "Íslenska",
        "sq" to "Shqip",
        "ca" to "Català",
    )
}
