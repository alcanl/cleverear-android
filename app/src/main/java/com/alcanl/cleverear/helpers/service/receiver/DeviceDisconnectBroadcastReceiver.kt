package com.alcanl.cleverear.helpers.service.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HA_LEFT_ADDRESS
import com.alcanl.cleverear.helpers.HA_RIGHT_ADDRESS
import com.alcanl.cleverear.helpers.SHARED_PREF_DEVICE
import dagger.hilt.android.AndroidEntryPoint
import com.alcanl.cleverear.helpers.isConnected
import javax.inject.Inject

@AndroidEntryPoint
class DeviceDisconnectBroadcastReceiver: HiltBroadcastReceiver() {

    @Inject
    lateinit var configuration: ConfigurationModule
    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
        val action = intent?.action
        val adapter = context?.getSystemService(BluetoothManager::class.java)?.adapter
        val prefs = context?.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)!!
        val leftDevice = prefs.getString(HA_LEFT_ADDRESS, "")!!
        val rightDevice = prefs.getString(HA_RIGHT_ADDRESS, "")!!
        val notification = handleNotification(context, intent!!)

        if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED && adapter?.isEnabled!!)
            onReceive(leftDevice, rightDevice, adapter, context, notification)

    }
    @SuppressLint("MissingPermission")
    private fun onReceive(leftDevice: String, rightDevice: String, adapter: BluetoothAdapter, context: Context, notification: Notification)
    {
        try {
            if (leftDevice.isNotBlank() && !isConnected(adapter.getRemoteDevice(leftDevice))) {
                Log.d("debug", String.format("Device %s disconnected", leftDevice))
                if (configuration.isDoubleDeviceConnection) {
                    configuration.doubleDeviceMap[EarSide.Left]?.isConnected = false
                    if (!configuration.doubleDeviceMap[EarSide.Right]?.isConnected!!)
                        configuration.isConnected = false
                }
                else {
                    configuration.hearingAid.isConnected = false
                    configuration.isConnected = false
                }

                showNotification(context, notification)
            }


            if (rightDevice.isNotBlank() && !isConnected(adapter.getRemoteDevice(rightDevice))) {
                Log.d("debug", String.format("Device %s disconnected", rightDevice))

                if (configuration.isDoubleDeviceConnection) {
                    configuration.doubleDeviceMap[EarSide.Right]?.isConnected = false
                    if (!configuration.doubleDeviceMap[EarSide.Left]?.isConnected!!)
                        configuration.isConnected = false
                }
                else {
                    configuration.hearingAid.isConnected = false
                    configuration.isConnected = false
                }

                showNotification(context, notification)
            }


        } catch (_: IllegalStateException) {}
    }
    private fun showNotification(context: Context, notification: Notification)
    {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(context).notify(1, notification)
    }
    private fun createDisconnectNotification(context: Context, pendingIntent: PendingIntent) : Notification
    {
        return NotificationCompat.Builder(context, "Disconnected")
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle("Hearing Aid Disconnected")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    private fun handleNotification(context: Context, intent: Intent) : Notification
    {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("Disconnected", "CleverEar", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val pendingIntent = PendingIntent.getActivity(context, 8, intent, PendingIntent.FLAG_IMMUTABLE)
        return createDisconnectNotification(context, pendingIntent)

    }
}