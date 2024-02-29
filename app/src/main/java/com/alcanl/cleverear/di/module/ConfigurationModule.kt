package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)

object ConfigurationModule {
    @Volatile
    var isConnected = false
    var isDoubleDeviceConnection = false
    var isDualDeviceConfig = false
    var isConnectedBefore = false
    var isServiceOnBoard = false
    var doubleDeviceMap = HashMap<EarSide, HearingAid>()
    var earSide : EarSide = EarSide.Right
    lateinit var hearingAid: HearingAid
    @Singleton
    @Provides
    fun createConfiguration() : ConfigurationModule
    {
        return this
    }

}