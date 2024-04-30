package com.theseuntaylor.vane

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.core.theme.VaneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CityCard()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityCard(modifier: Modifier = Modifier, name: String = "") {

    var expanded by remember {
        mutableStateOf(false)
    }
    Card(
        onClick = {
            expanded = !expanded
        },
        modifier = modifier.fillMaxWidth().padding(5.dp),
    ) {
        Column(
            modifier = modifier.fillMaxWidth()
        ){
            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    modifier = modifier
                )
                Text(
                    text = "City Temperature",
                    modifier = modifier
                )
            }

            if (expanded){
                // show additional details for the city.
                // 1. Forecast for the rest of the week
                // 2.
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VaneTheme {
        for (i: Int in 1..5) {
            LazyColumn {
                items(i) { CityCard(name = "City $i") }
            }
        }
    }
}