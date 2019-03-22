package com.android.iflyings.player.shader

import com.android.iflyings.player.MyApplication
import com.android.iflyings.player.R
import com.android.iflyings.player.utils.ShaderUtils
import com.android.iflyings.player.utils.TextureUtils

class ImageShader private constructor() : MediaShader(VERTEX_SHADER, FRAGMENT_SHADER) {

    override fun onDraw(textureId: Int, activeTextureId: Int, handle: Int, idx: Int) {
        TextureUtils.bindTexture2D(textureId, activeTextureId, handle, idx)
    }

    companion object {

        private val VERTEX_SHADER by lazy {
            ShaderUtils.readRawTextFile(MyApplication.getApplication(), R.raw.vertex_shader) }
        private val FRAGMENT_SHADER by lazy {
            ShaderUtils.readRawTextFile(MyApplication.getApplication(), R.raw.image_fragment_shader) +
                    ShaderUtils.readRawTextFile(MyApplication.getApplication(), R.raw.function_fragment_shader)
        }

        private val mInstance: ImageShader by lazy { ImageShader() }
        fun getInstance(): ImageShader {
            return ImageShader()
        }

    }
}
