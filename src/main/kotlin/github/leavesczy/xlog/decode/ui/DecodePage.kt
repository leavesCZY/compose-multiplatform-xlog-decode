package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose_multiplatform_xlog_decode.generated.resources.Res
import compose_multiplatform_xlog_decode.generated.resources.click_to_select_the_log_file_or_drag_the_log_file_here
import compose_multiplatform_xlog_decode.generated.resources.file_status_summary
import compose_multiplatform_xlog_decode.generated.resources.if_the_log_is_encrypted_the_private_key_needs_to_be_entered
import compose_multiplatform_xlog_decode.generated.resources.open_the_file
import compose_multiplatform_xlog_decode.generated.resources.parse_the_file
import compose_multiplatform_xlog_decode.generated.resources.please_select_the_log_file_first
import github.leavesczy.xlog.decode.logic.DecodePageViewState
import github.leavesczy.xlog.decode.logic.RuntimeLogEntry
import github.leavesczy.xlog.decode.platform.DesktopOs
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.io.path.toPath

@Composable
fun DecodePage(
    pageViewState: DecodePageViewState,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 24.dp)
    ) {
        LogFileSelector(
            coroutineScope = coroutineScope,
            selectedLogFiles = pageViewState.selectedLogFiles,
            onLogFilesConfirmed = pageViewState.onLogFilesSelected
        )
        PrivateKeyField(
            privateKey = pageViewState.privateKey,
            onPrivateKeyChanged = pageViewState.onPrivateKeyChanged
        )
        Button(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.4f)
                .height(height = 45.dp),
            onClick = {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val selectedLogFiles = pageViewState.selectedLogFiles
                    if (selectedLogFiles.isEmpty()) {
                        snackbarHostState.showSnackbar(
                            message = getString(resource = Res.string.please_select_the_log_file_first),
                            duration = SnackbarDuration.Short
                        )
                        return@launch
                    }
                    val outputFiles = pageViewState.onDecodeLogs()
                    val successCount = outputFiles.size
                    val failedCount = selectedLogFiles.size - successCount
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = getString(
                            resource = Res.string.file_status_summary,
                            selectedLogFiles.size,
                            successCount,
                            failedCount
                        ),
                        actionLabel = if (outputFiles.isEmpty()) {
                            null
                        } else {
                            getString(resource = Res.string.open_the_file)
                        },
                        withDismissAction = true
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed && outputFiles.isNotEmpty()) {
                        pageViewState.onOpenFiles(outputFiles)
                    }
                }
            }
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(resource = Res.string.parse_the_file)
            )
        }
        RuntimeLogList(
            logs = pageViewState.runtimeLogs,
            lazyListState = pageViewState.lazyListState
        )
    }
}

@Composable
private fun LogFileSelector(
    coroutineScope: CoroutineScope,
    selectedLogFiles: List<String>,
    onLogFilesConfirmed: (List<String>) -> Unit
) {
    val displayPath = remember(key1 = selectedLogFiles) {
        selectedLogFiles.joinToString(separator = "\n", limit = 5)
    }
    val dropTarget = remember(onLogFilesConfirmed) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val dragData = event.dragData()
                if (dragData !is DragData.FilesList) {
                    return false
                }
                val paths = dragData.readFiles().mapNotNull { uriText ->
                    uriText.toXlogPathOrNull()?.pathString
                }
                if (paths.isNotEmpty()) {
                    onLogFilesConfirmed(paths)
                }
                return paths.isNotEmpty()
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { startEvent ->
                        val dragData = startEvent.dragData()
                        dragData is DragData.FilesList && dragData.readFiles().any { uriText ->
                            uriText.toXlogPathOrNull() != null
                        }
                    },
                    target = dropTarget
                ),
            value = displayPath,
            readOnly = true,
            shape = RoundedCornerShape(size = 18.dp),
            label = {
                Text(
                    text = stringResource(resource = Res.string.click_to_select_the_log_file_or_drag_the_log_file_here),
                    maxLines = 1
                )
            },
            onValueChange = {}
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape = RoundedCornerShape(size = 18.dp))
                .clickable(onClick = {
                    coroutineScope.launch {
                        val files = FileKit.openFilePicker(
                            type = xlogFilePickerType(),
                            mode = FileKitMode.Multiple()
                        )
                        val paths = files?.mapNotNull { platformFile ->
                            val file = platformFile.file
                            if (file.isXlogFile()) {
                                file.path
                            } else {
                                null
                            }
                        }.orEmpty()
                        if (paths.isNotEmpty()) {
                            onLogFilesConfirmed(paths)
                        }
                    }
                })
        )
    }
}

@Composable
private fun PrivateKeyField(
    privateKey: String,
    onPrivateKeyChanged: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth(),
        value = privateKey,
        shape = RoundedCornerShape(size = 18.dp),
        label = {
            Text(
                modifier = Modifier,
                text = stringResource(resource = Res.string.if_the_log_is_encrypted_the_private_key_needs_to_be_entered)
            )
        },
        onValueChange = onPrivateKeyChanged
    )
}

@Composable
private fun RuntimeLogList(
    logs: List<RuntimeLogEntry>,
    lazyListState: LazyListState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        SelectionContainer(
            modifier = Modifier
                .align(alignment = Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                state = lazyListState,
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(
                    space = 10.dp,
                    alignment = Alignment.Top
                ),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            ) {
                itemsIndexed(
                    items = logs,
                    key = { _, entry ->
                        entry.id
                    },
                    contentType = { _, _ ->
                        "RuntimeLog"
                    }
                ) { _, entry ->
                    RuntimeLogItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        log = entry.message
                    )
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .fillMaxHeight(),
            style = ScrollbarStyle(
                minimalHeight = 22.dp,
                thickness = 10.dp,
                shape = RoundedCornerShape(size = 4.dp),
                unhoverColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                hoverColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 1f),
                hoverDurationMillis = 300
            ),
            adapter = rememberScrollbarAdapter(scrollState = lazyListState)
        )
    }
}

@Composable
private fun RuntimeLogItem(
    modifier: Modifier,
    log: String
) {
    Text(
        modifier = modifier,
        text = log,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Start
    )
}

private const val XLOG_FILE_EXTENSION = "xlog"

private fun xlogFilePickerType(): FileKitType {
    return if (DesktopOs.isMacOs) {
        // macOS NSOpenPanel rejects non-system extensions like "xlog" when used as allowedFileTypes.
        FileKitType.File()
    } else {
        FileKitType.File(extension = XLOG_FILE_EXTENSION)
    }
}

private fun String.toXlogPathOrNull(): Path? {
    return try {
        val path = URI(this).toPath()
        if (path.toFile().isXlogFile()) {
            path
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun File.isXlogFile(): Boolean {
    return exists() && isFile && extension.equals(other = XLOG_FILE_EXTENSION, ignoreCase = true)
}
