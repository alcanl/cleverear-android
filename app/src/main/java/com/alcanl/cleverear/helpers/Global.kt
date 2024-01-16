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
const val BUNDLE_EAR_SIDE_KEY = "earSide"
const val RIGHT = "right"
const val LEFT = "left"
const val HEARING_AID = "selected_device"
const val DOUBLE_DEVICE_SELECTION = "double_device_selection"
const val DOUBLE_DEVICE_MAP = "double_device_map"
const val SHARED_PREF_FILE_NAME = "double_device_flag"
const val IS_DOUBLE_DEVICE = "is_double_device"

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

