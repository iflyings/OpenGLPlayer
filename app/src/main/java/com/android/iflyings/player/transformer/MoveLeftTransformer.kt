package com.android.iflyings.player.transformer

import com.android.iflyings.player.MediaWindow
import com.android.iflyings.player.info.MediaInfo

class MoveLeftTransformer: MediaWindow.MediaTransformer {

    override fun transformMedia(mediaInfo: MediaInfo, position: Float) {
        mediaInfo.reset()
        when {
            position < -1 -> { mediaInfo.hide() }
            position < 0 -> { mediaInfo.setAlpha(1 + position) }
            position == 0f -> { mediaInfo.normal() }
            position < 1 -> { mediaInfo.setRect(0f, 1 - position, 0f, 1f) }
            else -> { mediaInfo.hide() }
        }
    }
}