package com.android.iflyings.player

import android.media.*
import android.view.Surface
import com.orhanobut.logger.Logger
import android.media.MediaCodec
import android.net.Uri
import android.opengl.Matrix
import android.os.Build


class VideoPlayer {

    private var mVideoPath: String? = null
    private var mVideoUri: Uri? = null
    private lateinit var mOutputSurface: Surface
    private var mCallback: VideoPlayerCallback? = null

    private var isThreadRunning = true
    private lateinit var mVideoThread: VideoThread
    private lateinit var mAudioThread: AudioThread

    fun setVideoPath(videoPath: String) {
        mVideoPath = videoPath
    }
    fun setVideoUri(videoUri: Uri?) {
        mVideoUri = videoUri
    }
    fun setVideoPlayerCallback(callback: VideoPlayerCallback) {
        mCallback = callback
    }
    fun setOutputSurface(surface: Surface) {
        mOutputSurface = surface
    }

    fun start() {
        mVideoThread = VideoThread()
        mVideoThread.start()
        mAudioThread = AudioThread()
        //mAudioThread.start()
    }

    fun release() {
        isThreadRunning = false
    }

    inner class AudioThread: Thread() {
        private val TIMEOUT_US = 0L
        private var startMs = 0L
        //获取指定类型媒体文件所在轨道
        private fun getMediaTrackIndex(videoExtractor: MediaExtractor, MEDIA_TYPE: String): Int {
            var trackIndex = -1
            for (i in 0 until videoExtractor.trackCount) {
                //获取视频所在轨道
                val mediaFormat = videoExtractor.getTrackFormat(i)
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith(MEDIA_TYPE)) {
                    trackIndex = i
                    break
                }
            }
            return trackIndex
        }

