package com.theseuntaylor.vane.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

@Composable
fun VaneFloatingActionButton(fabAction: () -> Unit) {
    FloatingActionButton(onClick = fabAction) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = "Plus button for adding a new location"
        )
    }
}