package com.android.iflyings.player.utils

import android.app.Activity
import android.content.Context


object PreferenceUtils {

    private const val PREFERENCES_OF_NAME = "setting"
    private const val MEDIA_NAME = "media_path"

    fun getMediaPath(context: Context): String? {
        val sp = context.getSharedPreferences(PREFERENCES_OF_NAME, Activity.MODE_PRIVATE)
        return sp.getString(MEDIA_NAME, null) ?: return null
    }

    fun setMediaPath(context: Context, path: String) {
        val sp = context.getSharedPreferences(PREFERENCES_OF_NAME, Activity.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(MEDIA_NAME, path)
        editor.apply()
    }
}