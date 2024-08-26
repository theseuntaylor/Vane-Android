package com.theseuntaylor.vane.core.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.core.utils.returnTime
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CityCard(
    modifier: Modifier = Modifier,
    name: String,
    uiModel: WeatherForecastUiModel = WeatherForecastUiModel(),
) {

    var expanded by remember { mutableStateOf(true) }
    val current = uiModel.current
    Card(
        shape = RoundedCornerShape(8.dp),
        onClick = {
            expanded = !expanded
        },
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${uiModel.current.temperature_2m}°C",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(current.time.returnTime())

                Text(
                    "${uiModel.highestAndLowestTemperature.second} " +
                            "-" +
                            " ${uiModel.highestAndLowestTemperature.first}"
                )
            }
            Text(uiModel.summary)

        }
    }
}