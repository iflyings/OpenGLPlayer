package com.android.iflyings.player.shader

import com.android.iflyings.player.MyApplication
import com.android.iflyings.player.R
import com.android.iflyings.player.utils.ShaderUtils
import com.android.iflyings.player.utils.TextureUtils


class VideoShader private constructor() : MediaShader(VERTEX_SHADER, FRAGMENT_SHADER) {

    override fun onDraw(textureId: Int, activeTextureId: Int, handle: Int, idx: Int) {
        TextureUtils.bindTextureEXTERNALOES(textureId, activeTextureId, handle, idx)
    }

    companion object {

        private val VERTEX_SHADER by lazy {
            ShaderUtils.readRawTextFile(MyApplication.getApplication(), R.raw.vertex_shader) }
        private val FRAGMENT_SHADER by lazy {
            ShaderUtils.readRawTextFile(MyApplication.getApplication(), R.raw.video_fragment_shader) +
                    ShaderUtils.readRawTextFile(MyApplication.getApplication(), R.raw.function_fragment_shader)
        }

        private val mInstance: VideoShader by lazy { VideoShader() }
        fun getInstance(): VideoShader {
            return VideoShader()
        }
    }
}
