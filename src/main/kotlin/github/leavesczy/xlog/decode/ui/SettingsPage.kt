package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose_multiplatform_xlog_decode.generated.resources.Res
import compose_multiplatform_xlog_decode.generated.resources.auto_open_file_on_success
import compose_multiplatform_xlog_decode.generated.resources.theme
import compose_multiplatform_xlog_decode.generated.resources.theme_dark
import compose_multiplatform_xlog_decode.generated.resources.theme_light
import compose_multiplatform_xlog_decode.generated.resources.theme_system
import github.leavesczy.xlog.decode.logic.SettingsPageViewState
import github.leavesczy.xlog.decode.logic.ThemeMode
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsPage(pageViewState: SettingsPageViewState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 20.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 20.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Text(text = stringResource(resource = Res.string.theme))
            SingleChoiceSegmentedButtonRow(
                space = (-20).dp
            ) {
                ThemeMode.entries.forEach { themeMode ->
                    SegmentedButton(
                        selected = themeMode == pageViewState.themeMode,
                        shape = RoundedCornerShape(size = 20.dp),
                        label = {
                            Text(text = themeModeLabel(themeMode = themeMode))
                        },
                        onClick = {
                            pageViewState.onThemeModeSelected(themeMode)
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 20.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Text(text = stringResource(resource = Res.string.auto_open_file_on_success))
            Switch(
                checked = pageViewState.autoOpenOnSuccess,
                onCheckedChange = pageViewState.onAutoOpenOnSuccessChanged
            )
        }
    }
}

@Composable
private fun themeModeLabel(themeMode: ThemeMode): String {
    return stringResource(
        resource = when (themeMode) {
            ThemeMode.System -> Res.string.theme_system
            ThemeMode.Light -> Res.string.theme_light
            ThemeMode.Dark -> Res.string.theme_dark
        }
    )
}
