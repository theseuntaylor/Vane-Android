package com.theseuntaylor.vane

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.theseuntaylor.vane.core.VaneAppState
import com.theseuntaylor.vane.core.components.VaneFloatingActionButton
import com.theseuntaylor.vane.core.navigation.AddFavouriteLocation
import com.theseuntaylor.vane.core.navigation.Home
import com.theseuntaylor.vane.core.navigation.addLocationScreen
import com.theseuntaylor.vane.core.navigation.detailedForecastScreen
import com.theseuntaylor.vane.core.navigation.homeScreen
import com.theseuntaylor.vane.core.theme.VaneTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var locationPermissionsGranted by remember {
                mutableStateOf(areLocationPermissionsAlreadyGranted())
            }
            var shouldShowPermissionRationale by remember {
                mutableStateOf(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION))
            }

            var shouldDirectUserToApplicationSettings by remember { mutableStateOf(false) }

            val locationPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )

            val locationPermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->

                        locationPermissionsGranted =
                            permissions.values.reduce { acc, isPermissionGranted ->
                                acc && isPermissionGranted
                            }

                        if (!locationPermissionsGranted) {
                            shouldShowPermissionRationale =
                                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        shouldDirectUserToApplicationSettings =
                            !shouldShowPermissionRationale && !locationPermissionsGranted
                    })

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(key1 = lifecycleOwner, effect = {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START && !locationPermissionsGranted && !shouldShowPermissionRationale) {
                        locationPermissionLauncher.launch(locationPermissions)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            })

            VaneTheme {

                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
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

                        if (shouldShowPermissionRationale) {
                            LaunchedEffect(Unit) {
                                scope.launch {
                                    val userAction = snackbarHostState.showSnackbar(
                                        message = "Please authorize location permissions",
                                        actionLabel = "Approve",
                                        duration = SnackbarDuration.Indefinite,
                                        withDismissAction = true
                                    )
                                    when (userAction) {
                                        SnackbarResult.ActionPerformed -> {
                                            shouldShowPermissionRationale = false
                                            locationPermissionLauncher.launch(locationPermissions)
                                        }

                                        SnackbarResult.Dismissed -> {
                                            shouldShowPermissionRationale = false
                                        }
                                    }
                                }
                            }
                        } else if (locationPermissionsGranted) {
                            Box(
                                modifier = modifier
                                    .padding(innerPaddingValue)
                                    .padding(12.dp)
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Home,
                                    modifier = Modifier
                                ) {
                                    homeScreen(
                                        snackbarHostState,
                                        navController = navController,
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

                        if (shouldDirectUserToApplicationSettings) {
                            openApplicationSettings()
                        }
                    }
                }
            }
        }
    }

    private fun areLocationPermissionsAlreadyGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openApplicationSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also {
            startActivity(it)
        }
    }

}