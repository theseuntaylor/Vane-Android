package com.theseuntaylor.vane.core.utils

import androidx.annotation.StringRes
import com.theseuntaylor.vane.R

enum class WmoCodes(val code: Int, @StringRes val description: Int) {
    CLEAR_SKY(0, R.string.wmo_clear_sky),
    MAINLY_CLEAR(1, R.string.wmo_mainly_clear),
    PARTLY_CLOUDY(2, R.string.wmo_partly_cloudy),
    OVERCAST(3, R.string.wmo_overcast),
    FOGGY(45, R.string.wmo_foggy),
    RIME_FOG(48, R.string.wmo_rime_fog),
    LIGHT_DRIZZLE(51, R.string.wmo_light_drizzle),
    MODERATE_DRIZZLE(53, R.string.wmo_moderate_drizzle),
    DENSE_DRIZZLE(55, R.string.wmo_dense_drizzle),
    SLIGHT_RAIN(61, R.string.wmo_slight_rain),
    MODERATE_RAIN(63, R.string.wmo_moderate_rain),
    HEAVY_RAIN(65, R.string.wmo_heavy_rain),
    LIGHT_INTENSITY_FREEZING_RAIN(66, R.string.wmo_light_freezing_rain),
    HEAVY_INTENSITY_FREEZING_RAIN(67, R.string.wmo_heavy_freezing_rain),
    SLIGHT_SNOWFALL(71, R.string.wmo_slight_snowfall),
    MODERATE_SNOWFALL(73, R.string.wmo_moderate_snowfall),
    HEAVY_SNOWFALL(75, R.string.wmo_heavy_snowfall),
    SNOW_GRAINS(77, R.string.wmo_snow_grains),
    SLIGHT_RAIN_SHOWERS(80, R.string.wmo_slight_rain_showers),
    MODERATE_RAIN_SHOWERS(81, R.string.wmo_moderate_rain_showers),
    VIOLENT_RAIN_SHOWERS(82, R.string.wmo_violent_rain_showers),
    SLIGHT_SNOW_SHOWERS(85, R.string.wmo_slight_snow_showers),
    HEAVY_SNOW_SHOWERS(86, R.string.wmo_heavy_snow_showers),
    SLIGHT_THUNDERSTORM(95, R.string.wmo_thunderstorm),
    THUNDERSTORM_WITH_HEAVY_HAIL(99, R.string.wmo_thunderstorm_heavy_hail),
    THUNDERSTORM_WITH_SLIGHT_HAIL(96, R.string.wmo_thunderstorm_slight_hail);

    companion object {
        fun getByValue(id: Int) = entries.firstOrNull { it.code == id }
    }
}
