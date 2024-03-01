package com.alcanl.cleverear.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.alcanl.cleverear.R
import com.ark.ArkException
import com.onsemi.androidble.BleUtil
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CleverEarApp : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            BleUtil.BleManager_Init(this)
        } catch (_: ArkException) {}

        val channel = NotificationChannel(
            getString(R.string.service_notification_start_channel_id),
            getString(R.string.service_notification_start_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }
}



