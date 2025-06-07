package github.leavesczy.xlog.decode

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.lifecycle.viewmodel.compose.viewModel
import compose_multiplatform_xlog_decode.generated.resources.Res
import compose_multiplatform_xlog_decode.generated.resources.application_icon
import github.leavesczy.xlog.decode.ui.CryptKeyPage
import github.leavesczy.xlog.decode.ui.MainPage
import github.leavesczy.xlog.decode.ui.Page
import github.leavesczy.xlog.decode.ui.SettingsPage
import github.leavesczy.xlog.decode.ui.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import java.awt.Toolkit

/**
 * @Author: leavesCZY
 * @Date: 2024/6/5 16:54
 * @Desc:
 */
fun main() = application {
    Window(
        title = "compose-multiplatform-xlog-decode",
        resizable = true,
        icon = painterResource(Res.drawable.application_icon),
        state = rememberWindowState(
            size = preferredWindowSize(),
            position = WindowPosition.Aligned(alignment = Alignment.Center)
        ),
        onCloseRequest = ::exitApplication
    ) {
        Main()
    }
}

@Composable
private fun FrameWindowScope.Main() {
    val logDecodeViewModel = viewModel {
        LogDecodeViewModel()
    }
    AppTheme(theme = logDecodeViewModel.settingsPageViewState.theme) {
        val snackBarHostState = remember {
            SnackbarHostState()
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            snackbarHost = {
                SnackbarHost(
                    modifier = Modifier,
                    hostState = snackBarHostState
                )
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            space = 24.dp,
                            alignment = Alignment.CenterVertically
                        )
                    ) {
                        for (page in Page.entries) {
                            NavigationRailItem(
                                modifier = Modifier,
                                selected = logDecodeViewModel.mainPageViewState.page == page,
                                alwaysShowLabel = true,
                                label = {
                                    Text(
                                        modifier = Modifier,
                                        text = page.title
                                    )
                                },
                                icon = {
                                    Icon(
                                        modifier = Modifier
                                            .size(size = 22.dp),
                                        imageVector = page.icon,
                                        contentDescription = page.title
                                    )
                                },
                                onClick = {
                                    logDecodeViewModel.mainPageViewState.switchPage(page)
                                }
                            )
                        }
                    }
                }
                when (logDecodeViewModel.mainPageViewState.page) {
                    Page.Main -> {
                        MainPage(
                            pageViewState = logDecodeViewModel.mainPageViewState,
                            snackBarHostState = snackBarHostState
                        )
                    }

                    Page.CryptKey -> {
                        CryptKeyPage(pageViewState = logDecodeViewModel.cryptKeyPageViewState)
                    }

                    Page.Settings -> {
                        SettingsPage(pageViewState = logDecodeViewModel.settingsPageViewState)
                    }
                }
            }
        }
    }
}

private fun preferredWindowSize(): DpSize {
    val aspectRatio = 1.60f
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val preferredHeight = screenSize.height * 0.60f
    val preferredWidth = minOf(screenSize.width * 0.60f, preferredHeight * aspectRatio)
    return DpSize(preferredWidth.dp, preferredHeight.dp)
}