        override fun run() {
            val audioExtractor = MediaExtractor()
            when {
                null != mVideoPath -> audioExtractor.setDataSource(mVideoPath!!)
                null != mVideoUri -> audioExtractor.setDataSource(MyApplication.getApplication(), mVideoUri!!, null)
                else -> return
            }

            val audioTrackIndex = getMediaTrackIndex(audioExtractor, "audio/")//音轨是"audio/"
            if (audioTrackIndex < 0) {
                mCallback?.noAudioData()
                return
            }

            val mediaFormat = audioExtractor.getTrackFormat(audioTrackIndex)
            val audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate,
                            if (audioChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT)
            val maxInputSize = mediaFormat.takeIf { it.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE) }?.
                    getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) ?: 2000000
            var audioInputBufferSize = if (minBufferSize > 0) minBufferSize * 4 else maxInputSize
            val frameSizeInBytes = audioChannels * 2
            audioInputBufferSize = audioInputBufferSize / frameSizeInBytes * frameSizeInBytes
            val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC,
                            audioSampleRate,
                            if (audioChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            audioInputBufferSize,
                            AudioTrack.MODE_STREAM)

            val audioCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME))
            audioCodec.configure(mediaFormat, null, null, 0)

            audioExtractor.selectTrack(audioTrackIndex)
            audioCodec.start()
            audioTrack.play()

            startMs = System.currentTimeMillis()
            while (isThreadRunning && decoder(audioCodec, audioExtractor, audioTrack)) { }

            audioCodec.stop()
            audioCodec.release()
            audioExtractor.release()
            audioTrack.stop()
            audioTrack.release()
        }

        private fun decoder(mediaCodec: MediaCodec, mediaExtractor: MediaExtractor, audioTrack: AudioTrack): Boolean {
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)!!
                val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)//将已写入数据的id为inputBufferIndex的ByteBuffer提交给MediaCodec进行解码
                    mediaExtractor.advance()//跳到下一个sample，然后再次读取数据
                }
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            while (outputBufferIndex >= 0) {
                val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)!!
                // 延时解码，跟视频时间同步
                sleepRender(bufferInfo, startMs)
                if (!isThreadRunning) return false
                // 如果解码成功，则将解码后的音频PCM数据用AudioTrack播放出来
                val chunkPCM = ByteArray(bufferInfo.size)
                outputBuffer.position(0)
                outputBuffer.get(chunkPCM, 0, chunkPCM.size)
                outputBuffer.clear()
                audioTrack.write(chunkPCM, 0, chunkPCM.size)

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)//更新surface
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            }
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                return false
            }

            return true
        }

        private fun sleepRender(bufferInfo: MediaCodec.BufferInfo, startMs: Long) {
            while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }

            }
        }

    }

    inner class VideoThread: Thread() {
        private val TIMEOUT_US = 0L
        private var startMs = 0L
        private var isNotDecoding = true
        //获取指定类型媒体文件所在轨道
        private fun getMediaTrackIndex(videoExtractor: MediaExtractor, MEDIA_TYPE: String): Int {
            var trackIndex = -1
            for (i in 0 until videoExtractor.trackCount) {
                //获取视频所在轨道
                val mediaFormat = videoExtractor.getTrackFormat(i)
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith(MEDIA_TYPE)) {
                    trackIndex = i
                    break
                }
            }
            return trackIndex
        }

        override fun run() {
            val videoExtractor = MediaExtractor()
            when {
                null != mVideoPath -> videoExtractor.setDataSource(mVideoPath!!)
                null != mVideoUri -> videoExtractor.setDataSource(MyApplication.getApplication(), mVideoUri!!, null)
                else -> return
            }

            val videoTrackIndex = getMediaTrackIndex(videoExtractor, "video/")//视轨的KEY_MIME是以"video/"开头的，音轨是"audio/"
            if (videoTrackIndex < 0) {
                mCallback?.noVideoData()
                return
            }

            val mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex)
            val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val during = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000//视频长度:毫秒

            val degree = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                mediaFormat.getInteger(MediaFormat.KEY_ROTATION)
            } else {
                0
            }

            val matrix = FloatArray(16)
            if (degree == 0) {
                Matrix.setIdentityM(matrix, 0)
                mCallback?.videoInfo(width, height, during, degree, matrix)
            } else {
                Matrix.setIdentityM(matrix, 0)
                Matrix.translateM(matrix, 0, 0.5f, 0.5f, 0f)
                Matrix.rotateM(matrix, 0, -1f * degree, 0f, 0f, 1f)
                Matrix.translateM(matrix, 0, -0.5f, -0.5f, 0f)
                mCallback?.videoInfo(height, width, during, degree, matrix)
            }

            val videoCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME))
            videoCodec.configure(mediaFormat, mOutputSurface, null, 0)

            videoExtractor.selectTrack(videoTrackIndex)//选择视轨所在的轨道子集(这样在之后调用readSampleData()/getSampleTrackIndex()方法时候，返回的就只是视轨的数据了，其他轨的数据不会被返回)
            videoCodec.start()

            isNotDecoding = true
            startMs = System.currentTimeMillis()
            isThreadRunning = true
            while (isThreadRunning && decoder(videoCodec, videoExtractor)) { }
            isThreadRunning = false

            mOutputSurface.release()
            videoCodec.stop()
            //videoCodec.flush()
            videoCodec.release()
            videoExtractor.release()

            Thread.sleep(100)

            mCallback?.playCompleted()
        }

        private fun decoder(mediaCodec: MediaCodec, mediaExtractor: MediaExtractor): Boolean {
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)!!
                inputBuffer.clear()
                val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)//将已写入数据的id为inputBufferIndex的ByteBuffer提交给MediaCodec进行解码
                    mediaExtractor.advance()//跳到下一个sample，然后再次读取数据
                }
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            while (outputBufferIndex >= 0) {
                if (isNotDecoding) {
                    mCallback?.playStarted()
                    isNotDecoding = false
                }
                sleepRender(bufferInfo, startMs)
                if (!isThreadRunning) return false
                mediaCodec.releaseOutputBuffer(outputBufferIndex, true)//更新surface
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            }
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //此处可以或得到视频的实际分辨率，用以修正宽高比
                val mediaFormat = mediaCodec.outputFormat
                val mediaWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
                val mediaHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
                //val mediaRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                Logger.i("mediaWidth = $mediaWidth, mediaHeight = $mediaHeight")
            }
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                return false
            }

            return true
        }

        private fun sleepRender(bufferInfo: MediaCodec.BufferInfo, startMs: Long) {
            while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }

            }
        }
    }

    interface VideoPlayerCallback {

        fun videoInfo(width: Int, height: Int, during: Long, angle: Int, matrix: FloatArray)

        fun noAudioData()

        fun noVideoData()

        fun playStarted()

        fun playCompleted()

    }
}