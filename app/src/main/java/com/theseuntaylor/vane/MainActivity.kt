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
import com.theseuntaylor.vane.core.navigation.Home
import com.theseuntaylor.vane.core.navigation.addLocationScreen
import com.theseuntaylor.vane.core.navigation.detailedForecastScreen
import com.theseuntaylor.vane.core.navigation.homeScreen
import com.theseuntaylor.vane.core.theme.VaneTheme
import com.theseuntaylor.vane.core.utils.hasLocationPermission
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theseuntaylor.vane.core.navigation.TopLevelDestination
import com.theseuntaylor.vane.core.navigation.favouritesScreen
import com.theseuntaylor.vane.core.navigation.licencesScreen
import com.theseuntaylor.vane.core.navigation.settingsScreen
import com.theseuntaylor.vane.core.theme.isDark
import com.theseuntaylor.vane.feature.favouriteLocations.ui.FavouritesViewModel
import com.theseuntaylor.vane.feature.settings.data.AppearanceSettings
import com.theseuntaylor.vane.feature.settings.data.UnitSettings
import com.theseuntaylor.vane.feature.widget.ui.WidgetLinks
import com.theseuntaylor.vane.core.navigation.DetailedForecast
import androidx.compose.runtime.LaunchedEffect
import com.theseuntaylor.vane.core.units.LocalUnits
import androidx.compose.runtime.CompositionLocalProvider
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appearanceSettings: AppearanceSettings

    @Inject
    lateinit var unitSettings: UnitSettings

    // A place's details to open, from a tap on the widget. Cleared once shown.
    private var widgetDestination by mutableStateOf<DetailedForecast?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after a rotation or process restore the details are already open.
        if (savedInstanceState == null) widgetDestination = WidgetLinks.detailedForecast(intent)
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

            val themeMode by appearanceSettings.themeMode.collectAsStateWithLifecycle()
            val units by unitSettings.units.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDark()

            // The status and navigation bar icons follow the app's theme, not the system's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }

            // Units from Settings for every screen; values are converted only when shown.
            VaneTheme(darkTheme = darkTheme) { CompositionLocalProvider(LocalUnits provides units) {

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val appState = remember(navController) { VaneAppState(navController = navController) }

                val modifier = Modifier

                Surface(
                    modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(bottomBar = {
                        if (locationPermissionsGranted && appState.shouldShowBottomBar) {
                            VaneNavigationBar(appState)
                        }
                    }, snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }) { innerPaddingValue ->

                        if (locationPermissionsGranted) {
                            // Activity-scoped, so favourites (and the widget's copy) load at launch, as before.
                            val favouritesViewModel: FavouritesViewModel = hiltViewModel()
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
                                    homeScreen()
                                    favouritesScreen(navController, favouritesViewModel)
                                    settingsScreen(navController)
                                    addLocationScreen(navController = navController)
                                    licencesScreen(navController)
                                    detailedForecastScreen(
                                        modifier = modifier,
                                        navController = navController,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                                // A widget tap opens the place on top of the Favourites tab, so Back lands there.
                                LaunchedEffect(widgetDestination) {
                                    val destination = widgetDestination ?: return@LaunchedEffect
                                    appState.navigateToTopLevelDestination(TopLevelDestination.FAVOURITES)
                                    navController.navigate(destination)
                                    widgetDestination = null
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
            } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        WidgetLinks.detailedForecast(intent)?.let { widgetDestination = it }
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

/** Material 3 bottom navigation bar with the three tabs. */
@Composable
private fun VaneNavigationBar(appState: VaneAppState) {
    val currentDestination = appState.currentDestination
    NavigationBar {
        TopLevelDestination.entries.forEach { tab ->
            NavigationBarItem(
                selected = appState.isSelected(currentDestination, tab),
                onClick = { appState.navigateToTopLevelDestination(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

// The scrims enableEdgeToEdge() uses by default for three-button navigation.
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
