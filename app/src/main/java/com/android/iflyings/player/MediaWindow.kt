package com.android.iflyings.player

import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import com.android.iflyings.player.info.MediaInfo
import com.android.iflyings.player.info.TextInfo
import com.android.iflyings.player.model.WindowData
import com.android.iflyings.player.transformer.NormalTransformer
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.*


class MediaWindow private constructor(rect: Rect? = null) {

    private val mWindowData = WindowData(rect)
    private val mScroller = MediaScroller()
    private val runOnDraw = LinkedList<Runnable>()

    private val mAllMediaLists = mutableListOf<MediaInfo>()
    private var mMediaModeIndex = 0

    private val mTextLock = Object()
    private val mMediaLock = Object()
    private var mTextMedia: MediaInfo? = null
    private var mNowMedia: MediaInfo? = null
    private var mNextMedia: MediaInfo? = null

    private val mTextTransformer = NormalTransformer()
    private var mMediaTransformer: MediaTransformer? = null
    private var mAnimationJob: Job? = null
    private var mAnimationPosition = -100f

    private val mOnMediaListener = object: MediaInfo.OnMediaListener {
        override fun runInGLThread(r: Runnable) {
            runOnDraw(r)
        }
        override fun runInUserThread(r: Runnable) {
            GlobalScope.launch(Dispatchers.Default) {
                r.run()
            }
        }
        override fun onCreated(m: MediaInfo) {
            Log.i("zw", "onCreated")
            startAnimation()
        }
        override fun onCompleted(m: MediaInfo) {
            Log.i("zw", "onCompleted")
            gotoNextMedia()
        }
        override fun onFailed(m: MediaInfo, msg: String) {
            Log.i("zw", "onFailed")
            gotoNextMedia()
        }
        override fun onDestroyed(m: MediaInfo) {
            Log.i("zw", "onDestroyed")
        }
    }

    val windowLeft
        get() = mWindowData.windowLeft
    val windowTop
        get() = mWindowData.windowTop
    val windowWidth
        get() = mWindowData.windowWidth
    val windowHeight
        get() = mWindowData.windowHeight

    private fun setWindowRect(rect: Rect?) {
        mWindowData.setWindowRect(rect)
    }

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
    private fun gotoNextMedia() {
        synchronized(mMediaLock) {
            if (mMediaModeIndex < mAllMediaLists.size) {
                mNextMedia = mAllMediaLists[mMediaModeIndex].also {
                    it.mediaCreate(mWindowData)
                    mMediaTransformer = it.getMediaTransformer()
                    if (mAllMediaLists.size > 0) {
                        mMediaModeIndex = (mMediaModeIndex + 1) % mAllMediaLists.size
                    }
                }
            }
        }
    }
    private fun startAnimation() {
        mScroller.startScroll(0f, 2000)
        mAnimationJob = GlobalScope.launch(Dispatchers.Default) {
            while (mScroller.computeScrollOffset()) {
                synchronized(mMediaLock) {
                    mAnimationPosition = - mScroller.currX
                }
                delay(50)
            }
            synchronized(mMediaLock) {
                mAnimationPosition = 0f
                mNowMedia?.mediaDestroy()
                mNowMedia = mNextMedia
                mNextMedia = null
            }
            mAnimationJob = null
        }
    }

    fun start(width: Int, height: Int) {
        mWindowData.setScreenSize(width, height)
        mMediaModeIndex = 0
        if (mAllMediaLists.size > 0) {
            gotoNextMedia()
        }
    }
    fun stop() {
        mAnimationPosition = -100f
        mAnimationJob?.cancel()
        mAnimationJob = null
        mNowMedia?.mediaDestroy()
        mNowMedia = null
        mNextMedia?.mediaDestroy()
        mNextMedia = null
        mTextMedia?.mediaDestroy()
        mTextMedia = null

        mAllMediaLists.clear()
    }

