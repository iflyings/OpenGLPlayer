package com.android.iflyings.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.provider.DocumentFile
import android.support.v7.app.AlertDialog
import com.android.iflyings.player.utils.FileUtils
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.widget.*
import java.io.File


open class SettingActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        ll_import_media.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/zip"
            startActivityForResult(intent, REQUEST_PICK_FILE)
        }
        ll_select_media.setOnClickListener {
            val listData = getMediaList("$filesDir")
            val listView = LayoutInflater.from(this).inflate(R.layout.listview, null) as ListView
            val adapter = SimpleAdapter(this, listData, android.R.layout.simple_list_item_1,
                    arrayOf("name"), intArrayOf(android.R.id.text1))
            listView.adapter = adapter
            listView.setOnItemClickListener { _, _, position: Int, _ ->
                PlayerActivity.play(this@SettingActivity, listData[position].getValue("path"))
            }

            val dialog = AlertDialog.Builder(this)
                    .setTitle(R.string.pick_media_package)
                    .setIcon(R.mipmap.ic_launcher)
                    .setView(listView)
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
        ll_select_folder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQUEST_PICK_FOLDER)
        }
        ll_exit_app.setOnClickListener {
            PlayerActivity.exit(this@SettingActivity)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_FILE -> {
                    val data = intent?.data ?: return
                    val path = FileUtils.getFilePath(this@SettingActivity, data) ?: return
                    val dialog = AlertDialog.Builder(this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.import_file_please_waiting)
                            .setCancelable(false)
                            .create()
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.show()
                    GlobalScope.launch(Dispatchers.IO) {
                        FileUtils.unZipFile(path, "$filesDir/media")
                        GlobalScope.launch(Dispatchers.Main) {
                            dialog.dismiss()
                        }
                    }
                }
                REQUEST_PICK_FOLDER -> {
                    val data = intent?.data ?: return
                    val filePath = FileUtils.getFullPathFromTreeUri(this, data)!!
                    PlayerActivity.play(this@SettingActivity, filePath)
                }
            }
        }
    }

    private fun getMediaList(filePath: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val parentDir = File(filePath + File.separator + "media")
        if (!parentDir.exists()) return list
        if (parentDir.isDirectory && parentDir.listFiles().isNotEmpty()) {
            for (file in parentDir.listFiles()) {
                if (file.isDirectory) {
                    val f = File(file, "package.json")
                    if (f.exists()) {
                        val map = HashMap<String, String>()
                        map["name"] = file.name!!
                        map["path"] = file.absolutePath
                        list.add(map)
                    }
                }
            }
        }
        return list
    }

    companion object {

        const val REQUEST_PICK_FILE = 640
        const val REQUEST_PICK_FOLDER = 470

    }
}
