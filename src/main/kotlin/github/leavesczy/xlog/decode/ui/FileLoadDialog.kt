package github.leavesczy.xlog.decode.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.FrameWindowScope
import kotlinx.coroutines.CompletableDeferred
import java.awt.FileDialog
import java.io.File
import java.nio.file.Path

@Composable
fun FrameWindowScope.FileLoadDialog(
    title: String = "Choose a file",
    isMultipleMode: Boolean,
    fileExtension: String?,
    onResult: (result: Path?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(window, title, LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    if (file != null) {
                        onResult(File(directory).resolve(file).toPath())
                    } else {
                        onResult(null)
                    }
                }
            }
        }
    },
    update = {
        it.title = title
        it.isMultipleMode = isMultipleMode
        it.setFilenameFilter { _, name ->
            if (fileExtension.isNullOrBlank()) {
                true
            } else {
                name.endsWith(suffix = fileExtension)
            }
        }
    },
    dispose = FileDialog::dispose
)

class DialogState<T> {

    private var onResult: CompletableDeferred<T>? by mutableStateOf(null)

    val isAwaiting get() = onResult != null

    suspend fun awaitResult(): T {
        onResult = CompletableDeferred()
        val result = onResult!!.await()
        onResult = null
        return result
    }

    fun onResult(result: T) = onResult!!.complete(result)

}