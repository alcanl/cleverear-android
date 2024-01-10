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
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HEARING_AID
import com.alcanl.cleverear.helpers.parseJsonBatteryData
import com.alcanl.cleverear.helpers.parseJsonConnectionData
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
import javax.inject.Inject
import kotlin.concurrent.thread

@AndroidEntryPoint
class ControlActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityControlBinding
    @Inject
    lateinit var mProductManager: ProductManager
    @Inject
    lateinit var mEventHandler: EventHandler
    @Volatile
    private var isConnected = false
    private lateinit var mHearingAid: HearingAid
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        startEventListener()
        Handler(Looper.myLooper()!!).postDelayed({ mBinding.frameLayoutLoading.visibility = View.GONE; initMemoryButtons(); initBatteryViews()}, 6000)
    }
    override fun onStart()
    {
        super.onStart()
        getDevicePropertiesFromSDK()
        connectToDevice()

    }
    private fun connectToDevice()
    {

        thread(isDaemon = false, start = true) {
            try {
                mHearingAid.communicationAdaptor?.connect()
                runBlocking {
                    delay(5000)
                    readDeviceConfiguration()
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
    private fun initBatteryViews()
    {
        if (mHearingAid.side == EarSide.Left) {
            mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
            mBinding.batteryViewLeftDevice.batteryLevel = mHearingAid.batteryLevel
        }
        else {
            mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
            mBinding.batteryViewRightDevice.batteryLevel = mHearingAid.batteryLevel
        }
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_control)
        mBinding.viewModel = ControlActivityListenersViewModel(this)
        mBinding.isDualDevice = false
        mBinding.hearingAid = mHearingAid
    }
    private fun initDevice()
    {
        mHearingAid = when {
            VERSION.SDK_INT < VERSION_CODES.TIRAMISU -> intent.getSerializableExtra(HEARING_AID) as HearingAid
            else -> intent.getSerializableExtra(HEARING_AID, HearingAid::class.java)!!
        }
    }
    private fun initialize()
    {
        initDevice()
        initBinding()
    }
    private fun initMemoryButtons()
    {
        mHearingAid.numberOfMemories.also {

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
    private fun getDevicePropertiesFromSDK()
    {
        mHearingAid.getPropertiesFromSDK(mProductManager, mEventHandler)
    }

    private fun readDeviceConfiguration()
    {
        mHearingAid.readDeviceConfiguration()
    }
    private fun startEventListener()
    {
       thread(isDaemon = false, start = true) {
            while (true) {
                val event = mEventHandler.event
                if (event.type == EventType.kConnectionEvent) {
                    Log.e("event", event.data)
                    Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
                    val connectionState = parseJsonConnectionData(event.data, this)
                    if ( connectionState == 1) {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Connection Lost, Please Reconnect To Device",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        isConnected = false
                    }
                    else if (connectionState == 3) {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Connection Established With The Device",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        isConnected = true
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
                    val batteryState = parseJsonBatteryData(event.data, this)

                    runOnUiThread {
                        if (mHearingAid.side == EarSide.Left)
                            mBinding.batteryViewLeftDevice.batteryLevel = batteryState
                        else
                            mBinding.batteryViewRightDevice.batteryLevel = batteryState
                    }
                }
                if (event.type == EventType.kMemoryEvent)
                {
                    Log.e("event", event.data)
                    Log.e(CurrentMemoryCharacteristicChangedEvent::class.java.simpleName, event.data)
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
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory0
    }
    fun buttonTwoClicked()
    {
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory1
    }
    fun buttonThreeClicked()
    {
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory2
    }
    fun buttonFourClicked()
    {
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory3
    }
    fun buttonFiveClicked()
    {
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory4
    }
    fun buttonSixClicked()
    {
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory5
    }
    fun buttonSevenClicked()
    {
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory6
    }
    fun buttonEightClicked()
    {
        mHearingAid.wirelessControl?.currentMemory = ParameterSpace.kNvmMemory7
    }
    fun sliderLeftEarOnValueChanged(value: Float)
    {
        if (isConnected) {
            mHearingAid.wirelessControl?.volume = value.toInt()

            if (mBinding.isDualDevice)
                sliderRightEarOnValueChanged(value)
        }

    }
    fun sliderRightEarOnValueChanged(value: Float)
    {
        if (isConnected)
            mHearingAid.wirelessControl?.volume = value.toInt()
    }
    fun switchOnCheckedChanged()
    {
        if (mBinding.isDualDevice) {
            try {
                mBinding.frameLayoutLoading.visibility = View.VISIBLE
                mHearingAid.pairDevices()
                Handler(Looper.myLooper()!!).postDelayed({ mBinding.frameLayoutLoading.visibility = View.GONE}, 5000)
            }
            catch (_ : ArkException)
            {
                mBinding.isDualDevice = false
                Toast.makeText(this, "Cannot Detect The Second Device For Pairing",
                    Toast.LENGTH_LONG).show()
            }
            mBinding.sliderLeft.isEnabled = true
            mBinding.sliderRight.isEnabled = false

            if (mBinding.batteryViewLeftDevice.visibility == View.INVISIBLE)
                mBinding.batteryViewLeftDevice.visibility = View.VISIBLE

            if (mBinding.batteryViewRightDevice.visibility == View.INVISIBLE)
                mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
        }
        else {
            mBinding.sliderLeft.isEnabled = mHearingAid.side == EarSide.Left
            mBinding.sliderRight.isEnabled = mHearingAid.side == EarSide.Right
        }
    }
}