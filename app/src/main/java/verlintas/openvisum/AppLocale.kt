package verlintas.openvisum

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

object AppLocale {

    const val SYSTEM = "system"
    const val ZH_CN = "zh-CN"
    const val ZH_TW = "zh-TW"
    const val EN = "en"

    fun apply(context: Context, tag: String) {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        val locales = if (tag == SYSTEM) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
        if (localeManager.applicationLocales != locales) {
            localeManager.setApplicationLocales(locales)
        }
    }
}
