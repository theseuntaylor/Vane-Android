package com.theseuntaylor.vane.feature.favouriteLocations.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.theseuntaylor.vane.R

/**
 * The menu on a favourite's card: rename, move up or down, and delete. Move up and Move down
 * are left out at the ends of the list.
 */
@Composable
fun FavouriteMenu(
    placeName: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onMove: (by: Int) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.favourite_options, placeName),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fun choose(action: () -> Unit) = { expanded = false; action() }
            DropdownMenuItem(text = { Text(stringResource(R.string.favourite_rename)) }, onClick = choose(onRename))
            if (canMoveUp) {
                DropdownMenuItem(text = { Text(stringResource(R.string.favourite_move_up)) }, onClick = choose { onMove(-1) })
            }
            if (canMoveDown) {
                DropdownMenuItem(text = { Text(stringResource(R.string.favourite_move_down)) }, onClick = choose { onMove(1) })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.favourite_delete)) }, onClick = choose(onDelete))
        }
    }
}

/** Asks for a place's new name. Save is off while the name is blank. */
@Composable
fun RenameFavouriteDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favourite_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.favourite_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
