/**
 * Created by ST on 2/1/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library

import android.app.Dialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.sina.library.utility.databinding.DialogCircularCameraBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files.exists

class CircularCameraDialog(
    private val outputFilePath: String? = null,
    private val onVideoRecorded: (filePath: String) -> Unit
) : DialogFragment() {
    private var _binding: DialogCircularCameraBinding? = null
    private val binding get() = _binding!!
    private var recording: Recording? = null
    private var videoFilePath: String? = null
    private var isRecording = false
    private var videoCapture: VideoCapture<Recorder>? = null
    val maxProgress = 100
    val updateInterval = 100L // Update every 100ms
    private val maxDurationMillis: Long = 20_000 // 60 seconds

    val steps = (maxDurationMillis / updateInterval).toInt()
    private var recordingJob: Job? = null // Coroutine job to handle max recording duration


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogCircularCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(300, 300)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        setupCameraPreview { startRecordingWithTimer() }
        dialog?.setOnDismissListener { stopRecording() }
    }

    private fun setupCameraPreview(onReady: () -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        binding.circularFrame.setMaxProgress(maxProgress) // Set the maximum progress
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            preview.surfaceProvider = binding.cameraPreview.surfaceProvider
            this.videoCapture = VideoCapture.withOutput(
                Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.SD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.LOWEST)
                        )
                    )
                    .build()
            )
            videoFilePath = getOutputFilePath()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                onReady() // Notify that camera setup is complete
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun getOutputFilePath(): String {
        // 1. Get app-specific external storage directory
        val baseDir = requireContext().getExternalFilesDir(null)
            ?: throw IllegalStateException("External storage not available")

        // 2. Determine the final directory
        val finalDir = if (outputFilePath != null) {
            File(baseDir, outputFilePath).apply {
                if (!exists()) mkdirs()
            }
        } else {
            // Default location if no path provided
            val appName =
                requireContext().applicationInfo.loadLabel(requireContext().packageManager)
                    .toString()
            File(baseDir, "$appName/Video").apply {
                if (!exists()) mkdirs()
            }
        }

        // 3. Generate unique filename
        return File(finalDir, "video_${System.currentTimeMillis()}.mp4").absolutePath
    }

    private fun startRecordingWithTimer() {
        if (isRecording) return
        isRecording = true

        recording = videoCapture?.output?.prepareRecording(
            requireContext(),
            FileOutputOptions.Builder(File(videoFilePath!!)).build()
        )
            ?.start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    onVideoRecorded(videoFilePath!!)
                    dismiss()
                }
            }
        binding.circularFrame.setMaxProgress(maxProgress)
        // Launch coroutine to update progress over time
        recordingJob = CoroutineScope(Dispatchers.Main).launch {
            for (i in 1..steps) {
                delay(updateInterval)
                val progress = (i.toFloat() / steps * maxProgress).toInt()
                binding.circularFrame.setProgressAnimated(progress) // Update progress
            }
            stopRecording() // Automatically stop recording after max duration
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            isRecording = false
            recording?.stop()
            recording = null

            // Cancel the coroutine if it's still running
            recordingJob?.cancel()
            recordingJob = null
        }
    }

    override fun onDestroyView() {
        stopRecording()
        super.onDestroyView()
        _binding = null
    }
}
