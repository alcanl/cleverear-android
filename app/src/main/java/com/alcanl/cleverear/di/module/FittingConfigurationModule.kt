package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.fitting.entity.DeviceInfoExt
import com.alcanl.cleverear.util.EarSide
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FittingConfigurationModule {
    lateinit var selectedEarSide : EarSide
    var deviceInfoExtMap: MutableMap<EarSide, DeviceInfoExt> = mutableMapOf()
    var isBinauralConnection = false
    @Provides
    @Singleton
    fun create() : FittingConfigurationModule = this
}