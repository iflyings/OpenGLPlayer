package com.android.iflyings.player.shader

import android.opengl.GLES20
import com.android.iflyings.player.model.MediaModel
import com.android.iflyings.player.utils.ShaderUtils
import com.android.iflyings.player.utils.TextureUtils
import java.util.*


abstract class MediaShader(vertexShader: String, fragmentShader: String) {

    private val mVertexShader = vertexShader
    private val mFragmentShader = fragmentShader

    private var glProgramId = TextureUtils.NO_TEXTURE_ID
    private var glAttributePositionId = TextureUtils.NO_TEXTURE_ID
    private var glAttributeTextureCoordinateId = TextureUtils.NO_TEXTURE_ID
    private var glUniformPositionId = TextureUtils.NO_TEXTURE_ID
    private var glUniformTextureCoordinateId = TextureUtils.NO_TEXTURE_ID
    private var glUniformTextureSamplerId = TextureUtils.NO_TEXTURE_ID

    private var glUniformTransTypeId = TextureUtils.NO_TEXTURE_ID
    private var glUniformTransDataId = TextureUtils.NO_TEXTURE_ID
    private var glUniformDrawTypeId = TextureUtils.NO_TEXTURE_ID
    private var glUniformDrawDataId = TextureUtils.NO_TEXTURE_ID

    private val runOnDraw: LinkedList<Runnable> = LinkedList()
    private fun runOnDraw(runnable: Runnable) {
        synchronized(runOnDraw) {
            runOnDraw.addLast(runnable)
        }
    }
    private fun runPendingOnDrawTasks() {
        while (!runOnDraw.isEmpty()) {
            runOnDraw.removeFirst().run()
        }
    }

    private fun ifNeedInit() {
        if (TextureUtils.NO_TEXTURE_ID == glProgramId) {
            glProgramId = ShaderUtils.createProgram(mVertexShader, mFragmentShader)
            glAttributePositionId = GLES20.glGetAttribLocation(glProgramId, "aPosition")
            glAttributeTextureCoordinateId = GLES20.glGetAttribLocation(glProgramId, "aTextureCoordinate")
            glUniformPositionId = GLES20.glGetUniformLocation(glProgramId, "uPositionMatrix")
            glUniformTextureCoordinateId = GLES20.glGetUniformLocation(glProgramId, "uTextureMatrix")
            glUniformTextureSamplerId = GLES20.glGetUniformLocation(glProgramId, "sTexture")

            glUniformTransTypeId = GLES20.glGetUniformLocation(glProgramId, "uTransType")
            glUniformTransDataId = GLES20.glGetUniformLocation(glProgramId, "uTransData")
            glUniformDrawTypeId = GLES20.glGetUniformLocation(glProgramId, "uDrawType")
            glUniformDrawDataId = GLES20.glGetUniformLocation(glProgramId, "uDrawData")
        }
    }

    fun create() {
        ifNeedInit()
    }

    fun destroy() {
        if (TextureUtils.NO_TEXTURE_ID != glProgramId) {
            ShaderUtils.destroyProgram(glProgramId)
            glProgramId = TextureUtils.NO_TEXTURE_ID
        }
    }

    fun draw(textureId: Int, textureIndex: Int, mediaModel: MediaModel) {
        ifNeedInit()
        // 使用shader程序
        GLES20.glUseProgram(glProgramId)
        runPendingOnDrawTasks()

        GLES20.glEnableVertexAttribArray(glAttributePositionId)
        GLES20.glEnableVertexAttribArray(glAttributeTextureCoordinateId)

        GLES20.glVertexAttribPointer(glAttributePositionId, 3, GLES20.GL_FLOAT, false, 12, mediaModel.vertexBuffer)
        GLES20.glVertexAttribPointer(glAttributeTextureCoordinateId, 2, GLES20.GL_FLOAT, false, 8, mediaModel.textureBuffer)

        if (TextureUtils.NO_TEXTURE_ID != textureId) {
            onDraw(textureId, GLES20.GL_TEXTURE0 + textureIndex, glUniformTextureSamplerId, textureIndex)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES20.glDisableVertexAttribArray(glAttributePositionId)
        GLES20.glDisableVertexAttribArray(glAttributeTextureCoordinateId)
    }

    fun setTransType(shaderType: Int) {
        runOnDraw(Runnable {
            GLES20.glUniform1i(glUniformTransTypeId, shaderType)
        })
    }

    fun setTransData(f1: Float, f2: Float, f3: Float, f4: Float) {
        runOnDraw(Runnable {
            GLES20.glUniform4fv(glUniformTransDataId, 1, floatArrayOf(f1, f2, f3, f4), 0)
        })
    }

    fun setDrawType(shaderType: Int) {
        runOnDraw(Runnable {
            GLES20.glUniform1i(glUniformDrawTypeId, shaderType)
        })
    }

    fun setDrawData(f1: Float, f2: Float, f3: Float, f4: Float) {
        runOnDraw(Runnable {
            GLES20.glUniform4fv(glUniformDrawDataId, 1, floatArrayOf(f1, f2, f3, f4), 0)
        })
    }

    fun setPositionMatrix(matrix: FloatArray) {
        runOnDraw(Runnable {
            GLES20.glUniformMatrix4fv(glUniformPositionId, 1, false, matrix, 0)
        })
    }

    fun setTextureMatrix(matrix: FloatArray) {
        runOnDraw(Runnable {
            GLES20.glUniformMatrix4fv(glUniformTextureCoordinateId, 1, false, matrix, 0)
        })
    }

    protected abstract fun onDraw(textureId: Int, activeTextureId: Int, handle: Int, idx: Int)

}