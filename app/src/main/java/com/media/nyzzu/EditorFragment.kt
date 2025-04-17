package com.media.nyzzu

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.EditorConfiguration
import ly.img.editor.EditorDefaults
import ly.img.editor.EditorUiMode
import ly.img.editor.EngineConfiguration
import ly.img.editor.PhotoEditor
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.rememberForPhoto
import ly.img.engine.Engine
import ly.img.engine.MimeType
import java.io.File
import java.nio.ByteBuffer

class EditorFragment : Fragment() {

    private val args by navArgs<EditorFragmentArgs>()

    @OptIn(UnstableEditorApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val engineConfiguration =
                    EngineConfiguration.Companion.remember(
                        license = "your_license_key",
                        onCreate = {
                            EditorDefaults.onCreateFromImage(
                                engine = this.editorContext.engine,
                                imageUri = args.uri[args.index].toUri(),
                                eventHandler = this.editorContext.eventHandler,
                                size = null
                            )
                        },
                        onExport = {
                            saveData(export(this.editorContext.engine))
                        }
                    )
                PhotoEditor(
                    engineConfiguration = engineConfiguration,
                    editorConfiguration = EditorConfiguration.rememberForPhoto(
                        uiMode = EditorUiMode.DARK
                    ),
                ) {
                    navigateBack(args.uri)
                }
            }
        }
    }

    private suspend fun export(engine: Engine): ByteBuffer {
        val page = engine.scene.getPages().first()
        return withContext(Dispatchers.Main) {
            engine.block.export(block = page, mimeType = MimeType.JPEG)
        }
    }

    private fun saveData(byteBuffer: ByteBuffer) {
        val byteArray =
            ByteArray(byteBuffer.remaining()).apply {
                byteBuffer.get(this)
            }
        val uri = saveImageToCache(requireContext(), byteArray).toString()

        val uris = args.uri.toMutableList()
        uris[args.index] = uri

        // send the new uris back
        navigateBack(uris.toTypedArray())
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

    private fun navigateBack(uris: Array<String>) {
        findNavController().popBackStack()
        findNavController().navigate(
            R.id.editorPreviewFragment,
            bundleOf(
                URI to uris,
                INDEX to args.index
            )
        )
    }
}
