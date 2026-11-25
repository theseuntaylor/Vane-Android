package com.theseuntaylor.vane.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.units.LocalUnits
import com.theseuntaylor.vane.core.units.formatTemperature
import com.theseuntaylor.vane.core.units.formatTime
import com.theseuntaylor.vane.core.units.temperatureValue
import com.theseuntaylor.vane.core.utils.weatherIconRes
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel

@Composable
fun CityCard(
    modifier: Modifier = Modifier,
    /** Opens this place's forecast. Null makes the card plain, not clickable. */
    onCardItemClicked: (() -> Unit)? = null,
    name: String,
    uiModel: WeatherForecastUiModel = WeatherForecastUiModel(),
    /** Shown after the temperature, e.g. a menu button. */
    actions: (@Composable () -> Unit)? = null,
) {
    val current = uiModel.current
    val units = LocalUnits.current
    val cardModifier = modifier
        .fillMaxWidth()
        .padding(top = 14.dp)
    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {

            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(weatherIconRes(current.weatherCode)),
                        contentDescription = stringResource(uiModel.summary),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatTemperature(uiModel.current.temperature_2m),
                    style = MaterialTheme.typography.headlineMedium,
                )
                actions?.invoke()
            }
            Row(
                modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(current.time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    stringResource(
                        R.string.high_low,
                        units.temperatureValue(uiModel.highestAndLowestTemperature.first),
                        units.temperatureValue(uiModel.highestAndLowestTemperature.second),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(uiModel.summary), style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (onCardItemClicked != null) {
        Card(shape = RoundedCornerShape(8.dp), onClick = onCardItemClicked, modifier = cardModifier, content = content)
    } else {
        Card(shape = RoundedCornerShape(8.dp), modifier = cardModifier, content = content)
    }
}
