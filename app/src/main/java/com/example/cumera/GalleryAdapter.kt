package com.example.cumera

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.cumera.databinding.ItemGalleryBinding
import java.io.File

class GalleryAdapter(
    private val files: Array<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    inner class GalleryViewHolder(val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val file = files[position]

        val lastModified = file.lastModified()
        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastModified))

        holder.binding.mediaDate.text = date

        if (file.extension == "mp4") {
            val thumbnail: Bitmap? = getVideoThumbnail(file)
            if (thumbnail != null) {
                holder.binding.imageView.setImageBitmap(thumbnail)
            } else {
                holder.binding.imageView.setImageResource(R.drawable.ic_video_placeholder)
            }
            holder.binding.playButton.visibility = View.VISIBLE
        } else {
            holder.binding.imageView.setImageURI(file.toUri())
            holder.binding.playButton.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount() = files.size

    private fun getVideoThumbnail(file: File): Bitmap? {
        return try {
            ThumbnailUtils.createVideoThumbnail(file.toString(), MediaStore.Images.Thumbnails.MINI_KIND)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


