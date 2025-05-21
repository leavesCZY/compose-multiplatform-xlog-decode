package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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
private const val xLogFileExtension = "xlog"

private fun Path.isXLogFile(): Boolean {
    val file = File(pathString)
    return file.exists() && file.isFile && file.extension == xLogFileExtension
}

@Composable
fun FrameWindowScope.MainPage(
    pageViewState: MainPageViewState,
    snackBarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        LogFilePath(
            logPath = pageViewState.logPath,
            confirmLogFilePath = {
                pageViewState.onInputLogFilePath(it)
            },
            openFileDialog = {
                coroutineScope.launch {
                    pageViewState.openFileDialog()
                }
            }
        )
        PrivateKey(
            privateKey = pageViewState.privateKey,
            onInputPrivateKey = pageViewState.onInputPrivateKey
        )
        Button(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.3f),
            onClick = {
                coroutineScope.launch {
                    val logPath = pageViewState.logPath
                    if (logPath.isBlank()) {
                        snackBarHostState.showSnackbar(
                            message = "请先选择日志文件",
                            duration = SnackbarDuration.Short
                        )
                    } else {
                        val outFile = pageViewState.decodeLog()
                        if (outFile != null) {
                            val result = snackBarHostState.showSnackbar(
                                message = "解析成功，文件路径：" + outFile.absolutePath,
                                actionLabel = "打开文件",
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
                text = "解析日志"
            )
        }
        RuntimeLog(
            log = pageViewState.runtimeLog,
            scrollState = pageViewState.logScrollState
        )
    }
    if (pageViewState.openDialog.isAwaiting) {
        val fileExtension = xLogFileExtension
        FileDialog(
            title = "请选择 $fileExtension 文件",
            isMultipleMode = false,
            fileExtension = fileExtension,
            onResult = {
                if (it != null) {
                    if (it.isXLogFile()) {
                        pageViewState.onInputLogFilePath(it)
                    } else {
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(message = "请选择 $fileExtension 文件")
                        }
                    }
                }
                pageViewState.openDialog.onResult(result = it)
            }
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
        shape = RoundedCornerShape(size = 16.dp),
        label = {
            Text(
                modifier = Modifier,
                text = "如果日志有进行加密则输入私钥，否则无需输入"
            )
        },
        onValueChange = onInputPrivateKey
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
            shape = RoundedCornerShape(size = 16.dp),
            label = {
                Text(
                    modifier = Modifier,
                    text = "点击选择日志文件，或者拖动日志文件到此处"
                )
            },
            onValueChange = {}
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape = RoundedCornerShape(size = 16.dp))
                .clickable(onClick = openFileDialog)
        )
    }
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