package com.alcanl.cleverear.entity

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.alcanl.cleverear.util.EDDYSTONE_SUPPORTED_AID_MODEL_NUMBER
import com.alcanl.cleverear.util.EarSide
import com.ark.ArkException
import com.ark.CommunicationAdaptor
import com.ark.EventHandler
import com.ark.ParameterList
import com.ark.ParameterMemory
import com.ark.ParameterMemoryList
import com.ark.ParameterSpace
import com.ark.ProductManager
import com.ark.WirelessControl
import java.util.Objects

data class HearingAid(val address: String, val name: String, val rssi: Int, val manufacturerData: String,
                      val side: EarSide, var bondStatus : Int = BluetoothDevice.BOND_NONE, var numberOfMemories: Int = 8,
                      var volume: Int = 45, var batteryLevel : Int = 100, var communicationAdaptor: CommunicationAdaptor? = null,
                      var wirelessControl: WirelessControl? = null, var currentMemory :  ParameterSpace = ParameterSpace.kNvmMemory0,
                      var auxAttenuation : Int = 45, var micAttenuation : Int = 8, var streamAddress : Int = 16,
                      var streamAddressRaw : Int = 1101286, var streamChannelList : Int = 1,
                      var memories: ParameterMemoryList? = null, var parameters: ParameterList? = null,
                      val systemMemory: ParameterMemory? = null, @Volatile var isConnected : Boolean = false,
                      @Volatile var isDeviceInRange: Boolean = false, var isEddystoneTLM : Boolean = false)
{

    fun getPropertiesFromSDK(productManager: ProductManager, eventHandler: EventHandler)
    {
        try {
            communicationAdaptor = productManager.createWirelessCommunicationInterface(address)
            communicationAdaptor?.isVerifyNvmWrites = false
            communicationAdaptor?.setEventHandler(eventHandler)
            wirelessControl = productManager.wirelessControl
            productManager.wirelessControl.setCommunicationAdaptor(communicationAdaptor)
            wirelessControl?.setCommunicationAdaptor(communicationAdaptor)

        } catch (ex: ArkException) {
            Log.e(HearingAid::class.simpleName, ex.message!!)
        }
    }

    fun readDeviceConfiguration()
    {
        try {
            wirelessControl?.let {
                batteryLevel = it.batteryLevel
                volume = it.volume
                numberOfMemories = it.numberOfMemories
                currentMemory = it.currentMemory
                micAttenuation = it.micAttenuation
                auxAttenuation = it.auxAttenuation
                streamChannelList = it.streamChannelList
                streamAddressRaw = it.streamAddress
                streamAddress = streamAddressRaw shr 16
                isEddystoneTLM = true //it.modelNumber == EDDYSTONE_SUPPORTED_AID_MODEL_NUMBER
            }
        }
        catch (e : ArkException) {
            Log.e(HearingAid::class.simpleName, e.message!!)
        }
    }

    fun getModelNumber() : String
    {
        return try {
            return wirelessControl?.modelNumber ?: ""
        } catch (_ : ArkException) { "" }
    }

    fun endConnectionSession()
    {
        try {
            communicationAdaptor?.disconnect()
            isConnected = false
        } catch (ex: ArkException) {
            Log.e(HearingAid::class.simpleName, ex.message!!)
        }
    }

    override fun equals(other: Any?): Boolean
    {
        return other is HearingAid && address == other.address && name == other.name && rssi == other.rssi
    }

    override fun hashCode(): Int
    {
        return Objects.hashCode(address)
    }

    override fun toString(): String
    {
        return name
    }
}


