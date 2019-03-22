package com.android.iflyings.player.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.support.v4.provider.DocumentFile
import android.util.AttributeSet
import com.android.iflyings.player.info.FrameInfo
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLPlayerView : GLSurfaceView {
    private lateinit var mFrameInfo: FrameInfo

    constructor(context: Context) : super(context) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun init() {
        // 创建一个OpenGL ES 2.0 context
        setEGLContextClientVersion(2)
        setRenderer(mRenderer)
        // 只有在绘制数据改变时才绘制view
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        //为了可以激活log和错误检查，帮助调试3D应用，需要调用setDebugFlags()。
        debugFlags = GLSurfaceView.DEBUG_CHECK_GL_ERROR or GLSurfaceView.DEBUG_LOG_GL_CALLS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.setFormat(PixelFormat.RGBA_8888)
        } else {
            holder.setFormat(PixelFormat.RGB_565)
        }
    }

    fun start() {
        mFrameInfo.start()
    }
    fun stop() {
        mFrameInfo.stop()
        queueEvent {
            mFrameInfo.destroy()
        }
    }
    fun setDataSource(file: String) {
        mFrameInfo.update(file)
    }

    private var mRenderer = object: Renderer {
        override fun onSurfaceCreated(gl10: GL10, eglConfig: EGLConfig) {
            GLES20.glEnable(GLES20.GL_BLEND)
            //GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            //ShaderUtils.checkGlError("onSurfaceCreated")
        }
        override fun onSurfaceChanged(gl10: GL10, w: Int, h: Int) {
            GLES20.glViewport(0, 0, w, h)
            mFrameInfo.create(w, h)
            //ShaderUtils.checkGlError("onSurfaceChanged")
        }
        override fun onDrawFrame(gl10: GL10) {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            mFrameInfo.draw()
            //ShaderUtils.checkGlError("onDrawFrame")
        }
    }
}
