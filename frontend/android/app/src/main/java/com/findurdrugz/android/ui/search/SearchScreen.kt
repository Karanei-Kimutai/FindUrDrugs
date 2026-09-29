package com.findurdrugz.android.ui.search

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.findurdrugz.android.data.local.LocationProvider
import com.findurdrugz.android.data.model.MedicineResult
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Screen for searching medicines across pharmacies.
 * Requests device location (falling back to Nairobi coordinates if permission
 * is denied or location is unavailable), then calls GET /api/search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onResultSelected: (MedicineResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationProvider = remember { LocationProvider(context) }

    var query by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("nearest") }

    // Fallback coordinates (Nairobi) — used if permission is denied or location is null
    var lat by remember { mutableStateOf(-1.286389) }
    var lng by remember { mutableStateOf(36.817223) }
    var locationLabel by remember { mutableStateOf("Using default location (Nairobi)") }

    val uiState by viewModel.uiState.collectAsState()

    // Launcher for the runtime permission request dialog
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                val result = locationProvider.getLastLocation()
                if (result != null) {
                    lat = result.first
                    lng = result.second
                    locationLabel = "Using your current location"
                } else {
                    locationLabel = "Location unavailable — using default (Nairobi)"
                }
            }
        } else {
            locationLabel = "Location permission denied — using default (Nairobi)"
        }
    }

    // On first entering the screen: check if permission is already granted,
    // and either fetch location directly or request permission.
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val result = locationProvider.getLastLocation()
            if (result != null) {
                lat = result.first
                lng = result.second
                locationLabel = "Using your current location"
            } else {
                locationLabel = "Location unavailable — using default (Nairobi)"
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Search Medicines") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {

            Text(locationLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Medicine name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.search(query, lat, lng, sortBy) }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sort by: ")
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = sortBy == "nearest",
                    onClick = { sortBy = "nearest" },
                    label = { Text("Nearest") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = sortBy == "cheapest",
                    onClick = { sortBy = "cheapest" },
                    label = { Text("Cheapest") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.search(query, lat, lng, sortBy) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is SearchUiState.Loading
            ) {
                Text(if (uiState is SearchUiState.Loading) "Searching..." else "Search")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is SearchUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        Text("No results found.")
                    } else {
                        LazyColumn {
                            items(state.results) { result ->
                                MedicineResultCard(
                                    result = result,
                                    onClick = { onResultSelected(result) }
                                )
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
fun MedicineResultCard(result: MedicineResult, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(result.pharmacyName, style = MaterialTheme.typography.titleMedium)
            Text(result.address, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${result.medicineName} (${result.strength})")
            Text("Price: KES ${result.price}")
            Text("Distance: ${round(result.distance * 10) / 10} km")
            Text("Stock: ${result.quantity}")
            if (result.verified) {
                Text("✓ Verified pharmacy", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}