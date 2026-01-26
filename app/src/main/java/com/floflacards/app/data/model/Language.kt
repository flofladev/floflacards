package com.floflacards.app.data.model

/**
 * Represents the available languages in the app.
 * Used for locale management and language selection.
 * 
 * @property code Language code (e.g., "en", "de", "pl")
 * @property displayName Human-readable language name in its own language
 * @property flagEmoji Flag emoji representation
 */
enum class Language(
    val code: String, 
    val displayName: String,
    val flagEmoji: String
) {
    SYSTEM("system", "System Default", "🌐"),
    ENGLISH("en", "English", "🇺🇸"),
    GERMAN("de", "Deutsch", "🇩🇪"),
    POLISH("pl", "Polski", "🇵🇱");
    
    companion object {
        /**
         * Gets the language enum from a code.
         * Returns SYSTEM if code not found.
         */
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: SYSTEM
        }
        
        /**
         * Converts language code to IETF language tag for AppCompatDelegate.
         * Returns empty string for SYSTEM to use system default locale.
         */
        fun toLanguageTag(code: String): String {
            return if (code == SYSTEM.code) "" else code
        }
    }
}
