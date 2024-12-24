package com.alcanl.cleverear.util

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.core.content.ContextCompat.getString
import com.alcanl.cleverear.R
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Method

private const val DEVICE_NAME = 0
private const val DEVICE_MAC = 1
private const val DEVICE_RSSI = 2
private const val CONNECTION_STATE = 1
private const val BATTERY_STATE = 1
private const val EVENT_SENDER = 0
private const val MEMORY_STATE = 1
private const val VOLUME_STATE = 1
const val ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE = 60
const val ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE = 61
const val EDDYSTONE_SUPPORTED_AID_MODEL_NUMBER = "Biyonix E S-P"
const val MANUFACTURING_DATA = 3
const val MANUFACTURER_CODE_LEFT = "d60900"
const val MANUFACTURER_CODE_RIGHT = "d60901"
const val SHARED_PREF_DEVICE = "SHARED_PREF_DEVICE"
const val HA_LEFT_ADDRESS = "HA_LEFT_ADDRESS"
const val HA_RIGHT_ADDRESS = "HA_RIGHT_ADDRESS"
const val HA_LEFT_NAME = "HA_LEFT_NAME"
const val HA_RIGHT_NAME = "HA_RIGHT_NAME"
const val HA_LEFT_RSSI = "HA_LEFT_RSSI"
const val HA_RIGHT_RSSI = "HA_RIGHT_RSSI"
const val EMERGENCY_NUMBER = "EMERGENCY_NUMBER"
const val WHAT_BACKGROUND_CONNECTED = 0
const val WHAT_PRODUCT_CREATE_FAIL = 11
const val WHAT_LIBRARY_SEARCH_SUCCESS = 12
const val WHAT_LIBRARY_SEARCH_FAIL = 13
const val WHAT_LIBRARY_NOT_FOUND = 16
const val WHAT_READ_PARAMETERS_SUCCESS = 14
const val WHAT_READ_PARAMETERS_FAIL = 15
const val WHAT_LEFT_EAR_INITIALIZED = 17
const val WHAT_RIGHT_EAR_INITIALIZED = 18
const val WHAT_SHOW_IN_SITU_DIALOG = 19
const val WHAT_UPDATE_CHART = 20
const val WHAT_READ_PARAMETERS_PROCESS = 21
const val WHAT_CHANGE_FRAME_LAYOUTS = 22
const val WHAT_SOUND_GENERATE_ERROR = 23
const val WHAT_INVALIDATE_BINDING = 25
const val WHAT_INVALIDATE_CHART = 24
const val WHAT_CONNECTION_LOST = 26
const val WHAT_SHOW_LOADING_FRAME = 27
const val WHAT_HIDE_LOADING_FRAME = 28
const val WHAT_ENABLE_EQUALIZER_SLIDERS = 29
const val WHAT_DISABLE_EQUALIZER_SLIDERS = 30
const val TURKISH = "tr"
const val GERMAN = "de"
const val TURKISH_MANUAL_URL = """http://dosya.eartechnic.com/cleverear/cleverear_android_tr.pdf"""
const val ENGLISH_MANUAL_URL = """http://dosya.eartechnic.com/cleverear/cleverear_android_en.pdf"""
const val GERMAN_MANUAL_URL = """http://dosya.eartechnic.com/cleverear/cleverear_android_de.pdf"""


@Throws(JSONException::class)
fun parseJsonScanData(data: String, context: Context): DiscoveredDeviceInfo
{
    try {
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
    } catch (ex: NullPointerException) {
        throw JSONException(ex.cause)
    }
}
@Throws(JSONException::class)
fun parseJsonConnectionData(data: String, context: Context) : Int
{
    try {
        val jsonObject = JSONObject(data)
        val connectionEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
        return connectionEvent.getJSONObject(CONNECTION_STATE)
            .getInt(getString(context, R.string.device_connection_state_json))
    }catch (ex: NullPointerException) {
        throw JSONException(ex.cause)
    }
}

@Throws(JSONException::class)
fun parseJsonBatteryStateData(data: String, context: Context) : Int
{
    try {
        val jsonObject = JSONObject(data)
        val batteryEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
        return batteryEvent.getJSONObject(BATTERY_STATE).getInt(getString(context, R.string.battery_level_json))
    } catch (ex: NullPointerException) {
        throw JSONException(ex.cause)
    }

}
@Throws(JSONException::class)
fun parseJsonSenderMacData(data: String, context: Context) : String
{
    try {
        val jsonObject = JSONObject(data)
        val event = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
        return event.getJSONObject(EVENT_SENDER)
            .getString(getString(context, R.string.device_id_json))
    } catch (ex: NullPointerException) {
        throw JSONException(ex.cause)
    }
}
@Throws(JSONException::class)
fun parseJsonMemoryActiveData(data: String, context: Context) : Int
{
    try {
        val jsonObject = JSONObject(data)
        val memoryEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
        return memoryEvent.getJSONObject(MEMORY_STATE)
            .getInt(getString(context, R.string.current_memory_json))
    } catch (ex: NullPointerException) {
        throw JSONException(ex.cause)
    }
}
@Throws(JSONException::class)
fun parseJsonVolumeData(data: String, context: Context) : Int
{
    try {
        val jsonObject = JSONObject(data)
        val volumeEvent = jsonObject.getJSONArray(getString(context, R.string.event_json_obj))
        return volumeEvent.getJSONObject(VOLUME_STATE).getInt(getString(context, R.string.volume_level_json))
    } catch (ex: NullPointerException) {
        throw JSONException(ex.cause)
    }

}
fun isConnected(device: BluetoothDevice): Boolean
{
    return try {
        val m: Method = device.javaClass.getMethod("isConnected")
        m.invoke(device) as Boolean
    } catch (_: Exception) { false }
}

fun getSystemLanguage() : String
{
    return Locale.current.language
}

