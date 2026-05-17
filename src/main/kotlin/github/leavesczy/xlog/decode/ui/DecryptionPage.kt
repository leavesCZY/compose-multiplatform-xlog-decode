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
import androidx.compose.foundation.lazy.items
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
import github.leavesczy.xlog.decode.logic.DecryptionPageViewState
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

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:15
 * @Desc:
 */
@Composable
fun DecryptionPage(
    pageViewState: DecryptionPageViewState,
    snackBarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 24.dp)
    ) {
        LogFilePath(
            coroutineScope = coroutineScope,
            selectedLogFiles = pageViewState.selectedLogFiles,
            confirmLogFiles = {
                pageViewState.onLogFileIsSelected(it)
            }
        )
        PrivateKey(
            privateKey = pageViewState.privateKey,
            onInputPrivateKey = pageViewState.onInputPrivateKey
        )
        Button(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.4f)
                .height(height = 45.dp),
            onClick = {
                coroutineScope.launch {
                    snackBarHostState.currentSnackbarData?.dismiss()
                    val selectedLogFiles = pageViewState.selectedLogFiles
                    if (selectedLogFiles.isEmpty()) {
                        snackBarHostState.showSnackbar(
                            message = getString(resource = Res.string.please_select_the_log_file_first),
                            duration = SnackbarDuration.Short
                        )
                    } else {
                        val outFiles = pageViewState.decodeLog()
                        val selectedLogFilesSize = selectedLogFiles.size
                        val successSize = if (outFiles.isNullOrEmpty()) {
                            0
                        } else {
                            outFiles.size
                        }
                        val failedSize = selectedLogFilesSize - successSize
                        val result = snackBarHostState.showSnackbar(
                            message = getString(
                                resource = Res.string.file_status_summary,
                                selectedLogFilesSize,
                                successSize,
                                failedSize
                            ),
                            actionLabel = if (outFiles.isNullOrEmpty()) {
                                null
                            } else {
                                getString(resource = Res.string.open_the_file)
                            },
                            withDismissAction = true
                        )
                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                if (!outFiles.isNullOrEmpty()) {
                                    pageViewState.openFile(outFiles)
                                }
                            }

                            SnackbarResult.Dismissed -> {

                            }
                        }
                    }
                }
            }
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(resource = Res.string.parse_the_file)
            )
        }
        RuntimeLog(
            logs = pageViewState.runtimeLogs,
            lazyListState = pageViewState.lazyListState
        )
    }
}

@Composable
private fun LogFilePath(
    coroutineScope: CoroutineScope,
    selectedLogFiles: List<String>,
    confirmLogFiles: (List<String>) -> Unit
) {
    val logPath = remember(key1 = selectedLogFiles) {
        selectedLogFiles.joinToString(separator = "\n", limit = 5)
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
                        if (dragData is DragData.FilesList) {
                            val files = dragData.readFiles()
                            files.any {
                                val path = URI(it).toPath()
                                path.isXLogFile()
                            }
                        } else {
                            false
                        }
                    },
                    target = object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val dragData = event.dragData() as DragData.FilesList
                            val files = dragData.readFiles()
                            val paths = files.mapNotNull {
                                val path = URI(it).toPath()
                                if (path.isXLogFile()) {
                                    path.pathString
                                } else {
                                    null
                                }
                            }
                            if (paths.isNotEmpty()) {
                                confirmLogFiles(paths)
                            }
                            return true
                        }
                    }
                ),
            value = logPath,
            readOnly = true,
            shape = RoundedCornerShape(size = 18.dp),
            label = {
                Text(
                    modifier = Modifier,
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
                            type = FileKitType.File(extension = xLogFileExtension),
                            mode = FileKitMode.Multiple()
                        )
                        val paths = files?.mapNotNull {
                            val file = it.file
                            if (file.isXLogFile()) {
                                file.path
                            } else {
                                null
                            }
                        }
                        if (!paths.isNullOrEmpty()) {
                            confirmLogFiles(paths)
                        }
                    }
                })
        )
    }
}

@Composable
private fun PrivateKey(
    privateKey: String,
    onInputPrivateKey: (String) -> Unit
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
        onValueChange = onInputPrivateKey
    )
}

@Composable
private fun RuntimeLog(
    logs: List<String>,
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
                items(
                    items = logs,
                    contentType = {
                        "Log"
                    }
                ) {
                    Log(
                        modifier = Modifier
                            .fillMaxWidth(),
                        log = it
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
private fun Log(
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

private const val xLogFileExtension = "xlog"

private fun Path.isXLogFile(): Boolean {
    val file = File(pathString)
    return file.isXLogFile()
}

private fun File.isXLogFile(): Boolean {
    return exists() && isFile && extension == xLogFileExtension
}