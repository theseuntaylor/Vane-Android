package com.theseuntaylor.vane.core.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable

@Composable
fun VaneFloatingActionButton(fabAction: () -> Unit) {
    FloatingActionButton(onClick = fabAction) {

    }
}