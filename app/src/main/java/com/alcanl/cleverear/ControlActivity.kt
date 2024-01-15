package com.alcanl.cleverear

import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.TableRow
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityControlBinding
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HEARING_AID
import com.alcanl.cleverear.helpers.IS_DOUBLE_DEVICE
import com.alcanl.cleverear.helpers.SHARED_PREF_FILE_NAME
import com.alcanl.cleverear.helpers.thread.ConnectedDeviceEventHandler
import com.alcanl.cleverear.viewmodel.ControlActivityListenersViewModel
import com.ark.ArkException
import com.ark.ParameterSpace
import com.google.android.material.slider.Slider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class ControlActivity : BaseActivity() {
    lateinit var mBinding: ActivityControlBinding
    @Volatile
    var mIsConnected = false
    var mIsDoubleDeviceConnection = false
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        ConnectedDeviceEventHandler(this, eventHandler, threadPool).startEventListener()
        Handler(Looper.myLooper()!!).postDelayed({ mBinding.frameLayoutLoading.visibility = View.GONE
            initMemoryButtons(); initSliders(); initBatteryViews()}, 7000)
    }

    override fun onResume()
    {
        super.onResume()
        mBinding.bottomNavigationView.selectedItemId = R.id.navbar_control
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
    override fun onDestroy() {
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
        mDoubleDeviceMap.forEach { connectToDevice(it.value) }
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
    }
    private fun initialize()
    {
        loadSharedPref()
        initDevice()
        initBinding()
        initializeNavigationView()
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> true
                R.id.navbar_connection -> { if (mIsConnected) {
                    startActivity(Intent(this, ConnectionActivity::class.java))
                    overridePendingTransition(R.anim.slide_out, R.anim.slide_in); true
                } else false
                }
                else -> {
                    if (mIsConnected) {
                        startActivity(Intent(this, AdvancedActivity::class.java))
                        overridePendingTransition(R.anim.slide_in, R.anim.slide_out); true
                    } else false
                }
            }
        }
    }
    private fun getDevicePropertiesFromSDK()
    {
        if (!mIsDoubleDeviceConnection)
            mHearingAid.getPropertiesFromSDK(productManager, eventHandler)
        else
            mDoubleDeviceMap.forEach { it.value.getPropertiesFromSDK(productManager, eventHandler) }
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
    fun buttonOneClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory0) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory0)
    }
    fun buttonTwoClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory1) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory1)
    }
    fun buttonThreeClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory2) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory2)
    }
    fun buttonFourClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory3) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory3)
    }
    fun buttonFiveClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory4) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory4)
    }
    fun buttonSixClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory5) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory5)
    }
    fun buttonSevenClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory6) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory6)
    }
    fun buttonEightClicked()
    {
        if (mIsDoubleDeviceConnection)
            mDoubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory7) }
        else
            memoryButtonsClickedCallback(mHearingAid, ParameterSpace.kNvmMemory7)
    }
    fun sliderLeftEarOnValueChanged(value: Float)
    {
        try {
            if (mIsConnected) {
                if (!mIsDoubleDeviceConnection) {
                    mHearingAid.wirelessControl?.volume = value.toInt()

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
                    ).also { it.setAnimationListener(setSlidersFirstAnimationEnd(mBinding.sliderLeft)) }
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_right_to_left_right_slider
                    ).also { it.setAnimationListener(setSlidersFirstAnimationEnd(mBinding.sliderRight)) }
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
                    ).also { it.setAnimationListener(setSlidersSecondAnimationEnd(mBinding.sliderLeft)) }
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_left_to_right_right_slider
                    ).also { it.setAnimationListener(setSlidersSecondAnimationEnd(mBinding.sliderRight)) }
                )

            }
            mBinding.isDualDevice = false
        }
    }
    fun sliderDualOnValueChanged(value: Float)
    {
        threadPool.execute { sliderLeftEarOnValueChanged(value) }
        threadPool.execute { sliderRightEarOnValueChanged(value) }
    }
    private fun setSlidersFirstAnimationEnd(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                slider.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animation?)
            {

                slider.visibility = View.INVISIBLE
                mBinding.textViewLeftSeekBar.visibility = View.INVISIBLE
                mBinding.textViewRightSeekBar.visibility = View.INVISIBLE

                if (mBinding.sliderCommon.visibility != View.VISIBLE) {
                    mBinding.sliderCommon.startAnimation(
                        android.view.animation.AnimationUtils.loadAnimation(
                            this@ControlActivity, R.anim.alpha_blink_anim
                        ).also {it.setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                                mBinding.sliderCommon.visibility = View.VISIBLE
                                mBinding.sliderCommon.isEnabled = true
                            }

                            override fun onAnimationEnd(animation: Animation?) {

                            }

                            override fun onAnimationRepeat(animation: Animation?) {

                            }
                        })

                        })
                }
            }
            override fun onAnimationRepeat(animation: Animation?)
            {

            }
        }
    }
    private fun setSlidersSecondAnimationEnd(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                mBinding.sliderCommon.visibility = View.GONE
                mBinding.sliderCommon.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animation?)
            {
                slider.visibility = View.VISIBLE
                slider.isEnabled = true
                mBinding.textViewRightSeekBar.visibility = View.VISIBLE
                mBinding.textViewLeftSeekBar.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }
        }
    }
}