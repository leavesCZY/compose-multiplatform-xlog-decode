package github.leavesczy.xlog.decode

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.lifecycle.viewmodel.compose.viewModel
import github.leavesczy.xlog.decode.ui.CryptKeyPage
import github.leavesczy.xlog.decode.ui.MainPage
import github.leavesczy.xlog.decode.ui.Page
import github.leavesczy.xlog.decode.ui.SettingsPage
import github.leavesczy.xlog.decode.ui.theme.AppTheme
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
        icon = painterResource(resourcePath = "application_icon.png"),
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
    val logDecodeViewModel = viewModel { LogDecodeViewModel() }
    val pageViewState = logDecodeViewModel.mainPageViewState
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
                        .fillMaxHeight()
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
                                selected = pageViewState.page == page,
                                alwaysShowLabel = true,
                                label = {
                                    Text(text = page.title)
                                },
                                icon = {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = page.title
                                    )
                                },
                                onClick = {
                                    pageViewState.switchPage(page)
                                }
                            )
                        }
                    }
                }
                when (pageViewState.page) {
                    Page.Main -> {
                        MainPage(
                            pageViewState = pageViewState,
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
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val screenWidth = screenSize.width
    val screenHeight = screenSize.height
    val preferredWidth = screenWidth * 0.60f
    val preferredHeight = screenHeight * 0.75f
    return DpSize(preferredWidth.dp, preferredHeight.dp)
}