package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.util.EarSide
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.EnumMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {
    @Volatile
    var isConnected = false
    @Volatile
    var canBondDeviceReConnect = false
    var isDoubleDeviceConnection = false
    var isDualDeviceConfig = false
    var isConnectedBefore = false
    var isServiceOnBoard = false
    var earSide : EarSide = EarSide.Right
    lateinit var doubleDeviceMap : EnumMap<EarSide, HearingAid>
    lateinit var hearingAid: HearingAid
    @Singleton
    @Provides
    fun createConfiguration() : ConfigurationModule
    {
        return this
    }

}