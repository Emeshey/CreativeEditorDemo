package com.media.nyzzu

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.imageeditor.ImageAdapter
import com.media.nyzzu.databinding.FragmentImageSelectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectionFragment : Fragment() {

    private lateinit var binding: FragmentImageSelectionBinding
    private lateinit var adapter: ImageAdapter
    private val selectedImages = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadImages()

        binding.continueButton.setOnClickListener {
            if (selectedImages.isEmpty()) {
                Toast.makeText(requireContext(), R.string.select_at_least_one, Toast.LENGTH_SHORT)
                    .show()
            } else {
                navigateToEditor()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ImageAdapter { uri, isSelected ->
            if (isSelected) {
                if (!selectedImages.contains(uri.toString())) {
                    selectedImages.add(uri.toString())
                }
            } else {
                selectedImages.remove(uri.toString())
            }
            updateContinueButtonState()
        }

        binding.imagesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
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

        requireActivity().contentResolver.query(
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
        findNavController().navigate(
            R.id.editorPreviewFragment,
            bundleOf(
                URI to selectedImages.toTypedArray(),
                INDEX to 0
            )
        )
    }
}