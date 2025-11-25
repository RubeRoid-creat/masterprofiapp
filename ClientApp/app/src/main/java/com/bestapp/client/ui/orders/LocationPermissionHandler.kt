package com.bestapp.client.ui.orders

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.bestapp.client.utils.LocationUtils

@Composable
fun rememberLocationPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): (() -> Unit) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    return {
        if (LocationUtils.hasLocationPermission(context)) {
            onPermissionGranted()
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}





