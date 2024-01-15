package com.alcanl.cleverear.di.module


import com.alcanl.cleverear.helpers.connection.DeviceConnectionHandler
import com.ark.EventHandler
import com.ark.ProductManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProductManagerModule {
    private val mProductManager = ProductManager.getInstance()
    @Provides
    @Singleton
    fun createProductManager() : ProductManager = mProductManager
    @Provides
    @Singleton
    fun createEventHandler() : EventHandler = mProductManager.eventHandler
    @Provides
    @Singleton
    fun createDeviceConnectionHandler() : DeviceConnectionHandler
    {
        return DeviceConnectionHandler(mProductManager)
    }
}