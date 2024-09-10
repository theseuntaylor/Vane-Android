package com.theseuntaylor.vane

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.theseuntaylor.vane.core.VaneAppState
import com.theseuntaylor.vane.core.components.VaneAppBar
import com.theseuntaylor.vane.core.components.VaneFloatingActionButton
import com.theseuntaylor.vane.core.navigation.Screen
import com.theseuntaylor.vane.core.navigation.addLocationScreen
import com.theseuntaylor.vane.core.navigation.detailedForecastScreen
import com.theseuntaylor.vane.core.navigation.homeScreen
import com.theseuntaylor.vane.core.theme.VaneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaneTheme {

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val appState = VaneAppState(navController = navController)

                val modifier = Modifier

                Surface(
                    modifier = modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            if (appState.shouldShowActionBar) {
                                VaneAppBar(
                                    title = appState.currentDestination?.route.toReadableName(),
                                    navController = navController,
                                    navigateBack = {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.navigateUp()
                                        }
                                    }
                                )
                            }
                        },
                        floatingActionButton = {
                            if (appState.shouldShowFAB) {
                                VaneFloatingActionButton {
                                    navController.navigate("addLocation")
                                }
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState)
                        }
                    ) { innerPaddingValue ->
                        Box(
                            modifier = modifier
                                .padding(innerPaddingValue)
                                .padding(12.dp)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route,
                                modifier = Modifier
                            ) {
                                homeScreen(
                                    snackbarHostState,
                                    navController = navController
                                )
                                addLocationScreen(navController = navController)
                                detailedForecastScreen(
                                    modifier = modifier,
                                    navController = navController,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.toReadableName(): String {
    return when (this) {
        "homeScreen" -> "Home"
        "addLocation" -> "Add Location"
        else -> ""
    }
}
