package com.alcanl.cleverear

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.Button
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityControlBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.listeners.ControlActivityEventListener
import com.alcanl.cleverear.viewmodel.ControlActivityListenersViewModel
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ParameterSpace
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject
@AndroidEntryPoint
class ControlActivity : AppCompatActivity() {
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var configuration : ConfigurationModule

    lateinit var mBinding: ActivityControlBinding
    private lateinit var mListener : ControlActivityEventListener
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        mListener =
            ControlActivityEventListener(this, eventHandler, threadPool, configuration).apply {
                startEventListener()
            }
    }
    override fun onResume()
    {
        super.onResume()
        if (!configuration.isConnected) {
            finish()
            return
        }
        mBinding.bottomNavigationView.selectedItemId = R.id.navbar_control
        runOnUiThread { initSliders() }
        runOnUiThread { initTextViews() }
        runOnUiThread { initActiveMemory(if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap[EarSide.Left]?.currentMemory?.value()!!
        else
            configuration.hearingAid.currentMemory?.value()!!
        ) }

        if (!mListener.isListenerOnBoard())
            mListener =
                ControlActivityEventListener(this, eventHandler, threadPool, configuration).apply {
                    startEventListener()
                }
    }
    override fun onStop()
    {
        super.onStop()

        if (mListener.isListenerOnBoard())
            mListener.stopListener()

    }
    override fun onDestroy() {
        super.onDestroy()

        if (mListener.isListenerOnBoard())
            mListener.stopListener()

    }
    private fun initBatteryViews()
    {
        if (configuration.isDoubleDeviceConnection) {
            with(configuration.doubleDeviceMap) {

                mBinding.batteryViewLeftDevice. apply {
                    batteryLevel = this@with[EarSide.Left]?.batteryLevel!!
                    mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
                }
                mBinding.batteryViewRightDevice.apply {
                    batteryLevel = this@with[EarSide.Right]?.batteryLevel!!
                    mBinding.batteryViewRightDevice.visibility = View.VISIBLE
                }
            }
        }
        else {

            with(configuration.hearingAid) {
                if (side == EarSide.Left)
                    mBinding.batteryViewLeftDevice.apply {
                        batteryLevel = this@with.batteryLevel
                        visibility = View.VISIBLE
                    }
                else
                    mBinding.batteryViewRightDevice.apply {
                        batteryLevel = this@with.batteryLevel
                        visibility = View.VISIBLE
                    }
            }
        }
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_control)
        mBinding.viewModel = ControlActivityListenersViewModel(this)
    }
    private fun initDoubleDeviceConnectionSliders()
    {
        mBinding.sliderLeft.apply {
            visibility = View.VISIBLE
            mBinding.textViewLeftSeekBar.visibility = View.VISIBLE
        }
        mBinding.sliderRight.apply {
            visibility = View.VISIBLE
            mBinding.textViewRightSeekBar.visibility = View.VISIBLE
        }
        mBinding.sliderCommon.visibility = View.INVISIBLE
        mBinding.textViewCommon.visibility = View.INVISIBLE
    }
    private fun initialize()
    {
        initBinding()
        initializeNavigationView()
        initMemoryButtons()
        initBatteryViews()
        initSwitch()

    }
    private fun initMemoryButtons()
    {
        runOnUiThread {
            initMemoryButtons(if (!configuration.isDoubleDeviceConnection) configuration.hearingAid
            else configuration.doubleDeviceMap[EarSide.Left]!!)
        }
    }
    private fun initMemoryButtons(hearingAid: HearingAid)
    {
        checkEnabledMemoryCount(hearingAid).also {

            if (it > 4) {
                initMemoryButtons(4, mBinding.tableLayout.getChildAt(0) as TableRow)
                initMemoryButtons(it - 4, mBinding.tableLayout.getChildAt(1) as TableRow)
            } else
                initMemoryButtons(it, mBinding.tableLayout.getChildAt(0) as TableRow)
        }
    }
    private fun checkEnabledMemoryCount(hearingAid: HearingAid) : Int
    {
        var count = 0
        try {
            (0..<hearingAid.numberOfMemories).forEach {
                if (hearingAid.wirelessControl?.memoryEnabled(
                        ParameterSpace.value(it)
                    )!!
                ) ++count
            }
        } catch (_: ArkException) {}

        return count
    }
    private fun initMemoryButtons(bound: Int, tableRow: TableRow)
    {
        (0..<bound).forEach { tableRow.getChildAt(it).visibility = View.VISIBLE }
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> true
                R.id.navbar_connection -> {
                    initializeNavigationView(ConnectionActivity::class.java)
                    finish(); true}
                else -> {
                    initializeNavigationView(AdvancedActivity::class.java)
                    finish(); true }
            }
        }
    }
    @Suppress("DEPRECATION")
    private inline fun <reified T : AppCompatActivity> initializeNavigationView(activity: Class<T>)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(Intent(this, T::class.java))
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        } else {
            Intent(this, T::class.java).apply {
                val animationBundle = ActivityOptions.makeCustomAnimation(
                    this@ControlActivity,
                    R.anim.slide_in,
                    R.anim.slide_out
                ).toBundle()
                startActivity(this, animationBundle)
            }
        }
    }
    private fun initSliders()
    {
        if (configuration.isConnected) {
            if (!configuration.isDoubleDeviceConnection)
                initSliders(mBinding.sliderCommon, configuration.hearingAid)

            else
                initDoubleDeviceConnectionSliders()

            if (configuration.isDualDeviceConfig)
                mBinding. apply {
                    sliderLeft.visibility = View.INVISIBLE
                    sliderRight.visibility = View.INVISIBLE
                    sliderCommon.visibility = View.VISIBLE
                }
        }
    }
    private fun initSliders(slider: Slider,  hearingAid: HearingAid)
    {
        slider.apply {
            value = hearingAid.volume.toFloat()
            trackActiveTintList = AppCompatResources.getColorStateList(
                this@ControlActivity, if (hearingAid.side == EarSide.Left) R.color.left_device_slider
                else R.color.right_device_slider
            )
            thumbTintList = AppCompatResources.getColorStateList(
                this@ControlActivity, if (hearingAid.side == EarSide.Left) R.color.left_device_slider
                else R.color.right_device_slider
            )
            mBinding.textViewCommon.text = getString(if (hearingAid.side == EarSide.Left) R.string.textview_left_device_text
            else R.string.textview_right_device_text)
        }
    }
    private fun initSwitch()
    {
        if (configuration.isDoubleDeviceConnection)
            mBinding.switchDualConfiguration.visibility = View.VISIBLE
    }
    private fun setButtonsDefaultColor(hearingAid: HearingAid, except: Int)
    {
        hearingAid.numberOfMemories.also {

            if (it > 4) {
                setButtonsDefaultColor(4, mBinding.tableLayout.getChildAt(0) as TableRow, except)
                setButtonsDefaultColor(it - 4, mBinding.tableLayout.getChildAt(1) as TableRow, except - 4)
            } else
                setButtonsDefaultColor(it, mBinding.tableLayout.getChildAt(0) as TableRow, except)
        }
    }
    private fun setButtonsDefaultColor(bound: Int, tableRow: TableRow, except: Int)
    {
        (0..<bound).filter { it != except  }.forEach { tableRow.getChildAt(it).apply {
            backgroundTintList = AppCompatResources.getColorStateList(
                this@ControlActivity, R.color.default_button_color)
            foregroundTintList = AppCompatResources.getColorStateList(
                this@ControlActivity, R.color.black)
            }
        }
    }
    private fun setSlidersFirstAnimation(slider: Slider) : AnimationListener
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
                            override fun onAnimationEnd(animation: Animation?) {}
                            override fun onAnimationRepeat(animation: Animation?) {}
                        })

                        })
                }
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        }
    }
    private fun setSlidersSecondAnimation(slider: Slider) : AnimationListener
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

            override fun onAnimationRepeat(animation: Animation?) {}
        }
    }
    @Synchronized
    private fun sliderLeftEarOnValueChangedCallback(value: Float)
    {
        try {
            configuration.apply {
                if (isConnected) {
                    if (!isDoubleDeviceConnection)
                        hearingAid.apply {
                            wirelessControl?.volume = value.toInt()
                            volume = value.toInt()
                        }
                    else
                        doubleDeviceMap[EarSide.Left]?.apply {
                            wirelessControl?.volume = value.toInt()
                            volume = value.toInt()
                        }
                }
            }
        } catch (_: ArkException) {}
    }
    private fun initActiveMemory(num: Int)
    {
        val button = with(mBinding) {
            when (num) {
                0 -> buttonOne
                1 -> buttonTwo
                2 -> buttonThree
                3 -> buttonFour
                4 -> buttonFive
                5 -> buttonSix
                6 -> buttonSeven
                else -> buttonEight
            }
        }
        setSelectedMemoryButtonView(button, num)
    }
    @Synchronized
    private fun sliderRightEarOnValueChangedCallback(value: Float)
    {
        try {
            configuration.apply {
                if (isConnected) {
                    if (!isDoubleDeviceConnection)
                        hearingAid.apply {
                            wirelessControl?.volume = value.toInt()
                            volume = value.toInt()
                        }
                    else
                        doubleDeviceMap[EarSide.Right]?.apply {
                            wirelessControl?.volume = value.toInt()
                            volume = value.toInt()
                        }
                }
            }
        } catch (_: ArkException) {}
    }
    fun memoryButtonClicked(buttonNumber: Int, button: Button)
    {
        try {
            with(configuration) {
                if (isDoubleDeviceConnection)
                    doubleDeviceMap.forEach {
                        it.value.wirelessControl?.currentMemory = ParameterSpace.value(buttonNumber)
                    }
                else
                    hearingAid.wirelessControl?.currentMemory = ParameterSpace.value(buttonNumber)
            }
            setSelectedMemoryButtonView(button, buttonNumber)
        } catch (_: ArkException) {}
    }
    fun setSelectedMemoryButtonView(button: Button, num : Int)
    {
        runOnUiThread {
            button.backgroundTintList = AppCompatResources.getColorStateList(
                this,
                com.google.android.material.R.color.design_default_color_secondary_variant
            )
            button.foregroundTintList = AppCompatResources.getColorStateList(
                this@ControlActivity, R.color.white)

            setButtonsDefaultColor(if (!configuration.isDoubleDeviceConnection) configuration.hearingAid
            else configuration.doubleDeviceMap[EarSide.Left]!!, num)
        }
    }
    fun sliderDualOnValueChanged(value: Float)
    {
        with(configuration) {
            if (isDoubleDeviceConnection) {
                sliderLeftEarOnValueChanged(value)
                sliderRightEarOnValueChanged(value)
            }
            else if (hearingAid.side == EarSide.Left)
                sliderLeftEarOnValueChanged(value)
            else
                sliderRightEarOnValueChanged(value)
        }
    }
    fun sliderLeftEarOnValueChanged(value: Float)
    {
        threadPool.execute { sliderLeftEarOnValueChangedCallback(value) }
    }

    fun sliderRightEarOnValueChanged(value: Float)
    {
        threadPool.execute { sliderRightEarOnValueChangedCallback(value) }
    }
    fun switchOnCheckedChanged()
    {
        if (!configuration.isDualDeviceConfig) {
            runOnUiThread {
                mBinding.sliderLeft.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_left_to_right_left_slider
                    ).also { it.setAnimationListener(setSlidersFirstAnimation(mBinding.sliderLeft)) }
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_right_to_left_right_slider
                    ).also { it.setAnimationListener(setSlidersFirstAnimation(mBinding.sliderRight)) }
                )

            }
            configuration.isDualDeviceConfig = true
        }
        else {
            runOnUiThread {
                mBinding.sliderLeft.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_right_to_left_left_slider
                    ).also { it.setAnimationListener(setSlidersSecondAnimation(mBinding.sliderLeft)) }
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_left_to_right_right_slider
                    ).also { it.setAnimationListener(setSlidersSecondAnimation(mBinding.sliderRight)) }
                )

            }
            configuration.isDualDeviceConfig = false
        }
    }
    private fun initTextViews()
    {
        if (configuration.isDualDeviceConfig) {
            mBinding.apply {
                textViewLeftSeekBar.visibility = View.INVISIBLE
                textViewRightSeekBar.visibility = View.INVISIBLE
            }
        }
    }
}