package com.theseuntaylor.vane.core.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.theseuntaylor.vane.R

@Composable
fun VaneFloatingActionButton(fabAction: () -> Unit) {
    FloatingActionButton(onClick = fabAction) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = stringResource(R.string.add_location_button)
        )
    }
}