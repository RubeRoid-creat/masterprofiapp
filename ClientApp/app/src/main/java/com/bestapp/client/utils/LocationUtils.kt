package com.bestapp.client.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LocationUtils {
    
    /**
     * Проверяет наличие разрешений на геолокацию
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Получает текущее местоположение пользователя
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) {
            return null
        }
        
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000L // 10 секунд
            ).build()
            
            val locationSettingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build()
            
            val settingsClient = LocationServices.getSettingsClient(context)
            val settingsResponse = withContext(Dispatchers.IO) {
                Tasks.await(settingsClient.checkLocationSettings(locationSettingsRequest))
            }
            
            if (settingsResponse.locationSettingsStates?.isLocationUsable == true) {
                withContext(Dispatchers.IO) {
                    Tasks.await(fusedLocationClient.lastLocation)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Получает адрес по координатам (обратное геокодирование)
     */
    fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Вычисляет расстояние между двумя точками в метрах (формула гаверсинуса)
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
}

