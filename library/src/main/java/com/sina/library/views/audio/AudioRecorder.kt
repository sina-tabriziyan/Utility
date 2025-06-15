/**
 * Created by ST on 6/7/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */

package com.sina.library.views.audio

import android.media.MediaRecorder
import android.media.MediaRecorder.AudioSource
import java.io.File
import java.io.IOException

class AudioRecorder : IAudioRecorder {
    private var recorder: MediaRecorder? = null

    override fun start(outputFile: File, fileName: String): String {
        var filePath = ""
        if (recorder == null) {
            try {
                recorder = MediaRecorder()
                recorder?.apply {
                    if (!outputFile.exists()) outputFile.mkdir()
                    filePath = outputFile.absolutePath + fileName
                    setAudioSource(AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncodingBitRate(8000)
                    setOutputFile(filePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    try {
                        recorder?.prepare()
                        recorder?.start()
                    } catch (e: IOException) {
                        e.printStackTrace();
                        recorder = null
                    } catch (e: IllegalStateException) {
                        e.printStackTrace();
                        recorder = null

                    }
                }
            } catch (e: RuntimeException) {
                e.printStackTrace();
            }
        }
        return filePath
    }

    override fun stop() {
        if (recorder != null)
            try {
                recorder?.stop()
            } catch (e: RuntimeException) {

            } finally {
                recorder?.release()
            }
        recorder = null
    }
}