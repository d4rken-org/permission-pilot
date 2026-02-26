package eu.darken.myperm.screenshots

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

internal const val DS = "spec:width=1080px,height=2400px,dpi=428"

/**
 * Multi-preview annotation generating one preview per Play Store-supported locale (light mode).
 * Each [name] is the fastlane metadata directory name for direct use in the copy script.
 */
@Preview(locale = "af", name = "af", device = DS)
@Preview(locale = "am", name = "am", device = DS)
@Preview(locale = "ar", name = "ar", device = DS)
@Preview(locale = "az", name = "az-AZ", device = DS)
@Preview(locale = "be", name = "be", device = DS)
@Preview(locale = "bg", name = "bg", device = DS)
@Preview(locale = "bn-BD", name = "bn-BD", device = DS)
@Preview(locale = "ca", name = "ca", device = DS)
@Preview(locale = "cs", name = "cs-CZ", device = DS)
@Preview(locale = "da", name = "da-DK", device = DS)
@Preview(locale = "de", name = "de-DE", device = DS)
@Preview(locale = "el", name = "el-GR", device = DS)
@Preview(locale = "en", name = "en-US", device = DS)
@Preview(locale = "es", name = "es-ES", device = DS)
@Preview(locale = "fi", name = "fi-FI", device = DS)
@Preview(locale = "fr", name = "fr-FR", device = DS)
@Preview(locale = "hi-IN", name = "hi-IN", device = DS)
@Preview(locale = "hr", name = "hr", device = DS)
@Preview(locale = "hu", name = "hu-HU", device = DS)
@Preview(locale = "it", name = "it-IT", device = DS)
@Preview(locale = "iw", name = "iw-IL", device = DS)
@Preview(locale = "ja", name = "ja-JP", device = DS)
@Preview(locale = "ko", name = "ko-KR", device = DS)
@Preview(locale = "nl", name = "nl-NL", device = DS)
@Preview(locale = "nb", name = "no-NO", device = DS)
@Preview(locale = "pl", name = "pl-PL", device = DS)
@Preview(locale = "pt-BR", name = "pt-BR", device = DS)
@Preview(locale = "pt", name = "pt-PT", device = DS)
@Preview(locale = "ro", name = "ro", device = DS)
@Preview(locale = "ru", name = "ru-RU", device = DS)
@Preview(locale = "sk", name = "sk", device = DS)
@Preview(locale = "sr", name = "sr", device = DS)
@Preview(locale = "sv", name = "sv-SE", device = DS)
@Preview(locale = "th", name = "th", device = DS)
@Preview(locale = "tr", name = "tr-TR", device = DS)
@Preview(locale = "uk", name = "uk", device = DS)
@Preview(locale = "vi", name = "vi", device = DS)
@Preview(locale = "zh-CN", name = "zh-CN", device = DS)
@Preview(locale = "zh-TW", name = "zh-TW", device = DS)
annotation class PlayStoreLocales

/**
 * Same locales but with night mode enabled for dark theme screenshots.
 */
@Preview(locale = "af", name = "af", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "am", name = "am", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ar", name = "ar", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "az", name = "az-AZ", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "be", name = "be", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "bg", name = "bg", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "bn-BD", name = "bn-BD", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ca", name = "ca", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "cs", name = "cs-CZ", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "da", name = "da-DK", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "de", name = "de-DE", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "el", name = "el-GR", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "en", name = "en-US", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "es", name = "es-ES", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "fi", name = "fi-FI", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "fr", name = "fr-FR", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "hi-IN", name = "hi-IN", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "hr", name = "hr", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "hu", name = "hu-HU", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "it", name = "it-IT", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "iw", name = "iw-IL", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ja", name = "ja-JP", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ko", name = "ko-KR", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "nl", name = "nl-NL", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "nb", name = "no-NO", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "pl", name = "pl-PL", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "pt-BR", name = "pt-BR", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "pt", name = "pt-PT", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ro", name = "ro", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ru", name = "ru-RU", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "sk", name = "sk", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "sr", name = "sr", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "sv", name = "sv-SE", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "th", name = "th", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "tr", name = "tr-TR", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "uk", name = "uk", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "vi", name = "vi", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "zh-CN", name = "zh-CN", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "zh-TW", name = "zh-TW", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PlayStoreLocalesDark

/**
 * Smoke test subset for fast iteration (6 locales covering LTR, RTL, CJK).
 */
@Preview(locale = "en", name = "en-US", device = DS)
@Preview(locale = "de", name = "de-DE", device = DS)
@Preview(locale = "ja", name = "ja-JP", device = DS)
@Preview(locale = "ar", name = "ar", device = DS)
@Preview(locale = "zh-CN", name = "zh-CN", device = DS)
@Preview(locale = "pt-BR", name = "pt-BR", device = DS)
annotation class PlayStoreLocalesSmoke
