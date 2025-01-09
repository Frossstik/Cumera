package com.example.cumera.ui

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cumera.GalleryAdapter
import com.example.cumera.databinding.FragmentGalleryBinding
import java.io.File

class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private var currentMediaFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Используем getExternalStoragePublicDirectory для получения директории Pictures и Movies
        val picturesDirectory = File(Environment.getExternalStorageDirectory(), "Pictures/Cumera")
        val picturesFiles = picturesDirectory.listFiles()?.reversedArray() ?: emptyArray()

        val moviesDirectory = File(Environment.getExternalStorageDirectory(), "Movies/Cumera")
        val movieFiles = moviesDirectory.listFiles()?.reversedArray() ?: emptyArray()

        val allFiles = picturesFiles + movieFiles

        val adapter = GalleryAdapter(allFiles) { file ->
            showFullscreenMedia(file)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        binding.closeButton.setOnClickListener { hideFullscreenMedia() }
        binding.deleteButton.setOnClickListener { deleteCurrentMedia() }
    }

    private fun showFullscreenMedia(file: File) {
        currentMediaFile = file
        val fileUri = file.toUri()

        if (file.extension in listOf("jpg", "jpeg", "png", "gif")) {
            binding.fullscreenImageView.setImageURI(fileUri)
            binding.fullscreenImageView.visibility = View.VISIBLE
            binding.fullscreenVideoView.visibility = View.GONE
        } else if (file.extension == "mp4") {
            binding.fullscreenVideoView.setVideoURI(fileUri)
            binding.fullscreenVideoView.visibility = View.VISIBLE
            binding.fullscreenImageView.visibility = View.GONE
            binding.fullscreenVideoView.start()
        }

        binding.recyclerView.visibility = View.GONE
        binding.fullscreenContainer.visibility = View.VISIBLE
    }

    private fun hideFullscreenMedia() {
        currentMediaFile = null
        binding.recyclerView.visibility = View.VISIBLE
        binding.fullscreenContainer.visibility = View.GONE
        binding.fullscreenImageView.visibility = View.GONE
        binding.fullscreenVideoView.visibility = View.GONE
    }

    private fun deleteCurrentMedia() {
        currentMediaFile?.let { file ->
            if (file.delete()) {
                Toast.makeText(requireContext(), "Удалено", Toast.LENGTH_SHORT).show()
                refreshGallery()
                hideFullscreenMedia()
            } else {
                Toast.makeText(requireContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshGallery() {
        // Чтение файлов из директории Pictures/Cumera
        val picturesDirectory = File(Environment.getExternalStorageDirectory(), "Pictures/Cumera")
        val picturesFiles = picturesDirectory.listFiles()?.reversedArray() ?: emptyArray()

        // Чтение файлов из директории Movies/Cumera
        val moviesDirectory = File(Environment.getExternalStorageDirectory(), "Movies/Cumera")
        val movieFiles = moviesDirectory.listFiles()?.reversedArray() ?: emptyArray()

        val allFiles = picturesFiles + movieFiles

        val adapter = GalleryAdapter(allFiles) { file ->
            showFullscreenMedia(file)
        }
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



