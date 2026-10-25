package com.theseuntaylor.vane.feature.widget.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.theseuntaylor.vane.MainActivity
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.theme.DarkColorScheme
import com.theseuntaylor.vane.core.theme.LightColorScheme
import com.theseuntaylor.vane.core.utils.WmoCodes
import com.theseuntaylor.vane.core.utils.weatherIconRes
import com.theseuntaylor.vane.feature.widget.data.FavouriteWeather
import com.theseuntaylor.vane.feature.widget.data.HourlyTemperature
import com.theseuntaylor.vane.feature.widget.data.WidgetState
import com.theseuntaylor.vane.feature.widget.data.WidgetWeatherSnapshot
import com.theseuntaylor.vane.feature.widget.data.WidgetWeatherStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.math.roundToInt

/** The app's generated M3 palette, so the widget matches the app in light and dark mode. */
private val VaneWidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

class VaneWeatherWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .widgetWeatherStore()
        val states = store.observe()
        val initial = store.readState()

        provideContent {
            val state by states.collectAsState(initial = initial)
            GlanceTheme(colors = VaneWidgetColors) {
                WidgetContent(context, state)
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun widgetWeatherStore(): WidgetWeatherStore
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 200.dp)
        private val TALL = DpSize(250.dp, 300.dp)
    }
}

@Composable
private fun WidgetContent(context: Context, state: WidgetState) {
    val size = LocalSize.current
    val snapshot = state.current
    val showFavourites = size.width >= 250.dp && size.height >= 300.dp && state.favourites.isNotEmpty()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = if (showFavourites) Alignment.Vertical.Top else Alignment.Vertical.CenterVertically,
    ) {
        when {
            snapshot == null -> EmptyState(context)
            size.width < 250.dp -> CompactWeather(context, snapshot)
            else -> {
                WideWeather(context, snapshot)
                if (size.height >= 200.dp && snapshot.upcomingHours.isNotEmpty()) {
                    Spacer(GlanceModifier.height(10.dp))
                    UpcomingHours(snapshot.upcomingHours)
                }
            }
        }
        if (showFavourites) {
            Spacer(GlanceModifier.height(10.dp))
            Favourites(context, state.favourites)
        }
    }
}

/** An M3 tonal container for a group of content inside the widget. */
@Composable
private fun TonalCard(
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content,
    )
}

@Composable
private fun EmptyState(context: Context) {
    Text(
        text = context.getString(R.string.widget_empty_state),
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
    )
}

@Composable
private fun WeatherIcon(weatherCode: Int, size: Dp, tint: ColorProvider = GlanceTheme.colors.primary) {
    Image(
        provider = ImageProvider(weatherIconRes(weatherCode)),
        contentDescription = conditionLabel(weatherCode),
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(size),
    )
}

/** The current condition's icon on a round primary-container badge. */
@Composable
private fun WeatherBadge(weatherCode: Int, size: Dp) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(size / 2),
        contentAlignment = Alignment.Center,
    ) {
        WeatherIcon(weatherCode, size * 0.6f, GlanceTheme.colors.onPrimaryContainer)
    }
}

@Composable
private fun CompactWeather(context: Context, snapshot: WidgetWeatherSnapshot) {
    Text(
        text = snapshot.locationName,
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    )
    Spacer(GlanceModifier.height(4.dp))
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        WeatherBadge(snapshot.weatherCode, 36.dp)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = formatTemperature(snapshot.temperature),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Medium),
        )
    }
    Spacer(GlanceModifier.height(6.dp))
    UpdatedChip(context, snapshot)
}

@Composable
private fun WideWeather(context: Context, snapshot: WidgetWeatherSnapshot) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        WeatherBadge(snapshot.weatherCode, 44.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = snapshot.locationName,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            )
            Text(
                text = conditionLabel(snapshot.weatherCode),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = formatTemperature(snapshot.temperature),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 36.sp, fontWeight = FontWeight.Medium),
        )
    }
    // Second row so the name and condition get the full width above.
    Spacer(GlanceModifier.height(6.dp))
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        UpdatedChip(context, snapshot)
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = context.getString(
                R.string.widget_high_low,
                formatTemperature(snapshot.high),
                formatTemperature(snapshot.low),
            ),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

@Composable
private fun UpcomingHours(hours: List<HourlyTemperature>) {
    TonalCard {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            hours.forEach { hour ->
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(
                        text = hour.hour,
                        style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp),
                    )
                    // Snapshots saved before hourly codes were requested have no icon.
                    if (hour.weatherCode >= 0) {
                        Spacer(GlanceModifier.height(2.dp))
                        WeatherIcon(hour.weatherCode, 20.dp)
                        Spacer(GlanceModifier.height(2.dp))
                    }
                    Text(
                        text = formatTemperature(hour.temperature),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Favourites(context: Context, favourites: List<FavouriteWeather>) {
    Text(
        text = context.getString(R.string.widget_favourites),
        modifier = GlanceModifier.padding(start = 4.dp, bottom = 4.dp),
        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    )
    // Fills the remaining height, so the widget has no empty band; scrolls when there are more favourites than fit.
    TonalCard(modifier = GlanceModifier.defaultWeight()) {
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(favourites) { favourite ->
                FavouriteRow(context, favourite)
            }
        }
    }
}

@Composable
private fun FavouriteRow(context: Context, favourite: FavouriteWeather) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        WeatherIcon(favourite.weatherCode, 22.dp)
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = favourite.name,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 15.sp),
        )
        if (favourite.high != null && favourite.low != null) {
            Text(
                text = context.getString(
                    R.string.widget_high_low,
                    formatTemperature(favourite.high),
                    formatTemperature(favourite.low),
                ),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
            Spacer(GlanceModifier.width(10.dp))
        }
        Text(
            text = formatTemperature(favourite.temperature),
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** "Updated 14 minutes ago" on a small tonal chip; when offline, this is how old the data is. */
@Composable
private fun UpdatedChip(context: Context, snapshot: WidgetWeatherSnapshot) {
    val relative = DateUtils.getRelativeTimeSpanString(
        snapshot.updatedAtMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    )
    Row(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_updated),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            modifier = GlanceModifier.size(12.dp),
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = context.getString(R.string.widget_updated, relative),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 11.sp),
        )
    }
}

private fun formatTemperature(celsius: Double) = "${celsius.roundToInt()}°"

private fun conditionLabel(code: Int) = WmoCodes.getByValue(code)?.info?.second ?: ""
