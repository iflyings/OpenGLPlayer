package com.android.iflyings.player

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class MyApplication: Application() {

    private lateinit var refWatcher: RefWatcher

    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: Context
        fun getApplication(): Context {
            return instance
        }

        fun getRefWatcher(context: Context): RefWatcher {
            val application = context.applicationContext as MyApplication
            return application.refWatcher
        }

    }
    /*
    private fun setupLeakCanary(): RefWatcher {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return RefWatcher.DISABLED
        }
        return LeakCanary.install(this)
    }
    */
    override fun onCreate() {
        super.onCreate()

        instance = this

        //refWatcher = setupLeakCanary()

        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin("libijkplayer.so")

        val formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(true)   //（可选）是否显示线程信息。 默认值为true
                .methodCount(1)         //（可选）要显示的方法行数。 默认2
                .methodOffset(5)        //（可选）隐藏内部方法调用到偏移量。 默认5
                .tag("zw")              //（可选）每个日志的全局标记。 默认PRETTY_LOGGER
                .build()
        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))//根据上面的格式设置logger相应的适配器
    }
}