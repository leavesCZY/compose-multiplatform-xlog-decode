package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.io.File
import java.net.URI
import kotlin.io.path.pathString
import kotlin.io.path.toPath

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:15
 * @Desc:
 */
private const val xLogFileExtension = "xlog"

@Composable
fun MainPage(
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
        PrivateKey(
            privateKey = pageViewState.privateKey,
            onInputPrivateKey = pageViewState.onInputPrivateKey
        )
        LogFilePath(
            logPath = pageViewState.logPath,
            onInputLogFilePath = {
                if (it.endsWith(suffix = xLogFileExtension)) {
                    pageViewState.onInputLogFilePath(it)
                } else {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(message = "请选择 $xLogFileExtension 文件")
                    }
                }
            }
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
    onInputLogFilePath: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        var isDragging by remember {
            mutableStateOf(value = false)
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder(
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    strokeWidth = 2.dp,
                    radius = 16.dp
                )
                .onExternalDrag(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragExit = {
                        isDragging = false
                    },
                    onDrop = { value ->
                        isDragging = false
                        val dragData = value.dragData
                        if (dragData is DragData.FilesList) {
                            val file = dragData.readFiles().firstNotNullOfOrNull {
                                val path = URI(it).toPath().pathString
                                val file = File(path)
                                if (file.exists() && file.isFile) {
                                    file
                                } else {
                                    null
                                }
                            }
                            if (file != null) {
                                onInputLogFilePath(file.absolutePath)
                            }
                        }
                    }
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
                .clickable {
                    val fileDialog = FileDialog(ComposeWindow(), "请选择 $xLogFileExtension 文件")
                    fileDialog.apply {
                        mode = FileDialog.LOAD
                        isMultipleMode = false
                        setFilenameFilter { _, name ->
                            name.endsWith(suffix = xLogFileExtension)
                        }
                        isVisible = true
                        val fileDirectory = fileDialog.directory
                        val fileName = fileDialog.file
                        if (!fileDirectory.isNullOrBlank() && !fileName.isNullOrBlank()) {
                            val filePath = fileDirectory + fileName
                            onInputLogFilePath(filePath)
                        }
                    }
                }
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

private fun Modifier.dashedBorder(
    strokeWidth: Dp,
    color: Color,
    radius: Dp
) = composed(
    factory = {
        val density = LocalDensity.current
        val strokeWidthPx = with(density) {
            strokeWidth.toPx()
        }
        val cornerRadius = with(density) {
            radius.toPx()
        }
        then(
            other = Modifier.drawWithCache {
                onDrawBehind {
                    drawRoundRect(
                        color = color,
                        style = Stroke(
                            width = strokeWidthPx,
                            pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(12f, 12f), phase = 6f)
                        ),
                        cornerRadius = CornerRadius(x = cornerRadius, y = cornerRadius)
                    )
                }
            }
        )
    }
)