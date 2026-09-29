package com.findurdrugz.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onNavigateToSearch, modifier = Modifier.fillMaxWidth()) {
            Text("Search Medicines")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth()) {
            Text("My Orders & Reservations")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Log Out")
        }
    }
}