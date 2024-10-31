package com.theseuntaylor.vane.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailChip(
    modifier: Modifier,
    weatherDetailTitle: String,
    weatherDetailValue: String,
    weatherDetailUnit: String,
    weatherDetailDescription: String = ""
) {

    val tooltipState = rememberTooltipState()
    val tooltipPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()

    TooltipBox(
        positionProvider = tooltipPositionProvider,
        tooltip = { PlainTooltip { Text(weatherDetailDescription) } },
        state = tooltipState
    ) {
        Card(
            modifier = modifier.padding(0.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = modifier.padding(8.dp)
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

            }
        }
    }
}