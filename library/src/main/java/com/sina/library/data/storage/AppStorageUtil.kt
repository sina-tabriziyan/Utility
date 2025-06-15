package com.sina.library.data.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.sina.library.data.enums.FileType
import com.sina.library.data.enums.MimeType
import com.sina.library.utility.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Date
import java.util.Locale

object AppStorageUtil {
    private val baseDirectory: File =
        File(Environment.getExternalStorageDirectory(), "Android/data/com.teamyar/files")


    /**
     * Initializes the app-specific directories on external storage.
     * This method should be called during the app's initialization phase,
     * e.g. in your Application class or MainActivity.
     */
    fun initializeAppDirectories(context: Context) {
        val baseDir: File? = context.getExternalFilesDir(null)
        baseDir?.let { base ->
            FileType.entries.forEach { fileType ->
                val dir = File(base, fileType.directoryName)
                if (!dir.exists()) {
                    val wasCreated = dir.mkdirs()
                    if (wasCreated) {
                        Log.d("AppStorageUtils", "Directory created: ${dir.absolutePath}")
                    } else {
                        Log.e("AppStorageUtils", "Failed to create directory: ${dir.absolutePath}")
                    }
                } else {
                    Log.d("AppStorageUtils", "Directory already exists: ${dir.absolutePath}")
                }
            }
        } ?: run {
            Log.e("AppStorageUtils", "External storage is not available.")
        }
    }

    /**
     * Creates a file in the specified app-specific subdirectory.
     *
     * @param context The application context.
     * @param fileName The name of the file to create.
     * @param fileType The type of the file (IMAGE, AUDIO, DOCUMENT, VIDEO).
     * @return The File object representing the created file, or null if creation failed.
     */
    fun createFile(context: Context, fileName: String, fileType: FileType): File? {
        val baseDir: File? = context.getExternalFilesDir(null)
        return if (baseDir != null) {
            val dir = File(baseDir, fileType.directoryName)
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e("AppStorageUtils", "Failed to create directory: ${dir.absolutePath}")
                return null
            }
            val file = File(dir, fileName)
            try {
                if (file.createNewFile()) {
                    Log.d("AppStorageUtils", "File created: ${file.absolutePath}")
                } else {
                    Log.d("AppStorageUtils", "File already exists: ${file.absolutePath}")
                }
                file
            } catch (e: IOException) {
                Log.e("AppStorageUtils", "Error creating file: ${e.message}")
                null
            }
        } else {
            Log.e("AppStorageUtils", "External storage is not available.")
            null
        }
    }

    /**
     * Checks if a file exists in the specified app-specific subdirectory.
     *
     * @param context The application context.
     * @param fileName The name of the file to check.
     * @param fileType The type of the file.
     * @return True if the file exists, false otherwise.
     */
    fun doesFileExist(context: Context, fileName: String, fileType: FileType): Boolean {
        val baseDir: File? = context.getExternalFilesDir(null)
        return if (baseDir != null) {
            val file = File(baseDir, "${fileType.directoryName}/$fileName")
            file.exists()
        } else {
            Log.e("AppStorageUtils", "External storage is not available.")
            false
        }
    }

    /**
     * Retrieves the size (in bytes) of a file in the specified app-specific subdirectory.
     *
     * @param context The application context.
     * @param fileName The name of the file.
     * @param fileType The type of the file.
     * @return The size of the file in bytes, or -1 if the file doesn't exist or an error occurs.
     */
    fun getFileSize(context: Context, fileName: String, fileType: FileType): Long {
        val baseDir: File? = context.getExternalFilesDir(null)
        return if (baseDir != null) {
            val file = File(baseDir, "${fileType.directoryName}/$fileName")
            if (file.exists()) {
                file.length()
            } else {
                Log.e("AppStorageUtils", "File does not exist: ${file.absolutePath}")
                -1
            }
        } else {
            Log.e("AppStorageUtils", "External storage is not available.")
            -1
        }
    }

    /**
     * Deletes a file in the specified app-specific subdirectory.
     *
     * @param context The application context.
     * @param fileName The name of the file to delete.
     * @param fileType The type of the file.
     * @return True if the file was successfully deleted, false otherwise.
     */
    fun deleteFile(context: Context, fileName: String, fileType: FileType): Boolean {
        val baseDir: File? = context.getExternalFilesDir(null)
        return if (baseDir != null) {
            val file = File(baseDir, "${fileType.directoryName}/$fileName")
            if (file.exists()) {
                val wasDeleted = file.delete()
                if (wasDeleted) {
                    Log.d("AppStorageUtils", "File deleted: ${file.absolutePath}")
                } else {
                    Log.e("AppStorageUtils", "Failed to delete file: ${file.absolutePath}")
                }
                wasDeleted
            } else {
                Log.e("AppStorageUtils", "File does not exist: ${file.absolutePath}")
                false
            }
        } else {
            Log.e("AppStorageUtils", "External storage is not available.")
            false
        }
    }

    /**
     * Lists all files in the specified app-specific subdirectory.
     *
     * @param context The application context.
     * @param fileType The type of files to list.
     * @return A list of File objects representing the files, or an empty list if the directory is missing or an error occurs.
     */
    fun listFiles(context: Context, fileType: FileType): List<File> {
        val baseDir: File? = context.getExternalFilesDir(null)
        return if (baseDir != null) {
            val dir = File(baseDir, fileType.directoryName)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.toList() ?: emptyList()
            } else {
                Log.e(
                    "AppStorageUtils",
                    "Directory does not exist or is not a directory: ${dir.absolutePath}"
                )
                emptyList()
            }
        } else {
            Log.e("AppStorageUtils", "External storage is not available.")
            emptyList()
        }
    }

    /**
     * Searches for a file by its name within the specified app-specific directory and executes a callback if found.
     *
     * @param context The application context.
     * @param fileName The name of the file to search for.
     * @param fileType The type of the file (IMAGE, AUDIO, DOCUMENT, VIDEO).
     * @param onFileFound A callback function that takes the found File as a parameter.
     */
