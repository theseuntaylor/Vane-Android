package com.theseuntaylor.vane.core.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.theseuntaylor.vane.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaneAppBar(
    title: String,
    navController: NavController,
    navigateBack: () -> Unit
) {

    val backStackEntry = navController.previousBackStackEntry

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (backStackEntry != null) {
                IconButton(onClick = navigateBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.navigate_back)
                    )
                }
            }
        }
    )
}