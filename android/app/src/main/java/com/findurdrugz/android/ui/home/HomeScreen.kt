package com.findurdrugz.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPremium by viewModel.isPremium.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPremiumStatus()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isPremium) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("⭐ Premium Member", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = onNavigateToSearch, modifier = Modifier.fillMaxWidth()) {
            Text("Search Medicines")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth()) {
            Text("My Orders & Reservations")
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (!isPremium) {
            OutlinedButton(onClick = onNavigateToPremium, modifier = Modifier.fillMaxWidth()) {
                Text("⭐ Go Premium")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Log Out")
        }
    }
}