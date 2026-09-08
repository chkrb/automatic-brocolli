package io.github.chkrb.pqcompanion.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import io.github.chkrb.pqcompanion.ui.NavDestination

@Composable
fun HomePage(navController: NavController) {
    Scaffold {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { navController.navigate(NavDestination.SHOP.route()) }
            ) {
                Text("Scan")
            }
        }
    }
}
