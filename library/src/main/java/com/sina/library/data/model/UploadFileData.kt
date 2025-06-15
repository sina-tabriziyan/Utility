package com.sina.library.data.model

data class UploadFileData(
    val isProfile: Boolean,
    val urlTo: String,
    val cookie: String,
    val params: Map<String, String>,
    val filepath: String?,
    val fileField: String,
    val fileMimeType: String,
    val userAgent: String
)
