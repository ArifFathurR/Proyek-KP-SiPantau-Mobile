package com.example.sipantau.util

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtil {
    fun saveUriToFile(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val dir = File(context.filesDir, "images")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            input.use { inp -> file.outputStream().use { out -> inp.copyTo(out) } }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyFileToInternal(context: Context, srcFile: File, dstFileName: String): String? {
        return try {
            val dir = File(context.filesDir, "images")
            if (!dir.exists()) dir.mkdirs()
            val dst = File(dir, dstFileName)
            srcFile.inputStream().use { ins -> dst.outputStream().use { os -> ins.copyTo(os) } }
            dst.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
