/**
 * Created by ST on 2/12/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Utility class for file-related operations such as saving, retrieving,
 * converting, and handling various file types.
 */
object FileUtils {

    /**
     * Enum class representing different file types.
     */
    enum class FileType {
        IMAGE, AUDIO, DOCUMENT, VIDEO
    }

    /** Root path where files will be saved */
    private val rootPath: String = "${Environment.getExternalStorageDirectory().absolutePath}/Teamyar/"
    private val downloadProgressMap = HashMap<String, MutableLiveData<Int>>()

    /**
     * Checks if a directory is empty.
     * @param directory File representing the directory
     * @return Boolean true if empty, otherwise false
     */
    fun isDirectoryEmpty(directory: File?): Boolean {
        return directory?.listFiles()?.isEmpty() ?: true
    }

    /**
     * Generates a unique image filename based on the current timestamp.
     * @return String Filename in `JPEG_yyyyMMdd_HHmmss` format
     */
    private fun generateImageFileName(): String =
        "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

    /**
     * Converts a Bitmap into a file and saves it in the "Teamyar Images" directory.
     * @param bitmap Bitmap to be saved
     * @param fileName Optional filename (default: generated)
     * @param format Bitmap compression format (default: JPEG)
     * @param quality Compression quality (default: 80)
     * @return File Saved file object
     */
    fun saveBitmapToFile(
        bitmap: Bitmap,
        fileName: String = generateImageFileName(),
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ): File {
        val subDirectory = File(rootPath, "Teamyar Images").apply { mkdirs() }
        val destinationFile = File(subDirectory, fileName)

        FileOutputStream(destinationFile).use { fos ->
            bitmap.compress(format, quality, fos)
            fos.flush()
        }

        return destinationFile
    }

    /**
     * Creates an image file in a specified directory.
     * @param directory File representing the parent directory
     * @param fileName Optional filename (default: generated)
     * @return File Newly created file object
     */
    fun createImageFile(directory: File, fileName: String = generateImageFileName()): File {
        return File(directory.apply { mkdirs() }, fileName)
    }

    /**
     * Saves a captured image bitmap to a file.
     * @param bitmap Bitmap image to save
     * @param imageFile Destination file
     */
    fun saveCapturedImage(bitmap: Bitmap, imageFile: File) {
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
        }
    }

    /**
     * Retrieves the file size of a given URI.
     * @param uri File URI
     * @param contentResolver ContentResolver instance
     * @return Long File size in bytes
     */
    fun getFileSize(uri: Uri, contentResolver: ContentResolver): Long {
        return contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
    }

    /**
     * Gets the last modified file in a folder.
     * @param folderPath Path to the directory
     * @return File? Last modified file or null if folder is empty
     */
    fun getLastModifiedFile(folderPath: String): File? {
        return File(folderPath).listFiles()?.maxByOrNull { it.lastModified() }
    }

    /**
     * Gets a file extension from its name.
     * @param fileName Name of the file
     * @return String File extension (MIME type)
     */
    fun getFileMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true) -> "application/msword"
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".ppt", true) || fileName.endsWith(".pptx", true) -> "application/vnd.ms-powerpoint"
            fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) -> "application/vnd.ms-excel"
            fileName.endsWith(".zip", true) || fileName.endsWith(".rar", true) -> "application/x-wav"
            fileName.endsWith(".mp3", true) || fileName.endsWith(".wav", true) -> "audio/x-wav"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true) -> "image/jpeg"
            fileName.endsWith(".mp4", true) || fileName.endsWith(".avi", true) -> "video/*"
            fileName.endsWith(".txt", true) -> "text/plain"
            fileName.endsWith(".apk", true) -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }

    /**
     * Converts a file into a byte array.
     * @param file File to be converted
     * @return ByteArray File content as a byte array
     */
    fun fileToByteArray(file: File): ByteArray {
        return try {
            FileInputStream(file).use { fis ->
                ByteArray(file.length().toInt()).apply { fis.read(this) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    /**
     * Retrieves the real file path from a URI.
     * @param context Context instance
     * @param uri File URI
     * @return String? Real file path or null if not found
     */
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            } else null
        }
    }

    /**
     * Deletes a file or directory recursively.
     * @param target File or directory to delete
     * @return Boolean True if successfully deleted, false otherwise
     */
    fun deleteRecursively(target: File?): Boolean {
        if (target == null || !target.exists()) return false
        return if (target.isDirectory) {
            target.listFiles()?.all { deleteRecursively(it) } == true && target.delete()
        } else {
            target.delete()
        }
    }

    /**
     * Finds a file by name in a specific directory.
     * @param fileName Name of the file
     * @param directory Directory where the search should occur
     * @return File? Found file or null if not found
     */
    fun findFileInDirectory(fileName: String, directory: File): File? {
        return directory.listFiles()?.find { it.name.contains(fileName, true) }
    }

    /**
     * Gets the name of a file from its absolute path.
     * @param filePath Absolute file path
     * @return String Extracted file name
     */
    fun getFileNameFromPath(filePath: String): String {
        return filePath.substringAfterLast("/")
    }


    fun Uri.toFilePath(context: Context): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA) // Deprecated but needed for older versions

        return context.contentResolver.query(this, projection, null, null, null)?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                it.getString(columnIndex)
            } else {
                null
            }
        }
    }
}
