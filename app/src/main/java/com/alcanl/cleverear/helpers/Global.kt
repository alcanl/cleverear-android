package com.alcanl.cleverear.helpers

import android.content.Context
import androidx.core.content.ContextCompat.getString
import com.alcanl.cleverear.R
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import org.json.JSONException
import org.json.JSONObject

private const val DEVICE_NAME = 0
private const val DEVICE_MAC = 1
private const val DEVICE_RSSI = 2
private const val CONNECTION_STATE = 1
private const val BATTERY_STATE = 1
private const val EVENT_SENDER = 0
private const val MEMORY_STATE = 1
private const val VOLUME_STATE = 1
const val MANUFACTURING_DATA = 3
const val MANUFACTURER_CODE_LEFT = "d60900"
const val MANUFACTURER_CODE_RIGHT = "d60901"
const val DEVICE_NAME_PREF = "EarnetDevice"
const val DEVICE_NAME_EDITED = "Earnet"
const val DEVICE_NAME_KEY = "EarnetKey"
const val DEVICE_RSSI_KEY = "EarnetRssi"
const val DEVICE_MAC_KEY = "EarnetMac"
const val DEVICE_MANU_KEY = "EarnetManu"
const val DEVICE_SIDE_KEY = "EarnetSide"
const val DEVICE_NAME_DEFAULT = "EARNET"


@Throws(JSONException::class)
fun parseJsonScanData(data: String, context: Context): DiscoveredDeviceInfo
{
    val jsonObject = JSONObject(data)
    val scanEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
    val deviceName = scanEvent.getJSONObject(DEVICE_NAME)
    val deviceMAC = scanEvent.getJSONObject(DEVICE_MAC)
    val deviceRSSI = scanEvent.getJSONObject(DEVICE_RSSI)
    val manufacturingData = scanEvent.getJSONObject(MANUFACTURING_DATA)

    return DiscoveredDeviceInfo(
        deviceMAC.getString(getString(context, R.string.device_id_json)),
        deviceName.getString(getString(context, R.string.device_name_json)),
        deviceRSSI.getInt(getString(context, R.string.device_rssi_json)),
        manufacturingData.getString(getString(context, R.string.device_manu_data_json))
    )
}
@Throws(JSONException::class)
fun parseJsonConnectionData(data: String, context: Context) : Int
{
    val jsonObject = JSONObject(data)
    val connectionEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
    return connectionEvent.getJSONObject(CONNECTION_STATE).getInt(getString(context, R.string.device_connection_state_json))
}

@Throws(JSONException::class)
fun parseJsonBatteryStateData(data: String, context: Context) : Int
{
    val jsonObject = JSONObject(data)
    val batteryEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
    return batteryEvent.getJSONObject(BATTERY_STATE).getInt(getString(context, R.string.battery_level_json))
}
@Throws(JSONException::class)
fun parseJsonSenderMacData(data: String, context: Context) : String
{
    val jsonObject = JSONObject(data)
    val batteryEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
    return batteryEvent.getJSONObject(EVENT_SENDER).getString(getString(context, R.string.device_id_json))
}
@Throws(JSONException::class)
fun parseJsonMemoryActiveData(data: String, context: Context) : Int
{
    val jsonObject = JSONObject(data)
    val memoryEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
    return memoryEvent.getJSONObject(MEMORY_STATE).getInt(getString(context, R.string.current_memory))
}
@Throws(JSONException::class)
fun parseJsonVolumeData(data: String, context: Context) : Int
{
    val jsonObject = JSONObject(data)
    val volumeEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
    return volumeEvent.getJSONObject(VOLUME_STATE).getInt(getString(context, R.string.volume_level))
}