//    fun findFileByName(context: Context, fileName: String, fileType: FileType, onFileFound: (File?) -> Unit) {
//        val baseDir: File? = context.getExternalFilesDir(null)
//
//        if (baseDir == null) {
//            Log.e("AppStorageUtils", "External storage is not available.")
//            onFileFound(null)
//            return
//        }
//
//        val dir = File(baseDir, fileType.directoryName)
//        Log.d("AppStorageUtils", "Looking for file in: ${dir.absolutePath}")
//
//        if (dir.exists() && dir.isDirectory) {
//            val files = dir.listFiles()
//            if (files.isNullOrEmpty()) {
//                Log.d("AppStorageUtils", "Directory is empty: ${dir.absolutePath}")
//            } else {
//                Log.d("AppStorageUtils", "Files in directory: ${files.joinToString { it.name }}")
//            }
//
//            val matchingFile = files?.find { it.name == fileName }
//            if (matchingFile != null) {
//                Log.d("AppStorageUtils", "File found: ${matchingFile.absolutePath}")
//                onFileFound(matchingFile)
//            } else {
//                Log.d("AppStorageUtils", "File not found: $fileName in ${fileType.directoryName}")
//                onFileFound(null)
//            }
//        } else {
//            Log.e("AppStorageUtils", "Directory does not exist: ${dir.absolutePath}")
//            onFileFound(null)
//        }
//    }

    fun findFileByName(
        context: Context,
        fileName: String,
        fileType: FileType,
        onFileFound: (File?) -> Unit
    ) {
        Log.d(
            "AppStorageUtils",
            "---- Attempting to find file: $fileName in FileType: ${fileType.name} ----"
        )
        val baseDir: File? = context.getExternalFilesDir(null)

        if (baseDir == null) {
            Log.e(
                "AppStorageUtils",
                "External storage base directory is not available (getExternalFilesDir(null) returned null)."
            )
            onFileFound(null)
            return
        }
        Log.d("AppStorageUtils", "Base external files directory: ${baseDir.absolutePath}")
        Log.d("AppStorageUtils", "Using FileType directoryName: '${fileType.directoryName}'")

        val dir = File(baseDir, fileType.directoryName)
        Log.d("AppStorageUtils", "Constructed directory path to search in: ${dir.absolutePath}")

        if (!dir.exists()) {
            Log.e("AppStorageUtils", "Target directory does NOT exist: ${dir.absolutePath}")
            onFileFound(null)
            return
        }

        if (!dir.isDirectory) {
            Log.e(
                "AppStorageUtils",
                "Target path exists but is NOT a directory: ${dir.absolutePath}"
            )
            onFileFound(null)
            return
        }

        Log.d("AppStorageUtils", "Target directory exists and is a directory: ${dir.absolutePath}")

        val files = dir.listFiles()

        if (files == null) {
            Log.e(
                "AppStorageUtils",
                "listFiles() returned null for directory: ${dir.absolutePath}. Possible I/O error or permission issue with listing directory contents."
            )
            onFileFound(null)
            return
        }

        if (files.isEmpty()) {
            Log.d("AppStorageUtils", "Directory is empty: ${dir.absolutePath}. No files to search.")
            onFileFound(null)
            return
        }

        Log.d(
            "AppStorageUtils",
            "Files found in directory (${files.size} files): ${files.joinToString { "'${it.name}'" }}"
        )

        // Consider using case-insensitive comparison if that's acceptable
        val matchingFile = files.find { it.name.equals(fileName, ignoreCase = true) }
        // If strict case sensitivity is required:
        // val matchingFile = files.find { it.name == fileName }

        if (matchingFile != null) {
            Log.d(
                "AppStorageUtils",
                "File MATCH FOUND: ${matchingFile.absolutePath} (is file: ${matchingFile.isFile}, is directory: ${matchingFile.isDirectory})"
            )
            onFileFound(matchingFile)
        } else {
            Log.d(
                "AppStorageUtils",
                "File NOT found with name '$fileName' (used case-insensitive search) in directory ${dir.absolutePath}"
            )
            // If you used strict case:
            // Log.d("AppStorageUtils", "File NOT found with name '$fileName' (used case-sensitive search) in directory ${dir.absolutePath}")
            onFileFound(null)
        }
        Log.d("AppStorageUtils", "---- Finished attempting to find file ----")
    }

    private fun getMimeTypeFromFileName(filename: String): String? {
        val extension = filename.substringAfterLast(".", "").lowercase()
        val enumMimeType = MimeType.entries.find { it.extension == extension }?.mimeType
        if (enumMimeType != null) return enumMimeType
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun openFileWithChooser(context: Context, filePath: String) {
        val intent = Intent(Intent.ACTION_VIEW)

        val mimeType = getMimeTypeFromFileName(filePath)
        if (mimeType != null) {
            intent.setDataAndType(filePath.toUri(), mimeType)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        } else {
            intent.setDataAndType(filePath.toUri(), "*/*")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(Intent.createChooser(intent, "Choose an app"))
    }

    fun File.size(): Double = this.length().toDouble()

    fun Double.calcFileLength(context: Context): String {
        val (value, unit) = when {
            this > 1_048_576 -> this / 1_048_576 to context.getString(R.string.txt_mb_size)
            this > 1024 -> this / 1024 to context.getString(R.string.txt_kb_size)
            else -> this to context.getString(R.string.txt_b_size)
        }
        return String.format("%.1f", value) + " " + unit
    }

    fun Double.calcFileLength(): String {
        val (value, unit) = when {
            this > 1_048_576 -> this / 1_048_576 to "MB"
            this > 1024 -> this / 1024 to "KB"
            else -> this to "B"
        }
        return String.format("%.1f", value) + " " + unit
    }

    fun fileFromContentUriNew(
        contentResolver: ContentResolver,
        contentUri: Uri,
        fileType: FileType
    ): File? {
        // Retrieve the file name from the content URI
        val fileName = getFileName(contentResolver, contentUri) ?: "unknown_file"

        // Define the base directory

        // Determine the destination folder based on the file type
        val destinationFolder = File(baseDirectory, fileType.directoryName)

        // Create the destination folder if it doesn't exist
        if (!destinationFolder.exists()) {
            if (!destinationFolder.mkdirs()) {
                Log.e(
                    "fileFromContentUriNew",
                    "Failed to create directory: ${destinationFolder.absolutePath}"
                )
                return null
            }
        }

        // Create the destination file
        val destinationFile = File(destinationFolder, fileName)

        // Copy the file from the content URI to the destination file
        return try {
            contentResolver.openInputStream(contentUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    copyStream(inputStream, outputStream)
                }
            }
            destinationFile
        } catch (e: IOException) {
            Log.e("fileFromContentUriNew", "Error copying file: ${e.message}")
            null
        }
    }


    /**
     * Helper function to retrieve the file name from a content URI.
     */
    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow("_display_name"))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }


    /**
     * Helper function to copy data from an InputStream to an OutputStream.
     */
    @Throws(IOException::class)
    private fun copyStream(input: InputStream, output: FileOutputStream) {
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }


    fun fileType(context: Context, uri: Uri): FileType? {
        val mimeType = context.contentResolver.getType(uri)
        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return when (fileExtension) {
            "jpg", "png", "gif" -> FileType.IMAGE
            "mp3", "wav", "ogg" -> FileType.AUDIO
            "pdf", "doc", "docx" -> FileType.DOCUMENT
            "mp4", "avi", "mkv" -> FileType.VIDEO
            else -> null
        }
    }


    fun getRealImagePathFromContentUri(contentResolver: ContentResolver, uri: Uri): String? {
        // Retrieve the file name from the content URI
        val fileName = getFileName(contentResolver, uri) ?: return null

        // Define the destination folder within the app's private external storage
        val destinationFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Teamyar/Teamyar Images"
        )

        // Create the destination folder if it doesn't exist
        if (!destinationFolder.exists()) {
            if (!destinationFolder.mkdirs()) {
                Log.e(
                    "getRealImagePath",
                    "Failed to create directory: ${destinationFolder.absolutePath}"
                )
                return null
            }
        }

        // Create a .nomedia file to prevent media scanning
        val nomediaFile = File(destinationFolder, ".nomedia")
        if (!nomediaFile.exists()) {
            try {
                val created = nomediaFile.createNewFile()
                Log.d("getRealImagePath", ".nomedia file created: $created")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Log.d("getRealImagePath", ".nomedia file already exists")
        }

        // Create the destination file
        val destinationFile = File(destinationFolder, fileName)

        // Copy the image data from the content URI to the destination file
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    fun createTempFile(context: Context, fileType: FileType, customName: String? = null): File? {
        // Define the base directory for the app's external files
        val baseDir = context.getExternalFilesDir(null)
        // Define the specific directory for the given FileType
        val specificDir = File(baseDir, fileType.directoryName)

        // Create the directory if it doesn't exist
        if (!specificDir.exists()) {
            if (!specificDir.mkdirs()) {
                // Log an error message if the directory creation fails
                Log.e("createTempFile", "Failed to create directory: ${specificDir.absolutePath}")
                return null
            }
        }

        // Determine the file name
        val fileName = customName ?: run {
            // Create a timestamp to ensure unique file names
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                Date()
            )
            "TEMP_$timeStamp${fileType.fileExtension}"
        }

        // Create the temporary file
        val tempFile = File(specificDir, fileName)
        return try {
            if (tempFile.createNewFile()) {
                tempFile
            } else {
                Log.e("createTempFile", "File already exists: ${tempFile.absolutePath}")
                null
            }
        } catch (e: IOException) {
            // Log the exception if file creation fails
            Log.e("createTempFile", "Error creating temporary file: ${e.message}")
            null
        }
    }


    private val downloadProgressMap = HashMap<String, MutableLiveData<Int>>()

    fun getProgressLiveData(path: String): MutableLiveData<Int> {
        var liveData = downloadProgressMap[path]
        if (liveData == null) {
            liveData = MutableLiveData()
            downloadProgressMap[path] = liveData
        }
        return liveData
    }

    fun writeResponseBodyToDisk(body: ResponseBody, path: String?): Boolean {
        Log.e("TAG", "writeResponseBodyToDisk: $body, path $path")
        return try {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                var totalDownloaded: Long = 0
                inputStream = body.byteStream()
                outputStream = FileOutputStream(File(path))
                var per = 0
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) break // End of stream reached
                    outputStream.write(fileReader, 0, read)
                    totalDownloaded += read.toLong()

                    val fileSize: Long = if (body.contentLength() > 0) {
                        body.contentLength()
                    } else {
                        // Estimation (adjust multiplier as needed)
                        (totalDownloaded * 1.5).toLong()
                    }

                    val percent = (totalDownloaded * 100 / fileSize).toInt()
                    if (percent != per) {
                        per = percent
                        path?.let { getProgressLiveData(it).postValue(per) }
                    }
                }

                outputStream.flush()

                // File download complete, delay and then set progress to 100%
                CoroutineScope(Job() + Dispatchers.Main).launch {
                    delay(500) // Delay for 500 milliseconds (adjust as needed)
                    path?.let { getProgressLiveData(it).postValue(100) }
                }
                true
            } catch (e: IOException) {
                // Handle exceptions, log errors, etc.
                Log.e("Download Error", "Error writing to disk: ${e.message}")
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            // Handle exceptions, log errors, etc.
            Log.e("Download Error", "Error creating file: ${e.message}")
            false
        }
    }

    fun getFileNameFromPath(filePath: String): String =
        filePath.substring(filePath.lastIndexOf("/") + 1)


    fun createResizedFiles(context: Context, bitmap: Bitmap?): Pair<File, File>? {
        if (bitmap == null) return null

        // Define the directory path within the app's private external storage
        val subDirectory = File(context.getExternalFilesDir(null), "Teamyar/Teamyar Images")

        // Create the directory if it doesn't exist
        if (!subDirectory.exists()) {
            if (!subDirectory.mkdirs()) {
                Log.e(
                    "createResizedFiles",
                    "Failed to create directory: ${subDirectory.absolutePath}"
                )
                return null
            }
        }

        // Generate unique file names based on the current timestamp
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file60x60 = File(subDirectory, "resize_$timeStamp.jpg")
        val file200x200 =
            File(subDirectory, "${imageNameWithDirection()}_image_200x200_$timeStamp.jpg")

        // Create resized bitmaps
        val bitmap60x60 = Bitmap.createScaledBitmap(bitmap, 60, 60, false)
        val bitmap200x200 = Bitmap.createScaledBitmap(bitmap, 200, 200, false)

        // Save the resized bitmaps to the respective files
        try {
            FileOutputStream(file60x60).use { outputStream ->
                bitmap60x60.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            FileOutputStream(file200x200).use { outputStream ->
                bitmap200x200.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        } catch (e: IOException) {
            Log.e("createResizedFiles", "Error saving resized images: ${e.message}")
            return null
        }

        return Pair(file60x60, file200x200)
    }

    private fun imageNameWithDirection() = "JPEG_${
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
            Date()
        )
    }"

    fun deleteAllFilesInRoot(context: Context) {
        val rootDir = File(context.getExternalFilesDir(null), "Teamyar")
        if (rootDir.exists() && rootDir.isDirectory) {
            rootDir.deleteRecursively()
            Log.d(
                "deleteAllFilesInRoot",
                "All files and directories deleted in: ${rootDir.absolutePath}"
            )
        } else Log.d("deleteAllFilesInRoot", "Directory does not exist: ${rootDir.absolutePath}")
    }

    private fun getFileExtension2(context: Context, uri: Uri): String? {
        val fileType: String? = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
    }

    fun getFileExtension(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }

    fun getAudioFilePath(context: Context, chatId: String, onFileFound: (File) -> Unit) {
        // Define the directory path within the app's private external storage
        val audioDir = File(context.getExternalFilesDir(null), "Teamyar Audio")

        // Create the directory if it doesn't exist
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        // Generate a timestamp for unique filename
        val timeStamp: String =
            SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

        // Create the audio file with the format: audio_<timestamp>__<chatId>.mp3
        val audioFileName = "audio_${timeStamp}__${chatId}.mp3"

        // Return the File object representing the audio file path3
        onFileFound.invoke(File(audioDir, audioFileName))
    }

    fun File.deleteRecursivelySafe(): Boolean = try {
        if (isDirectory) deleteRecursively() else delete()
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    fun getTeamyarDirectory(context: Context): File {
        // Retrieve the app-specific external files directory
        val appExternalDir = context.getExternalFilesDir(null)
        val teamyarDir = File(appExternalDir, "Teamyar")
        if (!teamyarDir.exists()) teamyarDir.mkdirs()
        return teamyarDir
    }
}
