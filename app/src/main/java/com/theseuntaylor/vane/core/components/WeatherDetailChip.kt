package com.theseuntaylor.vane.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeatherDetailChip(
    modifier: Modifier,
    weatherDetailTitle: String,
    weatherDetailValue: String,
    weatherDetailUnit: String,
    weatherDetailDescription: String = ""
) {
    Card(
        modifier = modifier
            .padding(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(weatherDetailTitle)

            Row {
                Text(
                    weatherDetailValue, style = TextStyle(
                        fontSize = 32.sp
                    )
                )
                Text(weatherDetailUnit)
            }

            Text(weatherDetailDescription)
        }
    }
}