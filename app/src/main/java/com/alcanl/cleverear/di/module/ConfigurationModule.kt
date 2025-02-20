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
    var keepConnectionForServiceDestroy = false

    @Volatile
    var isBluetoothEnabled = false

    @Volatile
    var isServiceOnBoard = false

    @Volatile
    var trackLocationAlwaysSuccessFlag = true

    var isDoubleDeviceConnection = false

    var isDualDeviceConfig = false

    var isConnectedBefore = false

    var isUserDisconnected = false

    var isEddyStoneScanInProgress = false

    var isRemoteFittingConnection = false

    var isMainActivityOnScreen = false

    var earSide : EarSide = EarSide.Right

    val hearingAidMap : EnumMap<EarSide, HearingAid> = EnumMap(EarSide::class.java)

    val eddyStoneMap : EnumMap<EarSide, EddystoneHearingAid> = EnumMap(EarSide::class.java)

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

    fun reSetConfiguration(config : ConfigurationModule)
    {
        isConnected = config.isConnected
        canBondDeviceReConnect = config.canBondDeviceReConnect
        keepConnectionForServiceDestroy = config.keepConnectionForServiceDestroy
        isBluetoothEnabled = config.isBluetoothEnabled
        isServiceOnBoard = config.isServiceOnBoard
        trackLocationAlwaysSuccessFlag = config.trackLocationAlwaysSuccessFlag
        isDoubleDeviceConnection = config.isDoubleDeviceConnection
        isDualDeviceConfig = config.isDualDeviceConfig
        isConnectedBefore = config.isConnectedBefore
        isUserDisconnected = config.isUserDisconnected
        isEddyStoneScanInProgress = config.isEddyStoneScanInProgress
        isRemoteFittingConnection = config.isRemoteFittingConnection
        isMainActivityOnScreen = config.isMainActivityOnScreen
        earSide = config.earSide
        hearingAidMap[EarSide.Left] = config.hearingAidMap[EarSide.Left]
        hearingAidMap[EarSide.Right] = config.hearingAidMap[EarSide.Right]
        eddyStoneMap[EarSide.Left] = config.eddyStoneMap[EarSide.Left]
        eddyStoneMap[EarSide.Right] = config.eddyStoneMap[EarSide.Right]
    }
}