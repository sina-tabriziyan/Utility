package com.sina.library.data.enums

enum class FileType(val directoryName: String, val fileExtension: String) {
    IMAGE("Teamyar/Teamyar Images", ".jpg"),
    VIDEO("Teamyar/Teamyar Videos", ".mp4"),
    DOCUMENT("Teamyar/Teamyar Documents", ".pdf"),
    AUDIO("Teamyar/Teamyar Audio", ".mp3")
}
