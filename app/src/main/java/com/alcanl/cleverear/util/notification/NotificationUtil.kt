package com.alcanl.cleverear.util.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alcanl.cleverear.R

fun createServiceNotification(context: Context) : Notification
{
    return NotificationCompat.Builder(context, context.getString(R.string.service_notification_start_channel_id))
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle(context.getString(R.string.service_notification_start_service_text))
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
}
fun handleDeviceFellNotification(intent: Intent, context: Context) : Notification
{
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(context.getString(R.string.service_notification_channel_id), context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))

    val pendingIntent = PendingIntent.getActivity(context, 6, intent, PendingIntent.FLAG_IMMUTABLE)

    return createFallenDeviceNotification(pendingIntent, context)
}
fun handleLowTemperatureNotification(intent: Intent, context: Context) : Notification
{
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(context.getString(R.string.service_notification_channel_id), context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))

    val pendingIntent = PendingIntent.getActivity(context, 7, intent, PendingIntent.FLAG_IMMUTABLE)

    return createLowTemperatureNotification(pendingIntent, context)
}
fun handleHighTemperatureNotification(intent: Intent, context: Context) : Notification
{
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(context.getString(R.string.service_notification_channel_id), context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))

    val pendingIntent = PendingIntent.getActivity(context, 8, intent, PendingIntent.FLAG_IMMUTABLE)

    return createHighTemperatureNotification(pendingIntent, context)
}
fun handleConnectedNotification(intent: Intent, context: Context) : Notification
{
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(context.getString(R.string.service_notification_channel_id), context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))

    val pendingIntent = PendingIntent.getActivity(context, 5, intent, PendingIntent.FLAG_IMMUTABLE)

    return createConnectedNotification(pendingIntent, context)
}
fun showNotification(notification: Notification, context: Context)
{
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED)
        NotificationManagerCompat.from(context).notify(2, notification)
}
private fun createConnectedNotification(pendingIntent: PendingIntent, context: Context) : Notification
{
    return NotificationCompat.Builder(context, context.getString(R.string.service_notification_channel_id))
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle(context.getString(R.string.notification_reconnect_text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
}
private fun createFallenDeviceNotification(pendingIntent: PendingIntent, context: Context) : Notification
{
    return NotificationCompat.Builder(context, context.getString(R.string.service_notification_channel_id))
        .setSmallIcon(R.drawable.cleverear_logo)
        .setContentTitle(context.getString(R.string.dialog_device_fall_text))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
}
private fun createLowTemperatureNotification(pendingIntent: PendingIntent, context: Context) : Notification
{
    return NotificationCompat.Builder(context, context.getString(R.string.service_notification_channel_id))
        .setSmallIcon(R.drawable.cleverear_logo)
        .setContentTitle(context.getString(R.string.dialog_low_temp_text))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
}
private fun createHighTemperatureNotification(pendingIntent: PendingIntent, context: Context) : Notification
{
    return NotificationCompat.Builder(context, context.getString(R.string.service_notification_channel_id))
        .setSmallIcon(R.drawable.cleverear_logo)
        .setContentTitle(context.getString(R.string.dialog_high_temp_text))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
}
