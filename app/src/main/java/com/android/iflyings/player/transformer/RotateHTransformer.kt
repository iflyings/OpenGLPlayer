package com.android.iflyings.player.transformer

import com.android.iflyings.player.MediaWindow
import com.android.iflyings.player.info.MediaInfo

class RotateHTransformer: MediaWindow.MediaTransformer {

    override fun transformMedia(mediaInfo: MediaInfo, position: Float) {
        mediaInfo.reset()
        when {
            position <= -0.5 -> { mediaInfo.hide() }
            position < 0 -> { mediaInfo.setRotate(180 * position, 0.0f, 1.0f, 0.0f) }
            position == 0f -> { mediaInfo.normal() }
            position < 0.5 -> { mediaInfo.setRotate(-180 * position, 0.0f, 1.0f, 0.0f) }
            else -> { mediaInfo.hide() }
        }
    }
}