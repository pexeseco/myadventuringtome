package ipca.block.myadventuringtome

import AppNavigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ipca.block.myadventuringtome.ui.theme.MyAdventuringTomeTheme
import ipca.block.myadventuringtome.models.Campaign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAdventuringTomeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // You should load campaigns from your data source
                    // For now, using empty list, but you might want to use a ViewModel
                    val campaigns = emptyList<Campaign>() // or load from ViewModel/Repository
                    AppNavigation(campaigns = campaigns)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyAdventuringTomeTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "My Adventuring Tome",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}