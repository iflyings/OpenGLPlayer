package com.android.iflyings.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.view.KeyEvent
import com.android.iflyings.player.utils.PreferenceUtils


class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()

        checkSelfPermissions()
    }

    override fun onPause() {
        super.onPause()

        psvPlayer.stop()
    }

    private fun checkSelfPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
            }
        } else {
            psvPlayer.post { initData() }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.app_name).setIcon(R.mipmap.ic_launcher)
                            .setMessage(R.string.no_permission_to_read_external_storage)
                            .setPositiveButton(android.R.string.ok) { _, _ -> this@PlayerActivity.finish() }
                            .show()
                } else {
                    initData()
                }
            }
        }
    }

    private fun initData() {
        var filePath: String? = null
        when (intent.getIntExtra(MY_INTENT_COMMAND, -1)) {
            MY_COMMAND_OF_EXIT -> {
                finish()
                return
            }
            MY_COMMAND_OF_PLAY -> {
                filePath = intent.getStringExtra(MY_PLAY_PATH)!!
            }
        }
        if (filePath == null) {
            filePath = PreferenceUtils.getMediaPath(this) ?: return
        } else {
            if (filePath != PreferenceUtils.getMediaPath(this)) {
                PreferenceUtils.setMediaPath(this, filePath)
            }
        }

        waitContainer.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.Default) {
            psvPlayer.setDataSource(filePath)
            GlobalScope.launch(Dispatchers.Main) {
                waitContainer.visibility = View.INVISIBLE
            }
            psvPlayer.start()
            delay(5000)
            //mediaWindow.setFontInfo("java中国话yunÃÇŸŒú", 50)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    /*
    override fun onBackPressed() {
        finish()
    }
    */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BACK -> {
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
            }
            else -> {}
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        private const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 236
        private const val MY_INTENT_COMMAND = "cmd"
        private const val MY_COMMAND_OF_EXIT = 45
        private const val MY_COMMAND_OF_PLAY = 76
        private const val MY_PLAY_PATH = "path"

        fun play(context: Context, path: String) {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(MY_INTENT_COMMAND, MY_COMMAND_OF_PLAY)
            intent.putExtra(MY_PLAY_PATH, path)
            context.startActivity(intent)
        }

        fun exit(context: Context) {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(MY_INTENT_COMMAND, MY_COMMAND_OF_EXIT)
            context.startActivity(intent)
        }
    }
}
