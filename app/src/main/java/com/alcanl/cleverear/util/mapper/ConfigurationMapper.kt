package com.alcanl.cleverear.util.mapper

import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.ConfigurationData
import javax.inject.Inject

class ConfigurationMapper @Inject constructor() {

    fun toConfigurationModule(data: ConfigurationData, configuration: ConfigurationModule)
    {
        configuration.apply {

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
            ConfigurationData(canBondDeviceReConnect, keepConnectionForServiceDestroy,
                isBluetoothEnabled, isServiceOnBoard, trackLocationAlwaysSuccessFlag, isDoubleDeviceConnection,
                isDualDeviceConfig, isConnectedBefore, isUserDisconnected, isEddyStoneScanInProgress,
                isRemoteFittingConnection, isMainActivityOnScreen, earSide)
        }
    }
}