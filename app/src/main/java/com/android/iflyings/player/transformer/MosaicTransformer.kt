package com.android.iflyings.player.transformer

import com.android.iflyings.player.MediaWindow
import com.android.iflyings.player.info.MediaInfo
import com.orhanobut.logger.Logger

class MosaicTransformer: MediaWindow.MediaTransformer {

    override fun transformMedia(mediaInfo: MediaInfo, position: Float) {
        mediaInfo.reset()
        when {
            position <= -0.5 -> { mediaInfo.hide() }
            position < 0 -> { mediaInfo.setMosaic(-position) }
            position == 0f -> { mediaInfo.normal() }
            position <= 0.5 -> { mediaInfo.setMosaic(position) }
            else -> { mediaInfo.hide() }
        }
    }
}