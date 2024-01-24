package com.alcanl.cleverear.application

import android.app.Application
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.onsemi.androidble.BleUtil
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CleverEarApp : Application(){
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    override fun onCreate() {
        super.onCreate()
        BleUtil.BleManager_Init(this)
    }
    override fun onTerminate() {
        super.onTerminate()
        BleUtil.BleManager_DeInit()

        if (configuration.isConnected) 
            if (configuration.isDoubleDeviceConnection)
                configuration.doubleDeviceMap.forEach { it.value.endConnectionSession()}
            else
                configuration.hearingAid.endConnectionSession()

        threadPool.shutdown()        
    }
}
