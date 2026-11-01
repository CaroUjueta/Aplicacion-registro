package com.example.registro.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// Rastreo con FusedLocationProvider (locationUpdates). Solo se inicia con el permiso ya concedido.
class LocationTracker(context: Context) {
    private val cliente = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
        .setMinUpdateDistanceMeters(2f)
        .setMinUpdateIntervalMillis(1000L)
        .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        .setWaitForAccurateLocation(false)
        .build()

    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun iniciar(onUbicacion: (Location) -> Unit) {
        if (callback != null) return
        val nuevo = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let(onUbicacion)
            }
        }
        try {
            cliente.requestLocationUpdates(solicitud, nuevo, Looper.getMainLooper())
            callback = nuevo
        } catch (_: SecurityException) {
            // Sin permiso: no se rastrea
        }
    }

    fun detener() {
        callback?.let { cliente.removeLocationUpdates(it) }
        callback = null
    }

    // Una sola ubicación, para centrar el mapa al abrirlo
    @SuppressLint("MissingPermission")
    fun ubicacionUnica(onUbicacion: (Location) -> Unit) {
        try {
            cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { it?.let(onUbicacion) }
        } catch (_: SecurityException) {
        }
    }
}
