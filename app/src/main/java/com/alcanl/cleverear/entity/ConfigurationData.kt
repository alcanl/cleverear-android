package com.alcanl.cleverear.entity

import com.alcanl.cleverear.util.EarSide

data class ConfigurationData(val isConnected : Boolean, val canBondDeviceReConnect : Boolean,
                             val keepConnectionForServiceDestroy : Boolean, val isBluetoothEnabled : Boolean,
                             val isServiceOnBoard : Boolean, val trackLocationAlwaysSuccessFlag : Boolean,
                             val isDoubleDeviceConnection : Boolean, val isDualDeviceConfig : Boolean,
                             val isConnectedBefore : Boolean, val isUserDisconnected : Boolean,
                             val isEddyStoneScanInProgress : Boolean, val isRemoteFittingConnection : Boolean,
                             val isMainActivityOnScreen : Boolean, val earSide : EarSide,
                             val hearingAidLeft : HearingAid?, val hearingAidRight: HearingAid?,
                             val eddyStoneLeft : EddystoneHearingAid?, val eddyStoneRight: EddystoneHearingAid?
)
