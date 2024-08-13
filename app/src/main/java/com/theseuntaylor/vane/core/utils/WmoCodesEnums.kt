package com.theseuntaylor.vane.core.utils

enum class WmoCodes(val info: Pair<Int, String>) {
    CLEAR_SKY(Pair(0, "Clear Sky")),
    MAINLY_CLEAR(Pair(1, "Mainly Clear Skies")),
    PARTLY_CLOUDY(Pair(2, "Partly Cloudy Skies")),
    OVERCAST(Pair(3, "Overcast Skies")),
    FOGGY(Pair(45, "Expect Fogs")),
    RIME_FOG(Pair(48, "Depositing Rime Fog")),
    LIGHT_DRIZZLE(Pair(51, "Light Drizzles")),
    MODERATE_DRIZZLE(Pair(53, "Moderate Drizzles")),
    DENSE_DRIZZLE(Pair(55, "Dense Drizzles")),
    SLIGHT_RAIN(Pair(61, "Slight Intensity Rain")),
    MODERATE_RAIN(Pair(63, "Moderate Intensity Rain")),
    HEAVY_RAIN(Pair(65, "Heavy Intensity Rain")),
    LIGHT_INTENSITY_FREEZING_RAIN(Pair(66, "Light Intensity Freezing Rain")),
    HEAVY_INTENSITY_FREEZING_RAIN(Pair(67, "Heavy Intensity Freezing Rain")),
    SLIGHT_SNOWFALL(Pair(71, "Slight Intensity Snow Fall")),
    MODERATE_SNOWFALL(Pair(73, "Moderate Intensity Snow Fall")),
    HEAVY_SNOWFALL(Pair(75, "Heavy Intensity Snow Fall")),
    SNOW_GRAINS(Pair(77, "Snow Grains")),
    SLIGHT_RAIN_SHOWERS(Pair(80, "Slight Rain Showers")),
    MODERATE_RAIN_SHOWERS(Pair(81, "Moderate Rain Showers")),
    VIOLENT_RAIN_SHOWERS(Pair(82, "Violent Rain Showers")),
    SLIGHT_SNOW_SHOWERS(Pair(85, "Slight Snow Showers")),
    HEAVY_SNOW_SHOWERS(Pair(86, "Heavy Snow Showers")),
    SLIGHT_THUNDERSTORM(Pair(95, "Slight or Moderate Thunderstorms")),
    THUNDERSTORM_WITH_HEAVY_HAIL(Pair(99, "Thunderstorm with Heavy Hail")),
    THUNDERSTORM_WITH_SLIGHT_HAIL(Pair(96, "Thunderstorm with Slight Hail"));

    companion object {
        fun getByValue(id: Int) = entries.firstOrNull { it.info.first == id }
    }
}