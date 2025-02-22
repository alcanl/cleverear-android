package com.alcanl.cleverear.util.mapper

import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.ConfigurationData
import com.alcanl.cleverear.util.EarSide
import javax.inject.Inject

class ConfigurationMapper @Inject constructor() {

    fun toConfigurationModule(data: ConfigurationData, configuration: ConfigurationModule)
    {
        configuration.apply {
            if (data.hearingAidLeft != null)
                hearingAidMap[EarSide.Left] = data.hearingAidLeft

            if (data.hearingAidRight != null)
                hearingAidMap[EarSide.Right] = data.hearingAidRight

            if (data.eddyStoneLeft != null)
                eddyStoneMap[EarSide.Left] = data.eddyStoneLeft

            if (data.eddyStoneRight != null)
                eddyStoneMap[EarSide.Right] = data.eddyStoneRight

            isConnected = data.isConnected
            isUserDisconnected = data.isUserDisconnected
            isBluetoothEnabled = data.isBluetoothEnabled
            isServiceOnBoard = data.isServiceOnBoard
            isConnectedBefore = data.isConnectedBefore
            isDoubleDeviceConnection = data.isDoubleDeviceConnection
            isDualDeviceConfig = data.isDualDeviceConfig
            isEddyStoneScanInProgress = data.isEddyStoneScanInProgress
            isMainActivityOnScreen = data.isMainActivityOnScreen
            isRemoteFittingConnection = data.isRemoteFittingConnection
            canBondDeviceReConnect = data.canBondDeviceReConnect
            keepConnectionForServiceDestroy = data.keepConnectionForServiceDestroy
            trackLocationAlwaysSuccessFlag = data.trackLocationAlwaysSuccessFlag
            earSide = data.earSide
        }

    }

    fun toConfigurationData(configuration: ConfigurationModule) : ConfigurationData
    {
        return with(configuration) {
            ConfigurationData(isConnected, canBondDeviceReConnect, keepConnectionForServiceDestroy,
                isBluetoothEnabled, isServiceOnBoard, trackLocationAlwaysSuccessFlag, isDoubleDeviceConnection,
                isDualDeviceConfig, isConnectedBefore, isUserDisconnected, isEddyStoneScanInProgress,
                isRemoteFittingConnection, isMainActivityOnScreen, earSide,
                hearingAidMap[EarSide.Left], hearingAidMap[EarSide.Right],
                eddyStoneMap[EarSide.Left], eddyStoneMap[EarSide.Right])
        }
    }
}