package com.theseuntaylor.vane.core.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.R

/** A message in place of a section's content, with a "Try again" button when [onRetry] is given. */
@Composable
fun SectionMessage(@StringRes message: Int, onRetry: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.try_again))
            }
        }
    }
}
