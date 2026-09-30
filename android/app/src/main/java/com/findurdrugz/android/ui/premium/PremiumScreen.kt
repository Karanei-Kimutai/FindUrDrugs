
package com.findurdrugz.android.ui.premium

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.purchaseWith

/**
 * Lets the user view and purchase a premium subscription. Fetches real
 * offerings from RevenueCat, triggers a Play Billing purchase, then verifies
 * the result against OUR OWN backend (not just trusting RevenueCat/Play locally) —
 * matching subscriptionController.ts's server-side verification approach.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPremiumScreen()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Go Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is PremiumUiState.Loading -> CircularProgressIndicator()

                is PremiumUiState.AlreadyPremium -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✓ You're Premium!", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.benefit)
                    }
                }

                is PremiumUiState.OfferingsLoaded -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unlock Premium", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Get 50% off delivery fees on every order", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))

                        state.packages.forEach { pkg ->
                            PackageCard(
                                pkg = pkg,
                                onClick = {
                                    if (activity != null) {
                                        purchasePackage(activity, pkg, viewModel)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                is PremiumUiState.PurchaseInProgress -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Verifying your purchase...")
                    }
                }

                is PremiumUiState.PurchaseSuccess -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉 Welcome to Premium!", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.benefit)
                    }
                }

                is PremiumUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPremiumScreen() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackageCard(pkg: Package, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(pkg.product.title, style = MaterialTheme.typography.titleMedium)
            Text(pkg.product.price.formatted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Triggers RevenueCat's purchase flow, which itself launches Google Play's
 * billing UI. On success/failure, informs the ViewModel so the screen updates.
 */
private fun purchasePackage(activity: Activity, pkg: Package, viewModel: PremiumViewModel) {
    Purchases.sharedInstance.purchaseWith(
        purchaseParams = PurchaseParams.Builder(activity, pkg).build(),
        onError = { error, userCancelled ->
            if (!userCancelled) {
                viewModel.onPurchaseFailed(error.message ?: "Purchase failed")
            }
        },
        onSuccess = { _, _ ->
            viewModel.onPurchaseCompleted()
        }
    )
}