package com.android.iflyings.player.transformer

import com.android.iflyings.player.MediaWindow
import com.android.iflyings.player.info.MediaInfo
import com.android.iflyings.player.info.TextInfo

class NormalTransformer: MediaWindow.MediaTransformer {

        override fun transformMedia(mediaInfo: MediaInfo, position: Float) {
            if (mediaInfo is TextInfo) {
                mediaInfo.reset()
                mediaInfo.setThreshold(mediaInfo.textColor)
            }
        }
    }