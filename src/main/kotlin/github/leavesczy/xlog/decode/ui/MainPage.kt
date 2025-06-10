package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import compose_multiplatform_xlog_decode.generated.resources.*
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
fun FrameWindowScope.MainPage(
    pageViewState: MainPageViewState,
    snackBarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 18.dp)
    ) {
        LogFilePath(
            logPath = pageViewState.logPath,
            confirmLogFilePath = {
                pageViewState.onInputLogFilePathChange(it)
            },
            openFileDialog = {
                coroutineScope.launch {
                    pageViewState.openFileDialog()
                }
            }
        )
        PrivateKey(
            privateKey = pageViewState.privateKey,
            onInputPrivateKeyChange = pageViewState.onInputPrivateKeyChange
        )
        Button(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.4f)
                .height(height = 45.dp),
            onClick = {
                coroutineScope.launch {
                    val logPath = pageViewState.logPath
                    if (logPath.isBlank()) {
                        snackBarHostState.showSnackbar(
                            message = getString(resource = Res.string.please_select_the_log_file_first),
                            duration = SnackbarDuration.Short
                        )
                    } else {
                        val outFile = pageViewState.decodeLog()
                        if (outFile != null) {
                            val result = snackBarHostState.showSnackbar(
                                message = getString(resource = Res.string.parsing_successful),
                                actionLabel = getString(resource = Res.string.open_the_file),
                                withDismissAction = true,
                                duration = SnackbarDuration.Short
                            )
                            when (result) {
                                SnackbarResult.ActionPerformed -> {
                                    pageViewState.openFile(outFile)
                                }

                                SnackbarResult.Dismissed -> {

                                }
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
            log = pageViewState.runtimeLog,
            scrollState = pageViewState.logScrollState
        )
    }
    val fileDialogIsAwaiting by remember {
        derivedStateOf {
            pageViewState.openDialog.onResult != null
        }
    }
    FileDialog(
        visible = fileDialogIsAwaiting,
        title = stringResource(resource = Res.string.please_select_the_xlog_file),
        fileExtension = xLogFileExtension,
        onResult = {
            if (it != null) {
                if (it.isXLogFile()) {
                    pageViewState.onInputLogFilePathChange(it)
                } else {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(message = getString(resource = Res.string.please_select_the_xlog_file))
                    }
                }
            }
            pageViewState.openDialog.onResult(result = it)
        }
    )
}

@Composable
private fun LogFilePath(
    logPath: String,
    confirmLogFilePath: (Path) -> Unit,
    openFileDialog: () -> Unit
) {
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
                                URI(it).toPath().isXLogFile()
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
                                    path
                                } else {
                                    null
                                }
                            }
                            val path = paths.firstOrNull()
                            if (path != null) {
                                confirmLogFilePath(path)
                            }
                            return true
                        }
                    },
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
                .clickable(onClick = openFileDialog)
        )
    }
}

@Composable
private fun PrivateKey(
    privateKey: String,
    onInputPrivateKeyChange: (String) -> Unit
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
        onValueChange = onInputPrivateKeyChange
    )
}

@Composable
private fun RuntimeLog(
    log: String,
    scrollState: ScrollState
) {
    LaunchedEffect(key1 = log.length) {
        scrollState.animateScrollTo(value = scrollState.maxValue)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = scrollState),
                text = log,
                fontSize = 16.sp
            )
        }
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = scrollState)
        )
    }
}

private const val xLogFileExtension = "xlog"

private fun Path.isXLogFile(): Boolean {
    val file = File(pathString)
    return file.exists() && file.isFile && file.extension == xLogFileExtension
}