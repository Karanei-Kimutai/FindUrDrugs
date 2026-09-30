package com.findurdrugz.android.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel,
    onBack: () -> Unit,
    onOrderComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val medicine = SelectedMedicineHolder.selected

    var quantity by remember { mutableStateOf("1") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isDelivery by remember { mutableStateOf(true) }

    val uiState by viewModel.uiState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPremiumStatus()
    }

    LaunchedEffect(uiState) {
        if (uiState is OrderUiState.Success) {
            onOrderComplete()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Place Order") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (medicine == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No medicine selected.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Text(medicine.medicineName, style = MaterialTheme.typography.titleLarge)
            Text("${medicine.pharmacyName} — KES ${medicine.price}")
            Spacer(modifier = Modifier.height(16.dp))

            Row {
                FilterChip(
                    selected = isDelivery,
                    onClick = { isDelivery = true },
                    label = { Text("Delivery") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = !isDelivery,
                    onClick = { isDelivery = false },
                    label = { Text("Pickup") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isDelivery) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Delivery Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Discount preview — purely informational; the ACTUAL discount is
                // calculated server-side in orderController.ts using the same
                // checkPremiumStatus() verification. This just previews what the
                // user should expect before they submit.
                if (isPremium) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "⭐ Premium: 50% off delivery fee applied at checkout",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    if (isDelivery) {
                        viewModel.placeOrder(medicine.pharmacyId, medicine.medicineId, qty, address, phone)
                    } else {
                        viewModel.placeReservation(medicine.pharmacyId, medicine.medicineId, qty)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is OrderUiState.Loading
            ) {
                Text(if (uiState is OrderUiState.Loading) "Placing..." else "Confirm ${if (isDelivery) "Order" else "Reservation"}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is OrderUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is OrderUiState.Success -> Text("Success! Status: ${state.result.status}", color = MaterialTheme.colorScheme.primary)
                else -> {}
            }
        }
    }
}