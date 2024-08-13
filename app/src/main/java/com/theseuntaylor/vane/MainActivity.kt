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
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.theseuntaylor.vane.core.components.VaneFloatingActionButton
import com.theseuntaylor.vane.core.theme.VaneTheme
import com.theseuntaylor.vane.feature.favouriteLocations.AddFavouriteLocation
import com.theseuntaylor.vane.feature.home.ui.HomeScreen
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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        floatingActionButton = {
                            VaneFloatingActionButton {
                                println("Floating Action Button Pressed")
                                navController.navigate("addLocation")
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState)
                        }
                    ) { padding ->
                        Box(Modifier.padding(14.dp)) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier
                            ) {
                                composable(
                                    route = "home"
                                ) {
                                    HomeScreen(snackBarHostState = snackbarHostState)
                                }
                                composable(
                                    route = "addLocation"
                                ) {
                                    AddFavouriteLocation()
                                }

                            }
                        }
                    }

                }
            }
        }
    }
}