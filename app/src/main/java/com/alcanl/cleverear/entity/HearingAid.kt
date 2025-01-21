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
            Log.e("SDKAdaptors", ex.message!!)
        }
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
                this@HearingAid.isEddystoneTLM = wirelessControl?.modelNumber == EDDYSTONE_SUPPORTED_AID_MODEL_NUMBER
            }
        }
        catch (e : ArkException) {
            Log.e("Read Config Error", e.message!!)
        }
    }

    fun getModelNumber() : String
    {
        return wirelessControl?.modelNumber ?: ""
    }

    fun endConnectionSession()
    {
        try {
            communicationAdaptor?.disconnect()
            isConnected = false
        } catch (_: ArkException) {}
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


