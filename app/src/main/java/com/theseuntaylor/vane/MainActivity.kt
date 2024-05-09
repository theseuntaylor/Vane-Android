package com.theseuntaylor.vane

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.theme.VaneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val cities = listOf("London", "Birmingham", "Manchester", "Anfield")

                    LazyColumn {
                        itemsIndexed(cities){ _, city ->
                            CityCard(name = city)
                        }
                    }

                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VaneTheme {
        for (i: Int in 0..3) {
            LazyColumn {
                items(i) { CityCard(name = "City $i") }
            }
        }
    }
}