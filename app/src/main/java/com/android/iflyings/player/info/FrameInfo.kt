package com.android.iflyings.player.info

import android.graphics.Rect
import android.opengl.GLES20
import com.android.iflyings.player.MediaWindow
import com.android.iflyings.player.utils.ShaderUtils
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream


class FrameInfo {

    private val mWindowLists = mutableListOf<MediaWindow>()

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private lateinit var mFrameRect: Rect

    private var isPlaying = false

    fun create(width: Int, height: Int) {
        mScreenWidth = width
        mScreenHeight = height

        openPlayer()
    }
    fun draw() {
        if (isPlaying) {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            var index = 0
            for (mediaWindow in mWindowLists) {
                GLES20.glViewport(mediaWindow.windowLeft, mScreenHeight - mediaWindow.windowHeight - mediaWindow.windowTop, mediaWindow.windowWidth, mediaWindow.windowHeight)
                index = mediaWindow.draw(index)
            }
        }
        ShaderUtils.checkGlError("FrameInfo->draw")
    }
    fun destroy() {
        ShaderUtils.checkGlError("FrameInfo->destroy")
    }

    private fun load(file: String, jsonObject: JSONObject) {
        mFrameRect = jsonObject.takeIf { it.has("rect") && !it.isNull("rect") }?.let {
            val array = it.getJSONArray("rect")
            Rect(array.getInt(0), array.getInt(1), array.getInt(2), array.getInt(3))
        } ?: Rect(0, 0, mScreenWidth, mScreenHeight)
        val list = jsonObject.getJSONArray("list")
        for (i in 0 until list.length()) {
            val mediaWindow = MediaWindow.from(file, list.getJSONObject(i))
            mWindowLists.add(mediaWindow)
        }
    }
    fun update(file: String) {
        val cfgFile = File(file, "package.json")
        if (cfgFile.exists()) {
            val fis = FileInputStream(cfgFile)
            val arrays = ByteArray(fis.available())
            fis.read(arrays)
            fis.close()
            load(file, JSONObject(String(arrays)))
        } else {
            mWindowLists.add(MediaWindow.from(file))
        }
    }

    fun start() {
        isPlaying = true
        openPlayer()
    }
    fun stop() {
        mScreenWidth = 0
        mScreenHeight = 0
        isPlaying = false
        for (mediaWindow in mWindowLists) {
            mediaWindow.stop()
        }
        mWindowLists.clear()
    }

    private fun openPlayer() {
        if (mScreenWidth > 0 && mScreenHeight > 0 && isPlaying) {
            for (mediaWindow in mWindowLists) {
                mediaWindow.start(mScreenWidth, mScreenHeight)
            }
        }
    }

}