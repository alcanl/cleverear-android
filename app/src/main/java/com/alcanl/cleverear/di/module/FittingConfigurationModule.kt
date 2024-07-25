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
    var selectedEarSide : EarSide = EarSide.Left
    val deviceInfoExtMap: MutableMap<EarSide, DeviceInfoExt> = mutableMapOf()
    var isBinauralConnection = false
    val isInitializeCompleted : Boolean
        get(){
            return if (deviceInfoExtMap.all { it.value.product != null })
                deviceInfoExtMap.all { it.value.isInitFinished }
            else if (deviceInfoExtMap[EarSide.Left]?.product != null)
                deviceInfoExtMap[EarSide.Left]?.isInitFinished!!
            else
                deviceInfoExtMap[EarSide.Right]?.isInitFinished!!
        }

    init {
        deviceInfoExtMap[EarSide.Left] = DeviceInfoExt()
        deviceInfoExtMap[EarSide.Right] = DeviceInfoExt()
    }
    @Provides
    @Singleton
    fun create() : FittingConfigurationModule = this
}