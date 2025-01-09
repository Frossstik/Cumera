package com.example.cumera.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.cumera.databinding.FragmentPhotoBinding
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PhotoFragment : Fragment() {
    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imageCaptureExecutor: ExecutorService

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
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imageCaptureExecutor = Executors.newSingleThreadExecutor()

        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        binding.captureButton.setOnClickListener {
            takePhoto()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                animateFlash()
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
        imageCaptureExecutor.shutdown()
        _binding = null
    }

    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.d("TAG", "Use case binding failed: $e")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        imageCapture?.let {
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                .format(System.currentTimeMillis())

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Cumera")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            it.takePicture(
                outputOptions,
                imageCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val msg = "Photo saved: ${output.savedUri}"
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка при сохранении фотографии",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("TAG", "Error taking photo: $exception")
                    }
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }
}
