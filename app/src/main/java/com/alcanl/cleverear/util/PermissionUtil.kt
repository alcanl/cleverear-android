package com.alcanl.cleverear.util

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun checkAndRequestPermissionForN(activity: AppCompatActivity, requestCode: Int)
{
    if (ContextCompat.checkSelfPermission(activity, ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) !=
        PackageManager.PERMISSION_GRANTED
    )
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
            ),
            requestCode
        )
}

@RequiresApi(Build.VERSION_CODES.Q)
fun checkAndRequestPermissionForQ(activity: AppCompatActivity, requestCode: Int)
{
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) !=
        PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(
                ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
            ), requestCode
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun checkAndRequestPermissionForS(activity: AppCompatActivity, requestCode: Int)
{
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) !=
        PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
    )
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ),
            requestCode
        )
}

fun hasLocationCoarsePermission(activity: FragmentActivity) : Boolean
{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ContextCompat.checkSelfPermission(
            activity,
            ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    else true
}

fun hasBluetoothPermission(activity: AppCompatActivity) : Boolean
{
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        }
        else -> {
            ContextCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }
}

fun hasBackgroundLocationPermission(context: Context): Boolean
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED
    }
    return true
}

fun hasForegroundServicePermission(context: Context) : Boolean
{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
        ) == PackageManager.PERMISSION_GRANTED
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    else
        true
}

fun requestLocationPermission(activity: AppCompatActivity)
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE
        )
    else
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(ACCESS_FINE_LOCATION),
            ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE
        )
}