    fun setFontInfo(textString: String, textSize: Int) {
        val callback = object: MediaInfo.OnMediaListener {
            override fun runInGLThread(r: Runnable) {
                runOnDraw(r)
            }
            override fun runInUserThread(r: Runnable) {
                GlobalScope.launch(Dispatchers.Default) {
                    r.run()
                }
            }
            override fun onCreated(m: MediaInfo) {
            }
            override fun onCompleted(m: MediaInfo) {
                m.mediaDestroy()
                mTextMedia = null
            }
            override fun onFailed(m: MediaInfo, msg: String) {
                m.mediaDestroy()
                mTextMedia = null
            }
            override fun onDestroyed(m: MediaInfo) {

            }
        }
        synchronized(mTextLock) {
            mTextMedia?.mediaDestroy()
            mTextMedia = TextInfo.from(textString, textSize, Color.RED).also {
                it.setOnMediaListener(callback)
                it.mediaCreate(mWindowData)
            }
        }

    }
    
    private fun drawTextInfo(textureIndex: Int): Int {
        var index = textureIndex
        synchronized(mTextLock) {
            if (null != mTextMedia) {
                mTextTransformer.transformMedia(mTextMedia!!, 0f)
                index = mTextMedia!!.mediaDraw(index)
            }
        }
        return index
    }
    private fun drawMediaInfo(textureIndex: Int): Int {
        var index = textureIndex
        synchronized(mMediaLock) {
            //Log.i("zw", "======================================================================")
            //Log.i("zw", "mAnimationPosition=$mAnimationPosition")
            mMediaTransformer?.also {
                mNowMedia?.apply {
                    it.transformMedia(this, mAnimationPosition)
                    index = this.mediaDraw(index)
                    //Log.i("zw", "mNowMedia=$this")
                }
                mNextMedia?.apply {
                    it.transformMedia(this, 1 + mAnimationPosition)
                    index = this.mediaDraw(index)
                    //Log.i("zw", "mNextMedia=$this")
                }
                //Log.i("zw", "mAnimationPosition = $mAnimationPosition")
            }
        }
        return index
    }
    // 在 GLThread 中运行
    fun draw(textureIndex: Int): Int {
        runPendingOnDrawTasks()
        var index = textureIndex
        index = drawMediaInfo(index)
        index = drawTextInfo(index)
        return index
    }

    interface MediaTransformer {

        fun transformMedia(mediaInfo: MediaInfo, position: Float)

    }

    companion object {

        fun from(file: String, jsonObject: JSONObject? = null): MediaWindow {
            val mediaWindow = MediaWindow(null)
            if (null != jsonObject) {
                val rect = jsonObject.takeIf { it.has("rect") && !it.isNull("rect") }?.let {
                    val array = it.getJSONArray("rect")
                    Rect(array.getInt(0), array.getInt(1), array.getInt(2), array.getInt(3))
                }
                mediaWindow.setWindowRect(rect)
                val mediaArray = jsonObject.getJSONArray("list")
                for (i in 0 until mediaArray.length()) {
                    val mediaInfo = MediaInfo.from(file, mediaArray.getJSONObject(i))
                    mediaInfo.setOnMediaListener(mediaWindow.mOnMediaListener)
                    mediaWindow.mAllMediaLists.add(mediaInfo)
                }
            } else {
                val dir = File(file)
                if (dir.isDirectory && dir.listFiles().isNotEmpty()) {
                    for (f in dir.listFiles()) {
                        if (f.isFile) {
                            val name = f.name!!
                            if (name.toLowerCase().endsWith(".jpg") ||
                                    name.toLowerCase().endsWith(".jpeg") ||
                                    name.toLowerCase().endsWith(".bmp") ||
                                    name.toLowerCase().endsWith(".png")) {
                                val mediaInfo = MediaInfo.from(f.absolutePath, "image")
                                mediaInfo.setOnMediaListener(mediaWindow.mOnMediaListener)
                                mediaWindow.mAllMediaLists.add(mediaInfo)
                            } else if (name.toLowerCase().endsWith(".mp4") ||
                                    name.toLowerCase().endsWith(".avi") ||
                                    name.toLowerCase().endsWith(".mkv") ||
                                    name.toLowerCase().endsWith(".mov") ||
                                    name.toLowerCase().endsWith(".webm")) {
                                val mediaInfo = MediaInfo.from(f.absolutePath, "video")
                                mediaInfo.setOnMediaListener(mediaWindow.mOnMediaListener)
                                mediaWindow.mAllMediaLists.add(mediaInfo)
                            }
                        }
                    }
                }
            }
            return mediaWindow
        }
    }
}
