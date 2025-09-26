package com.alcanl.cleverear.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.alcanl.cleverear.api.PREF_ACTIVE_PARAM_ID
import com.alcanl.cleverear.api.PREF_DEFAULT_PARAM_ID
import com.alcanl.cleverear.api.PREF_DEVICE_SERIAL
import com.alcanl.cleverear.api.PREF_EQUALIZER_ID
import com.alcanl.cleverear.api.PREF_EQUALIZER_VALUES
import com.alcanl.cleverear.api.PREF_EQUALIZER_VALUES_ID
import com.alcanl.cleverear.api.PREF_EQUALIZER_VALUES_MODEL_SERIAL
import com.alcanl.cleverear.api.PREF_FREQUENCY_CHANNEL_COUNT
import com.alcanl.cleverear.api.PREF_LIBRARY_ID
import com.alcanl.cleverear.api.PREF_MODEL_NUMBER
import com.alcanl.cleverear.api.PREF_WDRC_CHANNEL_COUNT
import com.alcanl.cleverear.api.aid.HearingAidDTO
import com.alcanl.cleverear.api.equalizer.EqualizerValuesDTO
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.entity.ConfigurationData
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.util.mapper.ConfigurationMapper
import com.google.gson.Gson
import androidx.core.content.edit
import java.time.LocalDate

private fun getPrivatePrefs(context: Context) = context.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)
fun getLeftHearingAidAddress(context: Context) = getPrivatePrefs(context).getString(HA_LEFT_ADDRESS, "")!!
fun getRightHearingAidAddress(context: Context) = getPrivatePrefs(context).getString(HA_RIGHT_ADDRESS, "")!!
fun getLeftHearingAidModelNumber(context: Context) = getPrivatePrefs(context).getString(HA_LEFT_NAME, "")!!
fun getRightHearingAidModelNumber(context: Context) = getPrivatePrefs(context).getString(HA_RIGHT_NAME, "")!!
fun saveHearingAidInfoToPrefs(context: Context, hearingAid: HearingAid)
{
    getPrivatePrefs(context).edit {

        putString(
            if (hearingAid.side == EarSide.Left) HA_LEFT_ADDRESS else HA_RIGHT_ADDRESS,
            hearingAid.address
        )
        putString(
            if (hearingAid.side == EarSide.Left) HA_LEFT_NAME else HA_RIGHT_NAME,
            hearingAid.name
        )
        putInt(
            if (hearingAid.side == EarSide.Left) HA_LEFT_RSSI else HA_RIGHT_RSSI,
            hearingAid.rssi
        )
        putString(
            if (hearingAid.side == EarSide.Left) MANUFACTURER_CODE_LEFT else MANUFACTURER_CODE_RIGHT,
            hearingAid.manufacturerData
        )
        putBoolean(IS_EDDYSTONE_TLM, hearingAid.isEddystoneTLM)

    }
}
fun getLeftSideHearingAid(context: Context) : HearingAid
{
    val prefs = getPrivatePrefs(context)

    val deviceAddress = prefs.getString(HA_LEFT_ADDRESS, "")
    val deviceName = prefs.getString(HA_LEFT_NAME, "")
    val deviceRSSI = prefs.getInt(HA_LEFT_RSSI, 0)
    val deviceManu = prefs.getString(MANUFACTURER_CODE_LEFT, "")
    val isEddystone = prefs.getBoolean(IS_EDDYSTONE_TLM, false)

    return HearingAid(deviceAddress!!, deviceName!!, deviceRSSI, deviceManu!!, EarSide.Left, isEddystoneTLM = isEddystone)
}
fun getRightSideHearingAid(context: Context) : HearingAid
{
    val prefs = getPrivatePrefs(context)

    val deviceAddress = prefs.getString(HA_RIGHT_ADDRESS, "")
    val deviceName = prefs.getString(HA_RIGHT_NAME, "")
    val deviceRSSI = prefs.getInt(HA_RIGHT_RSSI, 0)
    val deviceManu = prefs.getString(MANUFACTURER_CODE_RIGHT, "")
    val isEddystone = prefs.getBoolean(IS_EDDYSTONE_TLM, false)

    return HearingAid(deviceAddress!!, deviceName!!, deviceRSSI, deviceManu!!, EarSide.Right, isEddystoneTLM =  isEddystone)

}
fun saveEmergencyNumberToPrefs(context: Context, phoneNumber: String)
{
    getPrivatePrefs(context).edit {
        putString(EMERGENCY_NUMBER, phoneNumber)
    }
}
fun getEmergencyNumber(context: Context) : String = getPrivatePrefs(context).getString(EMERGENCY_NUMBER, "")!!
fun getHearingAidSerialNumber(context: Context, configuration: ConfigurationModule, fittingConfig: FittingConfigurationModule) : String
{
    return getPrivatePrefs(context).getString(String.format(PREF_DEVICE_SERIAL, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()), "")!!
}
fun saveHearingAidDTOPref(context: Context, configuration : ConfigurationModule, fittingConfig: FittingConfigurationModule, hearingAidDTO: HearingAidDTO?)
{
    if (hearingAidDTO == null)
        return

    getPrivatePrefs(context).edit {
        putString(
            String.format(
                PREF_DEVICE_SERIAL,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), hearingAidDTO.modelName
        )
        putString(
            String.format(
                PREF_LIBRARY_ID,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), Integer.toHexString(hearingAidDTO.libraryId.toInt())
        )
        putString(
            String.format(
                PREF_ACTIVE_PARAM_ID,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), hearingAidDTO.activeParamId
        )
        putString(
            String.format(
                PREF_DEFAULT_PARAM_ID,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), hearingAidDTO.defaultParamId
        )
        putInt(
            String.format(
                PREF_WDRC_CHANNEL_COUNT,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), hearingAidDTO.wdrcChannelCount
        )
        putInt(
            String.format(
                PREF_FREQUENCY_CHANNEL_COUNT,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), hearingAidDTO.frequencyChannelCount
        )
        putInt(
            String.format(
                PREF_MODEL_NUMBER,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), hearingAidDTO.modelNumber
        )
        putLong(
            String.format(
                PREF_EQUALIZER_VALUES_ID,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), hearingAidDTO.equalizerValuesId
        )
    }
}

