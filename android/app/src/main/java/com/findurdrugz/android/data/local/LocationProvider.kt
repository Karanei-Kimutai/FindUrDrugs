package com.findurdrugz.android.data.local

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wraps Google Play Services' FusedLocationProviderClient to fetch the
 * device's last known location as a simple suspend function.
 *
 * Caller is responsible for checking/requesting the location permission
 * BEFORE calling this — see LocationPermissionRequest in SearchScreen.kt.
 */
class LocationProvider(context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // @SuppressLint is safe here because the permission check happens in the UI layer
    // before this function is ever called — Android's linter just can't see that at compile time.
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    continuation.resume(null) // no last-known location available (e.g. fresh emulator)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}