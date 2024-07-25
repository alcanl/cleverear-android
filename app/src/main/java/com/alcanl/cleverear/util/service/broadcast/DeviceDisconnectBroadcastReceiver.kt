package com.alcanl.cleverear.util.service.broadcast

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.event.DisconnectEvent
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.HA_LEFT_ADDRESS
import com.alcanl.cleverear.util.HA_RIGHT_ADDRESS
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
class DeviceDisconnectBroadcastReceiver @Inject constructor(): HiltBroadcastReceiver() {

    @Inject
    lateinit var configuration: ConfigurationModule
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
        val action = intent?.action

        val device  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

        val prefs = context?.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)!!
        val leftDevice = prefs.getString(HA_LEFT_ADDRESS, "")!!
        val rightDevice = prefs.getString(HA_RIGHT_ADDRESS, "")!!
        val notification = handleNotification(context, intent!!)

        if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED)
            onReceive(leftDevice, rightDevice, device!!, context, notification)

    }
    @SuppressLint("MissingPermission")
    private fun onReceive(leftDevice: String, rightDevice: String, device: BluetoothDevice, context: Context, notification: Notification)
    {
        try {
            if (device.address == leftDevice) {
                Log.d("debug", String.format("The Device %s disconnected", leftDevice))
                handleLeftDeviceDisconnect()
                showNotification(context, notification)
                EventBus.getDefault().post(DisconnectEvent(this,
                    configuration.hearingAidMap[EarSide.Left]
                ))
            }

            if (device.address == rightDevice) {
                Log.d("debug", String.format("The Device %s disconnected", rightDevice))
                handleRightDeviceDisconnect()
                showNotification(context, notification)
                EventBus.getDefault().post(DisconnectEvent(this,
                    configuration.hearingAidMap[EarSide.Right]
                ))
            }

        } catch (_: IllegalStateException) {}
        catch (_: Throwable) {}
    }
    private fun handleLeftDeviceDisconnect()
    {
        with(configuration) {
            if (isDoubleDeviceConnection)
                hearingAidMap[EarSide.Left]?.apply {
                    isConnected = false
                    isDeviceInRange = false
                }
            else
                hearingAidMap[earSide]?.apply {
                    isConnected = false
                    isDeviceInRange = false
                }

            isConnected = false
            canBondDeviceReConnect = false
        }
    }
    private fun handleRightDeviceDisconnect()
    {
        with(configuration) {
            if (isDoubleDeviceConnection) {
                hearingAidMap[EarSide.Right]?.apply {
                    isConnected = false
                    isDeviceInRange = false
                }
            }
            else {
                hearingAidMap[earSide]?.apply {
                    isConnected = false
                    isDeviceInRange = false
                }
            }
            isConnected = false
            canBondDeviceReConnect = false
        }
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
            .setContentTitle(context.getString(R.string.notification_disconnect_text))
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