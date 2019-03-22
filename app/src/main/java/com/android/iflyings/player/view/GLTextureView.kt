package com.android.iflyings.player.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20.*
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import com.android.iflyings.player.info.FrameInfo

// https://blog.csdn.net/lb377463323/article/details/77096652
class GLTextureView: TextureView{

    private val mFrameInfo = FrameInfo()
    private var mGLRenderThread: GLRenderThread? = null

    constructor(context: Context):
            super(context) {
        initView()
    }
    constructor(context: Context, attrs: AttributeSet):
            super(context, attrs) {
        initView()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int):
            super(context, attrs, defStyleAttr) {
        initView()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int):
            super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
    }

    private fun initView() {
        surfaceTextureListener = mSurfaceTextureListener
    }

    fun start() {
        mFrameInfo.start()
    }
    fun stop() {
        mFrameInfo.stop()
    }
    fun setDataSource(file: String) {
        mFrameInfo.update(file)
    }

    private val mRenderer = object: GLRenderThread.Renderer {
        override fun onSurfaceCreated() {
            Log.i("zw","onSurfaceCreated")
            glEnable(GL_BLEND) //因为这里是两个图层，所以开启混合模式
            //GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glViewport(0,0, width, height)
            mFrameInfo.create(width, height)

        }

        override fun onDrawFrame() {
            //Log.i("zw","onDrawFrame")
            mFrameInfo.draw()
        }

        override fun onSurfaceDestroyed() {
            Log.i("zw","onSurfaceDestroyed")
            mFrameInfo.destroy()
        }
    }

    private val mSurfaceTextureListener = object: SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            //大小发生变化
            Log.i("zw","onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            //更新
            //Log.i("zw","onSurfaceTextureUpdated")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            mGLRenderThread?.stopThread()
            //被销毁
            Log.i("zw","onSurfaceTextureDestroyed")
            return false
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //创建成功
            mGLRenderThread = GLRenderThread(surface).apply {
                setRenderer(mRenderer)
                startThread()
            }
            Log.i("zw","onSurfaceTextureAvailable")
        }
    }

}