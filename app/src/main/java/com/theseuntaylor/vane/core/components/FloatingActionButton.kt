package com.theseuntaylor.vane.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.theseuntaylor.vane.R

@Composable
fun VaneFloatingActionButton(fabAction: () -> Unit) {
    FloatingActionButton(onClick = fabAction) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = stringResource(R.string.add_location_button)
        )
    }
}