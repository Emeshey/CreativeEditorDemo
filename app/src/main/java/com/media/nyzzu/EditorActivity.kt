package com.media.nyzzu

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.media.nyzzu.databinding.ActivityEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.DesignEditor
import ly.img.editor.EditorConfiguration
import ly.img.editor.EngineConfiguration
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.library.data.TextAssetSource
import ly.img.editor.core.library.data.TypefaceProvider
import ly.img.editor.rememberForDesign
import ly.img.engine.ContentFillMode
import ly.img.engine.DefaultAssetSource
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.ShapeType
import ly.img.engine.addDefaultAssetSources
import ly.img.engine.addDemoAssetSources

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var selectedImages: ArrayList<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get selected images from intent
        selectedImages =
            intent.getParcelableArrayListExtra(ImageSelectionActivity.EXTRA_SELECTED_IMAGES)
                ?: arrayListOf()

        if (selectedImages.isNotEmpty()) {
            initializeCreativeEditor(selectedImages)
        } else {
            finish()
        }
    }

    @OptIn(UnstableEditorApi::class)
    private fun initializeCreativeEditor(selectedImages: ArrayList<Uri>) {
        // This is where you would initialize the CreativeEditor SDK
        // The implementation depends on the specific SDK you're using

        binding.composeView.setContent {
            val engineConfiguration =
                EngineConfiguration.Companion.remember(
                    license = "your_license_key",
                    onCreate = {
                        setupEditor(this.editorContext.engine, selectedImages.toList())
                    },
                )
            val editorConfiguration = EditorConfiguration.rememberForDesign()
            DesignEditor(
                engineConfiguration = engineConfiguration,
                editorConfiguration = editorConfiguration,
            ) {
                // You can set result here
                supportFragmentManager.popBackStack()
            }
        }

    }

    private suspend fun setupEditor(engine: Engine, photosData: List<Uri>) {
        val firstUri = photosData.first()

        withContext(Dispatchers.Main) {
            engine.scene.load(firstUri)
            println("Scene created successfully from first image.")
        }

        val pages = engine.scene.getPages()
        val firstPage = pages.first()

        println("Total pages in scene: ${pages.size}")

        val parent = engine.block.getParent(firstPage)

        suspend fun addPageWithImage(url: Uri) {
            try {
                withContext(Dispatchers.Main) {
                    val newPage = engine.block.create(DesignBlockType.Page)
                    engine.block.setWidth(newPage, engine.block.getWidth(firstPage))
                    engine.block.setHeight(newPage, engine.block.getHeight(firstPage))
                    parent?.let { engine.block.appendChild(it, newPage) }

                    val graphicBlock = engine.block.create(DesignBlockType.Graphic)
                    val imageFill = engine.block.createFill(FillType.Image)
                    engine.block.setUri(imageFill, "fill/image/imageFileURI", url)
                    engine.block.setFill(graphicBlock, imageFill)
                    engine.block.setShape(graphicBlock, engine.block.createShape(ShapeType.Rect))
                    engine.block.setWidth(graphicBlock, engine.block.getWidth(newPage))
                    engine.block.setHeight(graphicBlock, engine.block.getHeight(newPage))
                    engine.block.appendChild(newPage, graphicBlock)
                }
            } catch (e: Exception) {
                println("Error adding new page: ${e.localizedMessage}")
            }
        }

        for (url in photosData.drop(1)) {
            addPageWithImage(url)
        }

        for (graphicBlock in engine.block.findByType(DesignBlockType.Graphic)) {
            engine.block.setContentFillMode(graphicBlock, ContentFillMode.CONTAIN)
        }

        withContext(Dispatchers.Main) {
            engine.addDefaultAssetSources()
            engine.addDemoAssetSources(
                sceneMode = engine.scene.getMode(),
                withUploadAssetSources = true
            )
            TypefaceProvider().provideTypeface(engine, DefaultAssetSource.TYPEFACE.name)?.let {
                engine.asset.addSource(
                    TextAssetSource(
                        engine = engine,
                        typeface = it
                    )
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}