package com.android.iflyings.player.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.*

import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.nio.file.Files.exists
import android.os.Build
import android.support.v4.provider.DocumentFile
import java.lang.reflect.Method
import java.nio.file.Files.deleteIfExists


object FileUtils {

    fun getAllFileInFolder(folderPath: String, filters: List<String>): List<String> {
        val fileLists = ArrayList<String>()
        val file = File(folderPath)
        if (file.isDirectory && file.list() != null && file.list().isNotEmpty()) {
            for (f in file.listFiles()) {
                if (f.isFile) {
                    val name = f.name
                    for (filter in filters) {
                        if (name.toLowerCase().endsWith(filter)) {
                            fileLists.add(f.path)
                            break
                        }
                    }
                } else if (f.isDirectory) {
                    fileLists.addAll(getAllFileInFolder(f.path, filters))
                }
            }
        }
        return fileLists
    }

    fun getStorageList(context: Context): List<File> {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageDataList = ArrayList<File>()
        try {
            val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList = storageManager.javaClass.getMethod("getVolumeList")
            val getPath = storageVolumeClazz.getMethod("getPath")
            //Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            val result = getVolumeList.invoke(storageManager)
            val length = java.lang.reflect.Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = java.lang.reflect.Array.get(result, i)
                val path = getPath.invoke(storageVolumeElement) as String
                //boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                storageDataList.add(File(path))
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return storageDataList
    }

    fun getFilePath(context: Context, uri: Uri): String? {
        // DocumentProvider
        when {
            DocumentsContract.isDocumentUri(context, uri) -> when {
                // ExternalStorageProvider
                isExternalStorageDocument(uri) -> return getExternalStorageDocumentPath(context, uri)
                // DownloadsProvider
                isDownloadsDocument(uri) -> return getDownloadsDocumentPath(context, uri)
                // MediaProvider
                isMediaDocument(uri) -> return getMediaDocumentPath(context, uri)
            }
            "content".toLowerCase() == uri.scheme -> { // MediaStore (and general)
                // Return the remote address
                if (isGooglePhotosUri(uri)) {
                    return uri.lastPathSegment
                }
                return getDataColumn(context, uri, null, null)
            }
            "file".toLowerCase() == uri.scheme -> // File
                return uri.path
        }
        return uri.path
    }

    private fun getDownloadsDocumentPath(context: Context, uri: Uri): String? {
        val id = DocumentsContract.getDocumentId(uri)
        val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
        return getDataColumn(context, contentUri, null, null)
    }

    private fun getMediaDocumentPath(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri) ?: return null
        val split = docId.split(":")
        if (split.size < 2) {
            return null
        }
        val contentUri = when (split[0]) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> null
        } ?: return null
        return getDataColumn(context, contentUri, "_id=?", arrayOf(split[1]))
    }

    private fun getExternalStorageDocumentPath(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri) ?: return null
        val split = docId.split(":")
        if (split.size < 2) {
            return null
        }
        return if ("primary".toLowerCase() == split[0]) {
            //内置存储
            Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
        } else {
            getVolumePath(context, split[0]) + File.separator + split[1]
        }
    }

    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        val column = "_data"
        var data: String? = null
        val cursor = context.contentResolver.query(uri, arrayOf(column), selection, selectionArgs, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                data = cursor.getString(index)
                cursor.close()
            }
        }
        return data
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    fun getFullPathFromTreeUri(context: Context, treeUri: Uri?): String? {
        if (treeUri == null) return null

        var volumePath = getVolumePath(context, getVolumeIdFromTreeUri(treeUri)!!) ?: return File.separator
        if (volumePath.endsWith(File.separator))
            volumePath = volumePath.substring(0, volumePath.length - 1)

        var documentPath = getDocumentPathFromTreeUri(treeUri)!!
        if (documentPath.endsWith(File.separator))
            documentPath = documentPath.substring(0, documentPath.length - 1)

        return if (documentPath.isNotEmpty()) {
            if (documentPath.startsWith(File.separator))
                volumePath + documentPath
            else
                volumePath + File.separator + documentPath
        }
        else volumePath
    }

    private fun getVolumePath(context: Context, volumeId: String): String? {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
        val getVolumeList = storageManager.javaClass.getMethod("getVolumeList")
        val getUuid = storageVolumeClazz.getMethod("getUuid")
        val getPath = storageVolumeClazz.getMethod("getPath")
        val isPrimary = storageVolumeClazz.getMethod("isPrimary")

        val result = getVolumeList.invoke(storageManager)
        val length = java.lang.reflect.Array.getLength(result)
        for (i in 0 until length) {
            val storageVolumeElement = java.lang.reflect.Array.get(result, i)
            val uuid = getUuid.invoke(storageVolumeElement) as String?
            val primary = isPrimary.invoke(storageVolumeElement) as Boolean

            // primary volume?
            if (primary && "primary" == volumeId)
                return getPath.invoke(storageVolumeElement) as String

            // other volumes?
            if (uuid != null && uuid == volumeId)
                return getPath.invoke(storageVolumeElement) as String
        }
        return null
    }

    private fun getVolumeIdFromTreeUri(treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":")
        return if (!split.isEmpty()) split[0] else null
    }

    private fun getDocumentPathFromTreeUri(treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":")
        return if (split.size >= 2) split[1] else File.separator
    }

    @Throws(IOException::class, FileNotFoundException::class, ZipException::class)
    fun unZipFile(archive: String, decompressDir: String) {
        val zipFile = ZipFile(archive)
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            val path = "$decompressDir/${zipEntry.name}"
            if (zipEntry.isDirectory) {
                val decompressDirFile = File(path)
                if (!decompressDirFile.exists()) {
                    decompressDirFile.mkdirs()
                }
            } else {
                val fileDir = path.substring(0, path.lastIndexOf("/"))
                val fileDirFile = File(fileDir)
                if (!fileDirFile.exists()) {
                    fileDirFile.mkdirs()
                }
                val bos = BufferedOutputStream(FileOutputStream(path))
                val bis = BufferedInputStream(zipFile.getInputStream(zipEntry))
                val readContent = ByteArray(1024)
                var readCount = bis.read(readContent)
                while (readCount > 0) {
                    bos.write(readContent, 0, readCount)
                    readCount = bis.read(readContent)
                }
                bos.close()
                bis.close()
            }
        }
        zipFile.close()
    }

    fun getDefaultMediaDir(context: Context): String {
        return "${context.filesDir}/media"
    }

}
