package com.theseuntaylor.vane

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.theseuntaylor.vane.core.VaneAppState
import com.theseuntaylor.vane.core.components.LocationPermissionScreen
import com.theseuntaylor.vane.core.components.VaneFloatingActionButton
import com.theseuntaylor.vane.core.navigation.AddFavouriteLocation
import com.theseuntaylor.vane.core.navigation.Home
import com.theseuntaylor.vane.core.navigation.addLocationScreen
import com.theseuntaylor.vane.core.navigation.detailedForecastScreen
import com.theseuntaylor.vane.core.navigation.homeScreen
import com.theseuntaylor.vane.core.theme.VaneTheme
import com.theseuntaylor.vane.core.utils.hasLocationPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val locationPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )

            var locationPermissionsGranted by remember { mutableStateOf(hasLocationPermission(this)) }
            // Android shows the prompt again only while this is true; after that, only Settings can grant it.
            var canAskForPermissionAgain by remember { mutableStateOf(true) }
            // Ask once per launch on our own; after that the user decides from the permission screen.
            var hasAskedForPermission by rememberSaveable { mutableStateOf(false) }

            val locationPermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        // Approximate location (coarse only, Android 12+) is enough for the weather.
                        locationPermissionsGranted = permissions.values.any { it } || hasLocationPermission(this)
                        canAskForPermissionAgain =
                            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                    })

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(key1 = lifecycleOwner, effect = {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        // The user may have changed the permission in Settings while Vane was in the background.
                        locationPermissionsGranted = hasLocationPermission(this@MainActivity)
                        if (!locationPermissionsGranted && !hasAskedForPermission) {
                            hasAskedForPermission = true
                            locationPermissionLauncher.launch(locationPermissions)
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            })

            VaneTheme {

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val appState = VaneAppState(navController = navController)

                val modifier = Modifier

                Surface(
                    modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(floatingActionButton = {
                        if (appState.shouldShowFAB) {
                            VaneFloatingActionButton {
                                navController.navigate(AddFavouriteLocation)
                            }
                        }
                    }, snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }) { innerPaddingValue ->

                        if (locationPermissionsGranted) {
                            Box(
                                modifier = modifier
                                    .padding(innerPaddingValue)
                                    // Nested screens' Scaffolds/TopAppBars must not pad for system bars again.
                                    .consumeWindowInsets(innerPaddingValue)
                                    .padding(12.dp)
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Home,
                                    modifier = Modifier
                                ) {
                                    homeScreen(navController = navController)
                                    addLocationScreen(navController = navController)
                                    detailedForecastScreen(
                                        modifier = modifier,
                                        navController = navController,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                            }
                        } else if (hasAskedForPermission) {
                            LocationPermissionScreen(
                                modifier = modifier.padding(innerPaddingValue),
                                canAskAgain = canAskForPermissionAgain,
                                onAllowLocation = { locationPermissionLauncher.launch(locationPermissions) },
                                onOpenSettings = ::openApplicationSettings,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openApplicationSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also {
            // Its own task: otherwise Settings sits on top of Vane's task, and returning to Vane
            // from Recents (or after Android kills it) shows the settings page instead of Vane.
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

}