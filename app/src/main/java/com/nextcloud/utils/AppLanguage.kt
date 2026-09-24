/*
 * DataDrive - Android Client
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * App language switch between English and Vietnamese. When nothing was chosen the system language
 * decides: Vietnamese resources for a Vietnamese system, English otherwise.
 */
object AppLanguage {
    const val ENGLISH = "en"
    const val VIETNAMESE = "vi"

    fun current(context: Context): String {
        val chosen = AppCompatDelegate.getApplicationLocales()
        val language = if (chosen.isEmpty) {
            context.resources.configuration.locales[0].language
        } else {
            chosen[0]?.language
        }
        return if (language == VIETNAMESE) VIETNAMESE else ENGLISH
    }

    fun set(context: Context, language: String) {
        if (current(context) != language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
    }
}
