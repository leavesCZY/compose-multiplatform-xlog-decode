package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.layout.*
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
import compose_multiplatform_xlog_decode.generated.resources.the_file_will_be_automatically_opened_after_successful_parsing
import compose_multiplatform_xlog_decode.generated.resources.theme
import org.jetbrains.compose.resources.stringResource

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:16
 * @Desc:
 */
@Composable
fun SettingsPage(pageViewState: SettingsPageViewState) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 20.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 20.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(resource = Res.string.theme)
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier,
                space = (-20).dp
            ) {
                Theme.entries.forEach {
                    SegmentedButton(
                        modifier = Modifier,
                        selected = it == pageViewState.theme,
                        shape = RoundedCornerShape(size = 20.dp),
                        label = {
                            Text(
                                modifier = Modifier,
                                text = it.name
                            )
                        },
                        onClick = {
                            pageViewState.switchTheme(it)
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 20.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(resource = Res.string.the_file_will_be_automatically_opened_after_successful_parsing)
            )
            Switch(
                modifier = Modifier,
                checked = pageViewState.autoOpenFileWhenParsingIsSuccessful,
                onCheckedChange = pageViewState.updateAutoOpenFileWhenParsingIsSuccessful
            )
        }
    }
}