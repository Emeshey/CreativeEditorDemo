package com.example.imageeditor

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.media.nyzzu.databinding.ItemImageBinding

class ImageAdapter(
    private val onImageSelected: (Uri, Boolean) -> Unit
) : ListAdapter<Uri, ImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(
        private val binding: ItemImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            Glide.with(binding.root)
                .load(uri)
                .centerCrop()
                .into(binding.imageView)

            binding.root.setOnClickListener {
                binding.checkBox.isChecked = !binding.checkBox.isChecked
                onImageSelected(uri, binding.checkBox.isChecked)
            }

            binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
                onImageSelected(uri, isChecked)
            }

            binding.checkBox.isChecked = false
        }
    }

    private class ImageDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
    }
}