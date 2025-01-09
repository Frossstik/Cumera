package com.example.cumera.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Chronometer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cumera.databinding.FragmentVideoBinding
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoFragment : Fragment() {
    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private lateinit var videoCaptureExecutor: ExecutorService

    @RequiresApi(Build.VERSION_CODES.M)
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            } else {
                Snackbar.make(
                    binding.root,
                    "The camera permission is necessary",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        videoCaptureExecutor = Executors.newSingleThreadExecutor()

        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        binding.recordButton.setOnClickListener {
            if (activeRecording != null) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.switchButton.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoCaptureExecutor.shutdown()
        _binding = null
    }

    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, videoCapture)
            } catch (e: Exception) {
                Log.d("TAG", "Use case binding failed: $e")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startRecording() {
        // Показываем Chronometer, когда начинается запись
        binding.chronometer.visibility = View.VISIBLE
        binding.chronometer.base = SystemClock.elapsedRealtime()  // Сброс времени до начала записи
        binding.chronometer.start()

        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Cumera")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        videoCapture?.output?.prepareRecording(requireContext(), mediaStoreOutputOptions)
            ?.start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        binding.recordButton.setColorFilter(Color.RED)
                        Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        binding.recordButton.setColorFilter(Color.WHITE)
                        binding.chronometer.stop()

                        val savedUri = event.outputResults.outputUri
                        val msg = "Video saved: $savedUri"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

                        // Прячем Chronometer после завершения записи
                        binding.chronometer.visibility = View.GONE

                        if (event.hasError()) {
                            Toast.makeText(requireContext(), "Error recording video", Toast.LENGTH_LONG).show()
                        }
                        activeRecording = null
                    }
                }
            }?.also {
                activeRecording = it
            }
    }



    private fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }
}
