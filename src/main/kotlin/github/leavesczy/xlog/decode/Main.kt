package github.leavesczy.xlog.decode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Loop
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
import compose_multiplatform_xlog_decode.generated.resources.decryption
import compose_multiplatform_xlog_decode.generated.resources.secret_key
import compose_multiplatform_xlog_decode.generated.resources.settings
import github.leavesczy.xlog.decode.logic.LogDecodeViewModel
import github.leavesczy.xlog.decode.logic.Page
import github.leavesczy.xlog.decode.ui.DecryptionPage
import github.leavesczy.xlog.decode.ui.SecretKeyPage
import github.leavesczy.xlog.decode.ui.SettingsPage
import github.leavesczy.xlog.decode.ui.theme.AppTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Toolkit

/**
 * @Author: leavesCZY
 * @Date: 2024/6/5 16:54
 * @Desc:
 */
fun main() = application {
    Window(
        title = stringResource(resource = Res.string.app_name),
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
private fun Main() {
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
                            val icon: ImageVector
                            val title: StringResource
                            when (page) {
                                Page.Decryption -> {
                                    icon = Icons.Outlined.Loop
                                    title = Res.string.decryption
                                }

                                Page.SecretKey -> {
                                    icon = Icons.Outlined.Key
                                    title = Res.string.secret_key
                                }

                                Page.Settings -> {
                                    icon = Icons.Outlined.Settings
                                    title = Res.string.settings
                                }
                            }
                            NavigationRailItem(
                                modifier = Modifier,
                                selected = logDecodeViewModel.mainPageViewState.page == page,
                                label = {
                                    Text(
                                        modifier = Modifier,
                                        text = stringResource(resource = title)
                                    )
                                },
                                icon = {
                                    Icon(
                                        modifier = Modifier
                                            .size(size = 22.dp),
                                        imageVector = icon,
                                        contentDescription = stringResource(resource = title)
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
                    Page.Decryption -> {
                        DecryptionPage(
                            pageViewState = logDecodeViewModel.decryptionPageViewState,
                            snackBarHostState = snackBarHostState
                        )
                    }

                    Page.SecretKey -> {
                        SecretKeyPage(pageViewState = logDecodeViewModel.secretKeyPageViewState)
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
    val aspectRatio = 1.72f
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val preferredHeight = screenSize.height * 0.60f
    val preferredWidth = minOf(a = screenSize.width * 0.70f, b = preferredHeight * aspectRatio)
    return DpSize(width = preferredWidth.dp, height = preferredHeight.dp)
}