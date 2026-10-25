package com.theseuntaylor.vane.core.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.core.utils.returnTime
import com.theseuntaylor.vane.core.utils.weatherIconRes
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CityCard(
    modifier: Modifier = Modifier,
    onCardItemClicked: () -> Unit = {},
    name: String,
    uiModel: WeatherForecastUiModel = WeatherForecastUiModel(),
) {
    val current = uiModel.current
    Card(
        shape = RoundedCornerShape(8.dp),
        onClick = onCardItemClicked,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
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
                        contentDescription = uiModel.summary,
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
                    text = "${uiModel.current.temperature_2m.roundToInt()}°",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Row(
                modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    current.time.returnTime(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    "H ${uiModel.highestAndLowestTemperature.first.roundToInt()}° · " +
                            "L ${uiModel.highestAndLowestTemperature.second.roundToInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(uiModel.summary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}