package com.media.nyzzu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.imageeditor.ImageAdapter
import com.media.nyzzu.databinding.ActivityImageSelectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageSelectionBinding
    private lateinit var adapter: ImageAdapter
    private val selectedImages = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadImages()

        binding.continueButton.setOnClickListener {
            if (selectedImages.isEmpty()) {
                Toast.makeText(this, R.string.select_at_least_one, Toast.LENGTH_SHORT).show()
            } else {
                navigateToEditor()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ImageAdapter { uri, isSelected ->
            if (isSelected) {
                selectedImages.add(uri)
            } else {
                selectedImages.remove(uri)
            }
            updateContinueButtonState()
        }

        binding.imagesRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.imagesRecyclerView.adapter = adapter
    }

    private fun updateContinueButtonState() {
        binding.continueButton.isEnabled = selectedImages.isNotEmpty()
    }

    private fun loadImages() {
        lifecycleScope.launch {
            val images = withContext(Dispatchers.IO) {
                fetchImagesFromGallery()
            }

            if (images.isEmpty()) {
                binding.emptyStateTextView.visibility = View.VISIBLE
                binding.imagesRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateTextView.visibility = View.GONE
                binding.imagesRecyclerView.visibility = View.VISIBLE
                adapter.submitList(images)
            }
        }
    }

    private fun fetchImagesFromGallery(): List<Uri> {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                images.add(contentUri)
            }
        }

        return images
    }

    private fun navigateToEditor() {
        val intent = Intent(this, EditorActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_SELECTED_IMAGES, ArrayList(selectedImages))
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_SELECTED_IMAGES = "extra_selected_images"
    }
}