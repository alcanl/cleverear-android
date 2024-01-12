package com.alcanl.cleverear

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TableRow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityControlBinding
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.DOUBLE_DEVICE_MAP
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HEARING_AID
import com.alcanl.cleverear.helpers.IS_DOUBLE_DEVICE
import com.alcanl.cleverear.helpers.SHARED_PREF_FILE_NAME
import com.alcanl.cleverear.helpers.parseJsonBatteryStateData
import com.alcanl.cleverear.helpers.parseJsonSenderMacData
import com.alcanl.cleverear.helpers.parseJsonConnectionData
import com.alcanl.cleverear.helpers.parseJsonMemoryActiveData
import com.alcanl.cleverear.sdk.events.AuxChangeEvent
import com.alcanl.cleverear.sdk.events.BatteryStateChangedEvent
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.sdk.events.CurrentMemoryCharacteristicChangedEvent
import com.alcanl.cleverear.sdk.events.MicChangeEvent
import com.alcanl.cleverear.sdk.events.VolumeChangeEvent
import com.alcanl.cleverear.viewmodel.ControlActivityListenersViewModel
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.EventType
import com.ark.ParameterSpace
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class ControlActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityControlBinding
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool: ExecutorService
    @Volatile
    private var mIsConnected = false
    private var mIsDoubleDeviceConnection = false
    private lateinit var mHearingAid: HearingAid
    private lateinit var mDoubleDeviceMap : HashMap<*,*>
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        startEventListener()
        Handler(Looper.myLooper()!!).postDelayed({ mBinding.frameLayoutLoading.visibility = View.GONE
            initMemoryButtons(); initBatteryViews(); initSliders()}, 7000)
    }
    override fun onStart()
    {
        super.onStart()
        getDevicePropertiesFromSDK()

        if (!mIsDoubleDeviceConnection)
            connectForSingleDevice()
        else
            connectForDoubleDevices()
    }
    override fun onDestroy()
    {
        super.onDestroy()
        threadPool.shutdown()
    }
    private fun connectToDevice(hearingAid: HearingAid)
    {
        threadPool.execute {
            try {
                hearingAid.communicationAdaptor?.connect()
                runBlocking {
                    delay(5000)
                    readDeviceConfiguration(hearingAid)
                }

            }
            catch (ex: ArkException)
            {
                runOnUiThread {
                    Toast.makeText(this, "Problem occurred while connecting to device", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun connectForSingleDevice()
    {
        connectToDevice(mHearingAid)
    }
    private fun connectForDoubleDevices()
    {
        mDoubleDeviceMap.forEach { connectToDevice(it.value as HearingAid) }
    }
    private fun initBatteryViews()
    {
        if (mIsDoubleDeviceConnection) {
            mBinding.batteryViewLeftDevice.batteryLevel = (mDoubleDeviceMap[EarSide.Left] as HearingAid).batteryLevel
            mBinding.batteryViewRightDevice.batteryLevel = (mDoubleDeviceMap[EarSide.Right] as HearingAid).batteryLevel
            mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
            mBinding.batteryViewRightDevice.visibility = View.VISIBLE

        }
        else {
            if (mHearingAid.side == EarSide.Left) {
                mBinding.batteryViewLeftDevice.batteryLevel = mHearingAid.batteryLevel
                mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
            } else {
                mBinding.batteryViewRightDevice.batteryLevel = mHearingAid.batteryLevel
                mBinding.batteryViewRightDevice.visibility = View.VISIBLE
            }
        }
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_control)
        mBinding.viewModel = ControlActivityListenersViewModel(this)
        mBinding.isDualDevice = false

        if (!mIsDoubleDeviceConnection)
            mBinding.hearingAid = mHearingAid

        mBinding.isDoubleDeviceConnection = mIsDoubleDeviceConnection
    }
    private fun initDevice()
    {
        if (!mIsDoubleDeviceConnection)
            mHearingAid = when {
                VERSION.SDK_INT < VERSION_CODES.TIRAMISU -> intent.getSerializableExtra(HEARING_AID) as HearingAid
                else -> intent.getSerializableExtra(HEARING_AID, HearingAid::class.java)!!
            }
        else
            getDoubleDeviceInfo()
    }
    private fun initialize()
    {
        loadSharedPref()
        initDevice()
        initBinding()
    }
    private fun getDevicePropertiesFromSDK()
    {
        if (!mIsDoubleDeviceConnection)
            mHearingAid.getPropertiesFromSDK(productManager, eventHandler)
        else
            mDoubleDeviceMap.forEach { (it.value as HearingAid).getPropertiesFromSDK(productManager, eventHandler) }
    }
    private fun getDoubleDeviceInfo()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap = when {
                VERSION.SDK_INT < VERSION_CODES.TIRAMISU -> intent.getSerializableExtra(
                    DOUBLE_DEVICE_MAP) as HashMap<*, *>
                else -> intent.getSerializableExtra(DOUBLE_DEVICE_MAP, HashMap::class.java)!!
            }
    }
    private fun initMemoryButtons()
    {
        initMemoryButtons(if (!mIsDoubleDeviceConnection) mHearingAid else mDoubleDeviceMap[EarSide.Left] as HearingAid)
    }
    private fun initMemoryButtons(hearingAid: HearingAid)
    {
        hearingAid.numberOfMemories.also {

            if (it > 4) {
                initMemoryButtons(4, mBinding.tableLayout.getChildAt(0) as TableRow)
                initMemoryButtons(it - 4, mBinding.tableLayout.getChildAt(1) as TableRow)
            } else
                initMemoryButtons(it, mBinding.tableLayout.getChildAt(0) as TableRow)
        }
    }
    private fun initMemoryButtons(bound: Int, tableRow: TableRow)
    {
        (0..<bound).forEach { tableRow.getChildAt(it).isEnabled = true }
    }
    private fun initSliders()
    {
        if (mIsConnected)
            if (!mIsDoubleDeviceConnection)
                if (mHearingAid.side == EarSide.Left)
                    mBinding.sliderLeft.value = mHearingAid.volume.toFloat()
                else
                    mBinding.sliderRight.value = mHearingAid.volume.toFloat()
            else {
                mBinding.sliderLeft.value =
                    (mDoubleDeviceMap[EarSide.Left] as HearingAid).volume.toFloat()
                mBinding.sliderRight.value =
                    (mDoubleDeviceMap[EarSide.Right] as HearingAid).volume.toFloat()
            }
    }
    private fun loadSharedPref()
    {
        mIsDoubleDeviceConnection = getSharedPreferences(SHARED_PREF_FILE_NAME, MODE_PRIVATE)
            .getBoolean(IS_DOUBLE_DEVICE, false)
    }
    private fun memoryButtonsClickedCallback(hearingAid: HearingAid, parameterSpace: ParameterSpace)
    {
        hearingAid.wirelessControl?.currentMemory = parameterSpace
    }
    private fun readDeviceConfiguration(hearingAid: HearingAid)
    {
        hearingAid.readDeviceConfiguration()
    }
    private fun startEventListener()
    {
        threadPool.execute {
            while (true) {
                val event = eventHandler.event
                val eventSender = parseJsonSenderMacData(event.data, this)
                if (event.type == EventType.kConnectionEvent) {
                    Log.e("event", event.data)
                    Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
                    val connectionState = parseJsonConnectionData(event.data, this)
                    if (connectionState == 1) {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Connection Lost, Please Reconnect To Device",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        mIsConnected = false
                    }
                    else if (connectionState == 3) {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Connection Established With The Device",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        mIsConnected = true
                    }
                }
                if (event.type == EventType.kVolumeEvent) {
                    Log.e("event", event.data)
                    Log.e(VolumeChangeEvent::class.java.simpleName, event.data)

                }
                if (event.type == EventType.kExitEvent) {
                    Log.e("event", event.data)
                    Log.e("exit", event.data)
                }
                if (event.type == EventType.kBatteryEvent)
                {
                    Log.e("event", event.data)
                    Log.e(BatteryStateChangedEvent::class.java.simpleName, event.data)
                    val batteryState = parseJsonBatteryStateData(event.data, this)

                    runOnUiThread {
                        if (!mIsDoubleDeviceConnection)
                            if (mHearingAid.side == EarSide.Left)
                                mBinding.batteryViewLeftDevice.batteryLevel = batteryState
                            else
                                mBinding.batteryViewRightDevice.batteryLevel = batteryState
                        else
                            if (eventSender == (mDoubleDeviceMap[EarSide.Left] as HearingAid).address)
                                mBinding.batteryViewLeftDevice.batteryLevel = batteryState
                            else
                                mBinding.batteryViewRightDevice.batteryLevel = batteryState
                    }
                }
                if (event.type == EventType.kMemoryEvent)
                {
                    Log.e("event", event.data)
                    Log.e(CurrentMemoryCharacteristicChangedEvent::class.java.simpleName, event.data)
                    val memoryState = parseJsonMemoryActiveData(event.data, this)

                    threadPool.execute {
                        if (!mIsDoubleDeviceConnection)
                            mHearingAid.currentMemory = ParameterSpace.value(memoryState)
                        else
                            if (eventSender == (mDoubleDeviceMap[EarSide.Left] as HearingAid).address)
                                (mDoubleDeviceMap[EarSide.Left] as HearingAid).currentMemory = ParameterSpace.value(memoryState)
                            else
                                (mDoubleDeviceMap[EarSide.Right] as HearingAid).currentMemory = ParameterSpace.value(memoryState)
                    }
                }
                if (event.type == EventType.kActiveEvent)
                {
                    Log.e("event", event.data)
                    Log.e("active", event.data)
                }
                if (event.type == EventType.kUnknownEvent)
                {
                    Log.e("event", event.data)
                    Log.e("unknown", event.data)
                }
                if (event.type == EventType.kECMemoryEvent)
                {
                    Log.e("event", event.data)
                    Log.e("ecMemory", event.data)
                }
                if (event.type == EventType.kAuxAttenuationEvent)
                {
                    Log.e("event", event.data)
                    Log.e(AuxChangeEvent::class.java.simpleName, event.data)
                }
                if (event.type == EventType.kMicAttenuationEvent)
                {
                    Log.e("event", event.data)
                    Log.e(MicChangeEvent::class.java.simpleName, event.data)
                }
            }
        }
    }

    fun buttonOneClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory0) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory0)
    }
    fun buttonTwoClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory1) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory1)
    }
    fun buttonThreeClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory2) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory2)
    }
    fun buttonFourClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory3) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory3)
    }
    fun buttonFiveClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory4) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory4)
    }
    fun buttonSixClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory5) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory5)
    }
    fun buttonSevenClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory6) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory6)
    }
    fun buttonEightClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value as HearingAid, ParameterSpace.kNvmMemory7) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory7)
    }
    fun sliderLeftEarOnValueChanged(value: Float)
    {
        try {
            if (mIsConnected) {
                if (!mIsDoubleDeviceConnection) {
                    mHearingAid.wirelessControl?.volume = value.toInt()

                    if (mBinding.isDualDevice)
                        sliderRightEarOnValueChanged(value)
                } else
                    (mDoubleDeviceMap[EarSide.Left] as HearingAid).wirelessControl?.volume =
                        value.toInt()
            }
        }
        catch (_: ArkException) {}
    }
    fun sliderRightEarOnValueChanged(value: Float)
    {
        try {
            if (mIsConnected) {
                if (!mIsDoubleDeviceConnection) {
                    mHearingAid.wirelessControl?.volume = value.toInt()

                    if (mBinding.isDualDevice)
                        sliderRightEarOnValueChanged(value)
                } else
                    (mDoubleDeviceMap[EarSide.Right] as HearingAid).wirelessControl?.volume =
                        value.toInt()
            }
        } catch (_: ArkException) {}
    }
    fun switchOnCheckedChanged()
    {
        if (!mBinding.isDualDevice) {
            runOnUiThread {
                mBinding.sliderLeft.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_left_to_right_left_slider
                    )
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_right_to_left_right_slider
                    )
                )
            }
            mBinding.isDualDevice = true
            }
        else {
            runOnUiThread {
                mBinding.sliderLeft.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_right_to_left_left_slider
                    )
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_left_to_right_right_slider
                    )
                )
            }
            mBinding.isDualDevice = false
        }
    }
}