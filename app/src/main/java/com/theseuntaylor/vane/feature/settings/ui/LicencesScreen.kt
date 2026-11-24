package com.theseuntaylor.vane.feature.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.components.VaneAppBar

private const val LICENCES_DIR = "font-licenses"

/** The licence texts bundled in assets/font-licenses (written there by design/generate-theme.mjs). */
@Composable
fun LicencesScreen(navController: NavController) {
    val assets = LocalContext.current.assets
    val licences = remember {
        assets.list(LICENCES_DIR).orEmpty().sorted().map { name ->
            name to assets.open("$LICENCES_DIR/$name").bufferedReader().use { it.readText() }
        }
    }

    Scaffold(topBar = {
        VaneAppBar(
            title = stringResource(R.string.open_source_licences),
            navController = navController,
            navigateBack = { navController.navigateUp() },
        )
    }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(licences, key = { it.first }) { (name, text) ->
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
