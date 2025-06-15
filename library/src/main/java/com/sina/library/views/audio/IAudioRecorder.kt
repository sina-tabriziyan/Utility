package com.sina.library.views.audio

import java.io.File

interface IAudioRecorder {
    fun start(outputFile: File, fileName: String): String
    fun stop()
}