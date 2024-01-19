package com.alcanl.cleverear.entity

import android.util.Log
import com.alcanl.cleverear.helpers.EarSide
import com.ark.ArkException
import com.ark.CommunicationAdaptor
import com.ark.EventHandler
import com.ark.ParameterSpace
import com.ark.ProductManager
import com.ark.WirelessControl

data class HearingAid(val address: String, val name: String, val rssi: Int, val manufacturerData: String,
                      val side: EarSide, var isBond : Boolean = false, var numberOfMemories: Int = 8,
                      var volume: Int = 0, var batteryLevel : Int = 100, var communicationAdaptor: CommunicationAdaptor? = null,
                      var wirelessControl: WirelessControl? = null, var autoConnect : Boolean = false,
                      var currentMemory :  ParameterSpace? = null, var auxAttenuation : Int = 0,
                      var micAttenuation : Int = 8, var streamAddress : Int = 16,
                      var streamAddressRaw : Int = 1101286, var streamChannelList : Int = 1, )
{
    fun getPropertiesFromSDK(productManager: ProductManager, eventHandler: EventHandler)
    {
        communicationAdaptor = productManager.createWirelessCommunicationInterface(address)
        communicationAdaptor?.setEventHandler(eventHandler)
        wirelessControl = productManager.wirelessControl
        productManager.wirelessControl.setCommunicationAdaptor(communicationAdaptor)
        wirelessControl?.setCommunicationAdaptor(communicationAdaptor)

    }

    fun readDeviceConfiguration()
    {
        try {
            with(wirelessControl!!) {
                this@HearingAid.batteryLevel = batteryLevel
                this@HearingAid.volume = volume
                this@HearingAid.numberOfMemories = numberOfMemories
                this@HearingAid.currentMemory = currentMemory
                this@HearingAid.micAttenuation = micAttenuation
                this@HearingAid.auxAttenuation = auxAttenuation
                this@HearingAid.streamChannelList = streamChannelList
                this@HearingAid.streamAddressRaw = streamAddress
                this@HearingAid.streamAddress = streamAddressRaw shr 16
            }
        }
        catch (e : ArkException) {
            Log.e("Read Config Error", e.message.toString())
        }
    }
    fun endConnectionSession()
    {
        communicationAdaptor?.disconnect()
    }
}

