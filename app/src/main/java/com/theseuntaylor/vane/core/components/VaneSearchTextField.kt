package com.theseuntaylor.vane.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.theseuntaylor.vane.R

@Composable
fun VaneSearchTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        singleLine = true,
        placeholder = {
            Text(stringResource(R.string.search_for_a_location))
        },
        onValueChange = onValueChanged,
        modifier = modifier.fillMaxWidth()
    )
}