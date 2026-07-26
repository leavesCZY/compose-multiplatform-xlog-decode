package github.leavesczy.xlog.decode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.viewmodel.compose.viewModel
import compose_multiplatform_xlog_decode.generated.resources.Res
import compose_multiplatform_xlog_decode.generated.resources.app_name
import compose_multiplatform_xlog_decode.generated.resources.application_icon
import compose_multiplatform_xlog_decode.generated.resources.decode
import compose_multiplatform_xlog_decode.generated.resources.secret_key
import compose_multiplatform_xlog_decode.generated.resources.settings
import github.leavesczy.xlog.decode.logic.MainViewModel
import github.leavesczy.xlog.decode.logic.Page
import github.leavesczy.xlog.decode.platform.DesktopOs
import github.leavesczy.xlog.decode.ui.DecodePage
import github.leavesczy.xlog.decode.ui.SecretKeyPage
import github.leavesczy.xlog.decode.ui.SettingsPage
import github.leavesczy.xlog.decode.ui.theme.AppTheme
import io.github.vinceglb.filekit.FileKit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Toolkit

private const val APP_ID = "compose-multiplatform-xlog-decode"

fun main() {
    configureDesktopPlatform()
    FileKit.init(appId = APP_ID)
    application {
        Window(
            title = stringResource(resource = Res.string.app_name),
            icon = painterResource(resource = Res.drawable.application_icon),
            state = rememberWindowState(
                size = preferredWindowSize(),
                position = WindowPosition.Aligned(alignment = Alignment.Center)
            ),
            onCloseRequest = ::exitApplication
        ) {
            App()
        }
    }
}

private fun configureDesktopPlatform() {
    if (DesktopOs.isMacOs) {
        System.setProperty("apple.awt.application.appearance", "system")
    }
}

@Composable
private fun App() {
    val mainViewModel = viewModel {
        MainViewModel()
    }
    val coroutineScope = rememberCoroutineScope()
    AppTheme(themeMode = mainViewModel.settingsPageViewState.themeMode) {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            space = 24.dp,
                            alignment = Alignment.CenterVertically
                        )
                    ) {
                        Page.entries.forEach { page ->
                            val (icon, title) = pageNavigation(page = page)
                            NavigationRailItem(
                                selected = mainViewModel.mainPageViewState.page == page,
                                label = {
                                    Text(text = stringResource(resource = title))
                                },
                                icon = {
                                    Icon(
                                        modifier = Modifier.size(size = 22.dp),
                                        imageVector = icon,
                                        contentDescription = stringResource(resource = title)
                                    )
                                },
                                onClick = {
                                    mainViewModel.mainPageViewState.onPageSelected(page)
                                }
                            )
                        }
                    }
                }
                when (mainViewModel.mainPageViewState.page) {
                    Page.Decode -> {
                        DecodePage(
                            pageViewState = mainViewModel.decodePageViewState,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope
                        )
                    }

                    Page.SecretKey -> {
                        SecretKeyPage(
                            pageViewState = mainViewModel.secretKeyPageViewState
                        )
                    }

                    Page.Settings -> {
                        SettingsPage(
                            pageViewState = mainViewModel.settingsPageViewState
                        )
                    }
                }
            }
        }
    }
}

private fun pageNavigation(page: Page): Pair<ImageVector, StringResource> {
    return when (page) {
        Page.Decode -> Icons.Outlined.Refresh to Res.string.decode
        Page.SecretKey -> Icons.Outlined.Lock to Res.string.secret_key
        Page.Settings -> Icons.Outlined.Settings to Res.string.settings
    }
}

private fun preferredWindowSize(): DpSize {
    val aspectRatio = 1.72f
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val preferredHeight = screenSize.height * 0.60f
    val preferredWidth = minOf(a = screenSize.width * 0.70f, b = preferredHeight * aspectRatio)
    return DpSize(width = preferredWidth.dp, height = preferredHeight.dp)
}