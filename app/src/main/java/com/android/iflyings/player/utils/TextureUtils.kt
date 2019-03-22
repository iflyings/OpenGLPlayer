package com.android.iflyings.player.utils

import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import android.opengl.GLUtils

object TextureUtils {

    const val NO_TEXTURE_ID = 0

    //生成一个视频纹理
    fun createVideoTexture(): Int {
        val textureIds = IntArray(1)
        glGenTextures(1, textureIds, 0)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0])
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST.toFloat())
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR.toFloat())
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE.toFloat())
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE.toFloat())

        return textureIds[0]
    }

    //生成一个图像纹理
    fun createBitmapTexture(bitmap: Bitmap?, width: Int, height: Int): Int {
        val textureIds = IntArray(1)
        glGenTextures(1, textureIds, 0)
        //第一个参数代表这是一个2D纹理，第二个参数就是OpenGL要绑定的纹理对象ID，也就是让OpenGL后面的纹理调用都使用此纹理对象
        glBindTexture(GL_TEXTURE_2D, textureIds[0])
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR.toFloat())
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST.toFloat())
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE.toFloat())
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE.toFloat())

        if (bitmap != null) {
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
        } else {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, null)
        }

        //我们为纹理生成MIP贴图，提高渲染性能，但是可占用较多的内存
        //glGenerateMipmap(GL_TEXTURE_2D);

        return textureIds[0]
    }

    fun createRenderBuffer(): Int {
        val renderBufferIds = IntArray(1)
        glGenRenderbuffers(1, renderBufferIds, 0)
        return renderBufferIds[0]
    }

    fun createFrameBuffer(): Int {
        val frameBufferIds = IntArray(1)
        glGenFramebuffers(1, frameBufferIds, 0)
        return frameBufferIds[0]
    }

    fun unloadTexture(id: Int) {
        if (id != 0) {
            glDeleteTextures(1, intArrayOf(id), 0)
        }
    }

    fun unloadRenderBuffer(id: Int) {
        if (id != 0) {
            glDeleteRenderbuffers(1, intArrayOf(id), 0)
        }
    }

    fun unloadFrameBuffer(id: Int) {
        if (id != 0) {
            glDeleteFramebuffers(1, intArrayOf(id), 0)
        }
    }

    fun bindTextureEXTERNALOES(textureId: Int, activeTextureId: Int, handle: Int, idx: Int) {
        glActiveTexture(activeTextureId)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        glUniform1i(handle, idx)
    }

    fun bindTexture2D(textureId: Int, activeTextureId: Int, handle: Int, idx: Int) {
        //激活纹理单元，GL_TEXTURE0代表纹理单元0，GL_TEXTURE1代表纹理单元1
        glActiveTexture(activeTextureId)
        //绑定纹理到这个纹理单元
        glBindTexture(GL_TEXTURE_2D, textureId)
        //把选定的纹理单元传给片段着色器中的u_TextureHandle
        glUniform1i(handle, idx)
    }
}
