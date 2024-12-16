package com.alcanl.cleverear.di.module

import com.ark.EventHandler
import com.ark.Manufacturing
import com.ark.ProductManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun createManufacturing() : Manufacturing = mProductManager.manufacturing

}