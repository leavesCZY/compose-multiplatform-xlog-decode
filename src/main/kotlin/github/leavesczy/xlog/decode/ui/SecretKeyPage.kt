package github.leavesczy.xlog.decode.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose_multiplatform_xlog_decode.generated.resources.Res
import compose_multiplatform_xlog_decode.generated.resources.generate_the_key_pair
import compose_multiplatform_xlog_decode.generated.resources.private_key
import compose_multiplatform_xlog_decode.generated.resources.public_key
import org.jetbrains.compose.resources.stringResource

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:31
 * @Desc:
 */
@Composable
fun SecretKeyPage(pageViewState: SecretKeyPageViewState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 18.dp)
    ) {
        ReadOnlyTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = pageViewState.privateKey,
            label = stringResource(resource = Res.string.private_key)
        )
        ReadOnlyTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = pageViewState.publicKey,
            label = stringResource(resource = Res.string.public_key)
        )
        Button(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.4f)
                .height(height = 45.dp),
            onClick = pageViewState.generateTheKeyPair
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(resource = Res.string.generate_the_key_pair)
            )
        }
    }
}

@Composable
private fun ReadOnlyTextField(
    modifier: Modifier,
    label: String?,
    value: String
) {
    OutlinedTextField(
        modifier = modifier
            .animateContentSize(),
        value = value,
        readOnly = true,
        shape = RoundedCornerShape(size = 18.dp),
        label = if (label.isNullOrBlank()) {
            null
        } else {
            {
                Text(
                    modifier = Modifier,
                    text = label
                )
            }
        },
        onValueChange = {}
    )
}