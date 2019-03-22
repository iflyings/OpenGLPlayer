package com.android.iflyings.player.info

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.view.Surface
import com.android.iflyings.player.VideoPlayer
import com.android.iflyings.player.shader.MediaShader
import com.android.iflyings.player.shader.VideoShader
import com.android.iflyings.player.utils.TextureUtils


class VideoInfo private constructor(texRect: Rect? = null) : MediaInfo(texRect) {

    private lateinit var mVideoFile: String
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mVideoPlayer: VideoPlayer? = null
    private var isVideoPlaying = false

    override fun onCreated(width: Int, height: Int): MediaShader {
        mVideoPlayer = openVideoPlayer()
        return VideoShader.getInstance()
    }

    override fun onDraw() {
        mSurfaceTexture?.takeIf { isVideoPlaying }?.updateTexImage()//更新纹理
    }

    override fun onDestroyed() {
        isVideoPlaying = false
        mVideoPlayer?.release()
        mVideoPlayer = null
        mSurfaceTexture?.release()
        mSurfaceTexture = null
    }

    private fun bindVideoPlayer(videoPlayer: VideoPlayer, textureId: Int) {
        postInUserThread(Runnable {
            val surfaceTexture = SurfaceTexture(textureId)
            val surface = Surface(surfaceTexture)
            videoPlayer.setOutputSurface(surface)
            videoPlayer.start()
            mSurfaceTexture = surfaceTexture
        })
    }
    private fun openVideoPlayer(): VideoPlayer {
        return VideoPlayer().also {
            it.setVideoPath(mVideoFile)
            it.setVideoPlayerCallback(object: VideoPlayer.VideoPlayerCallback {
                override fun videoInfo(width: Int, height: Int, during: Long, angle: Int, matrix: FloatArray) {
                    setTextureSize(width, height)
                    setTextureMatrix(matrix)
                }
                override fun noAudioData() {

                }
                override fun noVideoData() {
                    notifyMediaFailed("it is not a video")
                }
                override fun playStarted() {
                    notifyMediaCreated()
                    isVideoPlaying = true
                }
                override fun playCompleted() {
                    mVideoPlayer?.release()
                    mVideoPlayer = null
                    mSurfaceTexture?.release()
                    mSurfaceTexture = null
                    notifyMediaCompleted()
                }
            })
            postInGLThread(Runnable {
                val textureId = TextureUtils.createVideoTexture()
                setTextureId(textureId)
                bindVideoPlayer(it, textureId)
            })
        }
    }

    override fun toString(): String {
        return super.toString() + "\n" + "VideoPath=$mVideoFile"
    }

    companion object {
        fun from(file: String, rect: Rect? = null): VideoInfo {
            val videoInfo = VideoInfo(rect)
            videoInfo.mVideoFile = file
            return videoInfo
        }
    }
/*
    private fun bindSurfaceTexture(mediaPlayer: IMediaPlayer, textureId: Int) {
        postInUserThread(Runnable {
            synchronized(this) {
                val surfaceTexture = SurfaceTexture(textureId)
                surfaceTexture.setOnFrameAvailableListener {
                    Logger.i("setOnFrameAvailableListener = $mVideoPath")
                    isUpdateTexture = true
                }
                val surface = Surface(surfaceTexture)
                mediaPlayer.setSurface(surface)
                surface.release()
                mSurfaceTexture = surfaceTexture
            }
        })
    }
    private fun openIjkPlayer(): IMediaPlayer {
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG)
        return IjkMediaPlayer().apply {
            //setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
            //setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
            //setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)
            dataSource = mVideoPath
            isLooping = false
            setOnVideoSizeChangedListener { _, width, height, _, _ -> setTextureSize(width, height) }
            setOnPreparedListener {
                postInGLThread(Runnable {
                    mVideoTextureId = TextureUtils.getTextureFromVideo()
                    bindSurfaceTexture(this, mVideoTextureId)
                })
                isMediaPrepared = true
                if (isVideoPlaying) {
                    it.start()
                } else {
                    it.pause()
                    //mp.seekTo(100)
                }
            }
            setOnCompletionListener {
                synchronized(this) {
                    isMediaPrepared = false
                    it.release()
                    mMediaPlayer = null
                    //mAudioManager.abandonAudioFocus(null)
                    mSurfaceTexture?.release()
                    mSurfaceTexture = null
                }
                notifyMediaCompleted()
            }
            setOnErrorListener { _, what, extra ->
                notifyMediaFailed("$mVideoPath Error:what = $what,extra = $extra")
                return@setOnErrorListener false
            }
            prepareAsync()
        }
    }
*/
}