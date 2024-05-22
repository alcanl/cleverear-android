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
const val WHAT_BACKGROUND_CONNECTED = 0
const val WHAT_PRODUCT_CREATE_SUCCESS = 10
const val WHAT_PRODUCT_CREATE_FAIL = 11
const val WHAT_LIBRARY_SEARCH_SUCCESS = 12
const val WHAT_LIBRARY_SEARCH_FAIL = 13
const val WHAT_LIBRARY_NOT_FOUND = 16
const val WHAT_READ_PARAMETERS_SUCCESS = 14
const val WHAT_READ_PARAMETERS_FAIL = 15
const val TURKISH = "tr"
const val GERMAN = "de"
const val TURKISH_MANUAL_URL = """http://dosya.eartechnic.com/cleverear/cleverear_android_tr.pdf"""
const val ENGLISH_MANUAL_URL = """http://dosya.eartechnic.com/cleverear/cleverear_android_en.pdf"""
const val GERMAN_MANUAL_URL = """http://dosya.eartechnic.com/cleverear/cleverear_android_de.pdf"""
const val API_BASE_URL = "http://192.168.2.118:50531/api/ear-technic/"
const val API_LIB_INFO_GET_BY_HA_URL = "library/model/info"
const val API_LIB_INFO_GET_BY_LIB_ID_URL = "library"
const val API_PARAM_DEFAULT_DATA_GET_BY_HA = "param/model/data"
const val API_QUERY_NAME = "name"
const val API_QUERY_ID = "id"
const val API_QUERY_EMAIL = "email"
const val API_QUERY_PASS = "password"
const val API_ACTIVE_PARAM_DATA_GET_BY_USER = "user/param/active"


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
    } catch (ex: Exception) {
        throw IllegalStateException(ex)
    }
}

fun getSystemLanguage() : String
{
    return Locale.current.language
}

