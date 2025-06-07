package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:31
 * @Desc:
 */
@Composable
fun CryptKeyPage(pageViewState: CryptKeyPageViewState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 20.dp)
    ) {
        ReadOnlyTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = pageViewState.privateKey,
            label = "私钥"
        )
        ReadOnlyTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = pageViewState.publicKey,
            label = "公钥"
        )
        Button(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.4f)
                .height(height = 50.dp),
            onClick = pageViewState.generateKeyPair
        ) {
            Text(
                modifier = Modifier,
                text = "生成密钥"
            )
        }
    }
}

@Composable
private fun ReadOnlyTextField(
    modifier: Modifier,
    value: String,
    label: String? = null
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        readOnly = true,
        shape = RoundedCornerShape(size = 16.dp),
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