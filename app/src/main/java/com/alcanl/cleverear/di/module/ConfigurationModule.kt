package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.entity.EddystoneHearingAid
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

    @Volatile
    var stopServiceFlag = false

    var isDoubleDeviceConnection = false

    var isDualDeviceConfig = false

    var isConnectedBefore = false

    var isServiceOnBoard = false

    var isBluetoothEnabled = false

    var isEddyStoneScanInProgress = false

    var isEddystoneTrackingActive = false

    var isRemoteFittingConnection = false

    var earSide : EarSide = EarSide.Right

    val hearingAidMap : EnumMap<EarSide, HearingAid> = EnumMap(EarSide::class.java)

    val eddyStoneMap : EnumMap<EarSide, EddystoneHearingAid> by lazy {
        EnumMap(EarSide::class.java)
    }

    @Singleton
    @Provides
    fun createConfiguration() : ConfigurationModule
    {
        return this
    }
    fun setConnections()
    {
        isConnected = try {
            if (isDoubleDeviceConnection)
                hearingAidMap.all { it.value.isConnected }
            else
                hearingAidMap[earSide]?.isConnected ?: false
        } catch (_ : Throwable) { false }
    }

}