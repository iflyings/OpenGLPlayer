package com.android.iflyings.player.utils

import android.content.Context
import android.opengl.GLES20.*
import com.orhanobut.logger.Logger

object ShaderUtils {

    fun checkGlError(label: String? = null) {
        val error = glGetError()
        if (error != GL_NO_ERROR) {
            Logger.e("[GlError] error = $error, msg = $label")
            throw RuntimeException()
        }
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            throw IllegalArgumentException("vertexSource = $vertexSource")
        }
        val pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            throw IllegalArgumentException("fragmentSource = $fragmentSource")
        }
        val program = glCreateProgram()
        if (program == 0) {
            throw IllegalArgumentException("Create Program Error")
        }

        glAttachShader(program, vertexShader)
        glAttachShader(program, pixelShader)
        glLinkProgram(program)
        checkGlError("Create Program")
        glDeleteShader(vertexShader)
        glDeleteShader(pixelShader)

        val linkStatus = IntArray(1)
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GL_TRUE) {
            Logger.e(glGetProgramInfoLog(program))
            glDeleteProgram(program)
            throw IllegalArgumentException("Link Program Failed")
        }

        return program
    }

    fun destroyProgram(program: Int) {
        if (program != 0) {
            glDeleteProgram(program)
        }
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = glCreateShader(shaderType)
        if (shader != 0) {
            glShaderSource(shader, source)
            glCompileShader(shader)
            val compiled = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Logger.e(glGetShaderInfoLog(shader))
                glDeleteShader(shader)
                shader = 0
            }
        }
        return shader
    }

    fun readRawTextFile(context: Context, resId: Int): String {
        val inputStream = context.resources.openRawResource(resId)
        val byteArray = ByteArray(inputStream.available())
        inputStream.read(byteArray)
        inputStream.close()
        return String(byteArray)
    }
}
