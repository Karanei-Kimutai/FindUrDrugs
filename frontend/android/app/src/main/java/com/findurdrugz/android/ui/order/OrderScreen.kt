package com.findurdrugz.android.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Lets the user confirm details and submit either a delivery order or a
 * pickup reservation for the medicine they selected on SearchScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel,
    onBack: () -> Unit,
    onOrderComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Reads the medicine selected on the previous screen (see SelectedMedicineHolder).
    val medicine = SelectedMedicineHolder.selected

    var quantity by remember { mutableStateOf("1") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isDelivery by remember { mutableStateOf(true) } // toggles between Order vs Reservation flow

    val uiState by viewModel.uiState.collectAsState()

    // Once the order/reservation succeeds, navigate away automatically.
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        // Defensive check: if this screen is somehow reached without a selected
        // medicine (e.g. holder wasn't set, or app process was restarted), avoid a crash.
        if (medicine == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No medicine selected.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            // Summary of what's being ordered
            Text(medicine.medicineName, style = MaterialTheme.typography.titleLarge)
            Text("${medicine.pharmacyName} — KES ${medicine.price}")
            Spacer(modifier = Modifier.height(16.dp))

            // Delivery vs Pickup toggle — determines which backend endpoint gets called
            // and which fields are shown/required below.
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
                // Filters input to digits only, preventing non-numeric crashes on toIntOrNull() later
                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Delivery-only fields — backend's /api/orders route requires these,
            // but /api/reservations does not, so we only show/send them when isDelivery is true.
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1 // fallback to 1 if input is somehow empty
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