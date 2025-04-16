package com.media.nyzzu

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2

const val URI = "uri"
const val INDEX = "index"

class EditorPreviewFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var viewImageButton: Button

    private val args by navArgs<EditorPreviewFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        viewImageButton = view.findViewById(R.id.viewImageButton)

        val images = args.uri

        viewPager.apply {
            adapter = ImagePagerAdapter(images.toList())
            setCurrentItem(args.index, false)
        }

        viewImageButton.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(
                R.id.editorFragment,
                bundleOf(
                    URI to images,
                    INDEX to viewPager.currentItem
                )
            )
        }
    }
}