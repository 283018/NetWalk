package edu.pwr.zpi.netwalk.location

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import edu.pwr.zpi.netwalk.logD
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Osobny fetcher lokalizacji
@RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
)
suspend fun getCurrentLocation(context: Context): Pair<Double?, Double?> =
    suspendCancellableCoroutine { continuation ->
        val client = LocationServices.getFusedLocationProviderClient(context)

        client
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null,
            ).addOnSuccessListener { location ->
                if (location != null) {
                    logD(
                        """
                        [LocationFetcher: getCurrentLocation] Location obtained: ${location.latitude}, ${location.longitude}
                        """.trimIndent(),
                    )
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    logD("[LocationFetcher: getCurrentLocation] No location returned")
                    continuation.resume(Pair(null, null))
                }
            }.addOnFailureListener { ex ->
                logD("[LocationFetcher: getCurrentLocation] Failed to obtain location: ${ex.message}")
                continuation.resume(Pair(null, null))
            }
    }
