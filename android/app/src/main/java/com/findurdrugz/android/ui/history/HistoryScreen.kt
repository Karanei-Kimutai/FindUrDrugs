package com.findurdrugz.android.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findurdrugz.android.data.model.OrderOrReservation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Orders, 1 = Reservations

    val ordersState by viewModel.ordersState.collectAsState()
    val reservationsState by viewModel.reservationsState.collectAsState()

    // Load both lists once when the screen first appears
    LaunchedEffect(Unit) {
        viewModel.loadOrders()
        viewModel.loadReservations()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Orders") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reservations") }
                )
            }

            val currentState = if (selectedTab == 0) ordersState else reservationsState

            when (val state = currentState) {
                is HistoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HistoryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is HistoryUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (selectedTab == 0) "No orders yet." else "No reservations yet.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            items(state.items) { item ->
                                HistoryItemCard(item)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: OrderOrReservation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.medicineName ?: "Unknown medicine", style = MaterialTheme.typography.titleMedium)
            Text(item.pharmacyName ?: "Unknown pharmacy", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Quantity: ${item.quantity}")
            Text("Status: ${item.status}")
        }
    }
}