package com.alcanl.cleverear.di.module

import android.content.Context
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.cleanConfigurations
import com.alcanl.cleverear.util.markServiceAsClosed
import com.alcanl.cleverear.util.markServiceAsRestarted
import com.alcanl.cleverear.util.saveConfigurations
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

    @Volatile
    var isAshaConnected = false

    @Volatile
    var isDoubleDeviceConnection = false

    var isDualDeviceConfig = false

    var isConnectedBefore = false

    @Volatile
    var isSystemRestartOnService = true

    @Volatile
    var isUserDisconnected = false

    var isEddyStoneScanInProgress = false

    var isRemoteFittingConnection = false

    @Volatile
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
                if (hearingAidMap.isEmpty())
                    false
                else hearingAidMap.all { it.value.isConnected }
        } catch (_ : Throwable) { false }
    }

    fun setServiceStopAndKeepConnectionIfRequired(context: Context)
    {
        if (isConnected && !keepConnectionForServiceDestroy) {
            hearingAidMap.forEach { it.value.endConnectionSession() }
            isConnected = false
        }

        if (isSystemRestartOnService) {
            saveConfigurations(this, context)
            markServiceAsRestarted(context)
        }
        else {
            cleanConfigurations(context)
            markServiceAsClosed(context)
        }

        isServiceOnBoard = false
    }
}