package com.android.iflyings.player.transformer

import com.android.iflyings.player.MediaWindow
import com.android.iflyings.player.info.MediaInfo

class ColourElapseTransformer: MediaWindow.MediaTransformer {

    override fun transformMedia(mediaInfo: MediaInfo, position: Float) {
        mediaInfo.reset()
        when {
            position <= -1 -> { mediaInfo.hide() }
            position < 0 -> { /*mediaInfo.setAlpha(1 + position)*/ }
            position == 0f -> { mediaInfo.normal() }
            position < 1 -> { mediaInfo.setBright(position) }
            else -> { mediaInfo.hide() }
        }
    }
}