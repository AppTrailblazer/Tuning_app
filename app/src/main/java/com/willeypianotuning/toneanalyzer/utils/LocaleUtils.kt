package com.willeypianotuning.toneanalyzer.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.willeypianotuning.toneanalyzer.BuildConfig
import com.willeypianotuning.toneanalyzer.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.util.Locale

object LocaleUtils {
    private var sLocale: Locale? = null

    fun setLocale(locale: Locale?) {
        if (!BuildConfig.DEBUG) {
            return
        }
        sLocale = locale
        if (locale != null) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
        }
    }

    fun getLocaleListFromXml(ctx: Context): LocaleListCompat {
        val tagsList = mutableListOf<CharSequence>()
        try {
            val xpp: XmlPullParser = ctx.resources.getXml(R.xml.locales_config)
            while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                if (xpp.eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "locale") {
                        tagsList.add(xpp.getAttributeValue(0))
                    }
                }
                xpp.next()
            }
        } catch (e: XmlPullParserException) {
            Timber.e(e, "Failed to parse locales config")
        } catch (e: IOException) {
            Timber.e(e, "Failed to parse locales config")
        }

        return LocaleListCompat.forLanguageTags(tagsList.joinToString(","))
    }
}