fun getHearingAidDTOFromPrefs(context: Context, configuration: ConfigurationModule, fittingConfig: FittingConfigurationModule) : HearingAidDTO
{
    val pref = getPrivatePrefs(context)

    return HearingAidDTO(pref.getString(String.format(PREF_DEVICE_SERIAL,
        configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()), "")!!,
        pref.getInt(String.format(PREF_MODEL_NUMBER, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()), 0),
        pref.getString(String.format(PREF_LIBRARY_ID, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()),
            "")!!, pref.getString(String.format(
            PREF_DEFAULT_PARAM_ID, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()),
            "")!!,
        pref.getString(String.format(PREF_ACTIVE_PARAM_ID, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()),
            "")!!, pref.getInt(String.format(
            PREF_WDRC_CHANNEL_COUNT, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()),
            0),
        pref.getInt(String.format(PREF_FREQUENCY_CHANNEL_COUNT, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()),
            0), pref.getLong(String.format(
            PREF_EQUALIZER_VALUES_ID, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()), 0)
    )
}

@SuppressLint("DefaultLocale")
fun saveEqualizerDTOPref(context: Context, configuration: ConfigurationModule, fittingConfig: FittingConfigurationModule, equalizerValuesDTO: EqualizerValuesDTO?) {
    if (equalizerValuesDTO == null)
        return

    getPrivatePrefs(context).edit {
        putLong(
            String.format(
                PREF_EQUALIZER_ID,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), equalizerValuesDTO.id
        )
        putString(
            String.format(
                PREF_EQUALIZER_VALUES_MODEL_SERIAL,
                configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()
            ), equalizerValuesDTO.equalizerValuesName
        )
        equalizerValuesDTO.valueList.forEachIndexed { index, i ->
            putInt(
                String.format(
                    PREF_EQUALIZER_VALUES,
                    configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString(),
                    index
                ), i
            )
        }
    }
}

fun getEqualizerValuesModelSerial(context: Context, configuration: ConfigurationModule, fittingConfig: FittingConfigurationModule) : String
{
    return getPrivatePrefs(context).getString(String.format(PREF_EQUALIZER_VALUES_MODEL_SERIAL, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()), "")!!
}

@SuppressLint("DefaultLocale")
fun getEqualizerDTOPref(context: Context, configuration: ConfigurationModule, fittingConfig: FittingConfigurationModule, count: Int) : EqualizerValuesDTO {

    val prefs = getPrivatePrefs(context)
    val valuesList = mutableListOf<Int>()

    (0..< count).forEach {
        valuesList.add(prefs.getInt(String.format(PREF_EQUALIZER_VALUES, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString(), it), 0))
    }

    return EqualizerValuesDTO(
        prefs.getLong(String.format(PREF_EQUALIZER_ID, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()), 0),
        valuesList,
        prefs.getString(String.format(PREF_EQUALIZER_VALUES_MODEL_SERIAL, configuration.hearingAidMap[fittingConfig.selectedEarSide]?.toString()), "")!!
    )
}

fun cleanConfigurations(context: Context)
{
    getPrivatePrefs(context).edit {
        putString(CONFIGURATION_JSON, "")
    }
}

fun markServiceAsRestarted(context: Context)
{
    getPrivatePrefs(context).edit {
        putBoolean(SERVICE_RESTART_FLAG, true)
    }
}

fun markServiceAsClosed(context: Context)
{

    getPrivatePrefs(context).edit {
        putBoolean(SERVICE_RESTART_FLAG, false).apply()
    }
}

fun isServiceRestarted(context: Context) : Boolean
{
    val prefs = getPrivatePrefs(context)
    return prefs.getBoolean(SERVICE_RESTART_FLAG, false)
}

fun saveConfigurations(configuration: ConfigurationModule, context: Context)
{
    getPrivatePrefs(context).edit {
        val mapper = ConfigurationMapper()
        putString(CONFIGURATION_JSON, Gson().toJson(mapper.toConfigurationData(configuration)))
    }
}

fun readConfigurations(configuration: ConfigurationModule, context: Context)
{
    val prefs = getPrivatePrefs(context)
    val json = prefs.getString(CONFIGURATION_JSON, "")!!

    if (json.isNotBlank()) {
        val gson = Gson()
        val mapper = ConfigurationMapper()
        val data = gson.fromJson(json, ConfigurationData::class.java)
        mapper.toConfigurationModule(data, configuration)
    }
}

fun determineHearingAidObjects(configuration: ConfigurationModule, context: Context)
{
    configuration.apply {
       if (getLeftHearingAidAddress(context).isNotBlank())
           hearingAidMap[EarSide.Left] = getLeftSideHearingAid(context)

       if (getRightHearingAidAddress(context).isNotBlank())
           hearingAidMap[EarSide.Right] = getRightSideHearingAid(context)
    }
}

fun saveStepCount(stepCount: Long, context: Context, deviceMac: String)
{
    getPrivatePrefs(context).edit {
        putLong(STEP_COUNT + deviceMac + LocalDate.now(), stepCount)
    }
}

fun readStepCount(context: Context, deviceMac: String) = getPrivatePrefs(context).getLong(STEP_COUNT + deviceMac + LocalDate.now(), 0)