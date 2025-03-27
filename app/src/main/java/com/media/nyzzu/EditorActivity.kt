package com.media.nyzzu

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.media.nyzzu.databinding.ActivityEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.DesignEditor
import ly.img.editor.EditorConfiguration
import ly.img.editor.EditorDefaults
import ly.img.editor.EngineConfiguration
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.event.EditorEventHandler
import ly.img.editor.rememberForDesign
import ly.img.engine.ContentFillMode
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.MimeType
import ly.img.engine.ShapeType
import java.io.File
import java.nio.ByteBuffer

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
                        setupEditor(
                            this.editorContext.engine,
                            selectedImages.toList(),
                            this.editorContext.eventHandler
                        )
                    },
                    onExport = {
                        saveData(export(this.editorContext.engine))
                    }
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

    @OptIn(UnstableEditorApi::class)
    private suspend fun setupEditor(
        engine: Engine,
        photosData: List<Uri>,
        editorEventHandler: EditorEventHandler
    ) {
        EditorDefaults.onCreate(
            engine = engine,
            eventHandler = editorEventHandler,
            sceneUri = EngineConfiguration.defaultDesignSceneUri,
        )

        val pages = engine.scene.getPages()
        val firstPage = pages.first()

        val parent = engine.block.getParent(firstPage)

        suspend fun addPageWithImage(uri: Uri, useFirstPage: Boolean) {
            try {
                withContext(Dispatchers.Main) {
                    val targetPage = if (useFirstPage) {
                        firstPage
                    } else {
                        val newPage = engine.block.create(DesignBlockType.Page)
                        engine.block.setWidth(newPage, engine.block.getWidth(firstPage))
                        engine.block.setHeight(newPage, engine.block.getHeight(firstPage))
                        parent?.let { engine.block.appendChild(it, newPage) }
                        newPage
                    }
                    val graphicBlock = engine.block.create(DesignBlockType.Graphic)
                    val imageFill = engine.block.createFill(FillType.Image)
                    engine.block.apply {
                        setUri(imageFill, "fill/image/imageFileURI", uri)
                        setFill(graphicBlock, imageFill)
                        setShape(graphicBlock, engine.block.createShape(ShapeType.Rect))
                        setWidth(graphicBlock, engine.block.getWidth(targetPage))
                        setHeight(graphicBlock, engine.block.getHeight(targetPage))
                        appendChild(targetPage, graphicBlock)
                    }
                }
            } catch (e: Exception) {
                println("Error adding new page: ${e.localizedMessage}")
            }
        }

        photosData.forEachIndexed { index, uri ->
            addPageWithImage(uri, index == 0)
        }

        for (graphicBlock in engine.block.findByType(DesignBlockType.Graphic)) {
            engine.block.setContentFillMode(graphicBlock, ContentFillMode.CONTAIN)
        }
    }

    private suspend fun export(engine: Engine): List<ByteBuffer> {
        val pages = engine.scene.getPages()
        val exportedMedias = mutableListOf<ByteBuffer>()

        for (page in pages) {
            val imageData = withContext(Dispatchers.Main) {
                engine.block.export(block = page, mimeType = MimeType.JPEG)
            }
            exportedMedias.add(imageData)
        }

        return exportedMedias
    }

    private fun saveData(byteBuffers: List<ByteBuffer>) {
        val byteArrays = byteBuffers.map { byteBuffer ->
            ByteArray(byteBuffer.remaining()).apply { byteBuffer.get(this) }
        } as ArrayList<ByteArray>
        byteArrays
            .take(10)
            .map { saveImageToCache(this, it).toString() }
            .toTypedArray()
    }

    private fun saveImageToCache(context: Context, byteArray: ByteArray): Uri? {
        return try {
            val file = File(context.cacheDir, "my_image${System.currentTimeMillis()}.jpg")
            file.outputStream().use { it.write(byteArray) }
            FileProvider.getUriForFile(context, "${context.packageName}.customFileProvider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}