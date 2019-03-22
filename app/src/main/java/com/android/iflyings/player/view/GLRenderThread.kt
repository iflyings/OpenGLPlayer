package com.android.iflyings.player.view

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.android.iflyings.player.shader.ImageShader
import com.android.iflyings.player.shader.VideoShader
import com.android.iflyings.player.utils.ShaderUtils
import javax.microedition.khronos.egl.*

class GLRenderThread(surface: SurfaceTexture) : Thread("RenderThread") {
    private val MSG_UPDATE = 1

    private val mSurfaceTexture = surface
    private lateinit var mEgl: EGL10
    private var mEglSurface: EGLSurface = EGL10.EGL_NO_SURFACE
    private var mEglDisplay: EGLDisplay = EGL10.EGL_NO_DISPLAY
    private var mEglContext: EGLContext = EGL10.EGL_NO_CONTEXT
    private var mEglConfig: Array<EGLConfig?> = arrayOfNulls(1)
    private var mRenderer: Renderer? = null
    private var mHandler: Handler? = null

    private fun initGLES() {
        //获取系统的EGL对象
        mEgl = EGLContext.getEGL() as EGL10

        //获取显示设备
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed! " + mEgl.eglGetError())
        }

        //version中存放当前的EGL版本号，版本号即为version[0].version[1]，如1.0
        val versions = intArrayOf(2, 0)
        //初始化EGL
        if (!mEgl.eglInitialize(mEglDisplay, versions)) {
            throw RuntimeException("eglInitialize failed! " + mEgl.eglGetError())
        }

        //构造需要的配置列表
        val configSpec = intArrayOf(
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                //颜色缓冲区R、G、B、A分量的位数
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE)

        val configsCount = intArrayOf(1)
        //EGL根据attributes选择最匹配的配置
        if (!mEgl.eglChooseConfig(mEglDisplay, configSpec, mEglConfig, 1, configsCount)) {
            throw RuntimeException("eglChooseConfig failed! " + mEgl.eglGetError())
        }

        //如本文开始所讲的，获取TextureView内置的SurfaceTexture作为EGL的绘图表面，也就是跟系统屏幕打交道
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, mEglConfig[0], mSurfaceTexture, null)

        //指定哪个版本的OpenGL ES上下文，本文为OpenGL ES 2.0
        val contextSpec = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE)
        //创建上下文，EGL10.EGL_NO_CONTEXT表示不和别的上下文共享资源
        mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig[0], EGL10.EGL_NO_CONTEXT, contextSpec)
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY || mEglContext == EGL10.EGL_NO_CONTEXT){
            throw RuntimeException("eglCreateContext fail failed! " + mEgl.eglGetError())
        }

        //指定mEGLContext为当前系统的EGL上下文，你可能发现了使用两个mEglSurface，第一个表示绘图表面，第二个表示读取表面
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw RuntimeException("eglMakeCurrent failed! " + mEgl.eglGetError())
        }

        ShaderUtils.checkGlError("initGL")
    }

    private fun destroyGLES() {
        mEgl.eglDestroyContext(mEglDisplay, mEglContext)
        mEgl.eglDestroySurface(mEglDisplay, mEglSurface)
        mEglContext = EGL10.EGL_NO_CONTEXT
        mEglSurface = EGL10.EGL_NO_SURFACE
    }

    private fun drawFrame() {
        mRenderer?.onDrawFrame()
        mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)
    }

    fun startThread() {
        start()
    }

    fun stopThread() {
        mHandler?.post {
            mRenderer?.onSurfaceDestroyed()
            //ImageShader.getInstance().destroy()
            //VideoShader.getInstance().destroy()
            destroyGLES()
            mHandler?.removeMessages(MSG_UPDATE)
            Looper.myLooper()!!.quitSafely()
            mHandler?.removeCallbacksAndMessages(null)
            mHandler = null
        }
    }

    fun setRenderer(renderer: Renderer) {
        mRenderer = renderer
    }

    fun post(runnable: Runnable) {
        mHandler?.post(runnable)
    }

    override fun run() {

        Looper.prepare()

        mHandler = @SuppressLint("HandlerLeak")
        object: Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_UPDATE) {
                    drawFrame()
                    sendEmptyMessageDelayed(MSG_UPDATE, 16L)
                }
                super.handleMessage(msg)
            }
        }

        initGLES()

        //ImageShader.getInstance().create()
        //VideoShader.getInstance().create()

        mRenderer?.onSurfaceCreated()

        mHandler?.sendEmptyMessage(MSG_UPDATE)

        Looper.loop()

    }

    interface Renderer {

        fun onSurfaceCreated()

        fun onDrawFrame()

        fun onSurfaceDestroyed()

    }

}