package com.alcanl.cleverear

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityAdvancedBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.listeners.AdvancedActivityEventListener
import com.alcanl.cleverear.viewmodel.AdvancedActivityViewModel
import com.ark.ArkException
import com.ark.EventHandler
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedActivity : AppCompatActivity(){
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var configuration : ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    private lateinit var mBinding : ActivityAdvancedBinding
    private lateinit var mListener: AdvancedActivityEventListener
    private var isListenerOnBoard = false
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
    }
    override fun onResume()
    {
        super.onResume()
        mBinding.bottomNavigationView.selectedItemId = R.id.navbar_advanced
        if (!configuration.isConnected)
            finish()
        else
            if (!isListenerOnBoard) {
                mListener = AdvancedActivityEventListener(this, eventHandler, threadPool, configuration)
                mListener.startEventListener()
                isListenerOnBoard = true
            }
    }
    override fun onStop()
    {
        super.onStop()
        if (isListenerOnBoard) {
            mListener.stopListener()
            isListenerOnBoard = false
        }
    }
    override fun onDestroy() {
        super.onDestroy()

        if (isListenerOnBoard) {
            mListener.stopListener()
            isListenerOnBoard = false
        }
    }
    private fun initialize()
    {
        initBinding()
        initializeNavigationView()
        initSwitch()
        initSliders()

    }
    private fun initSliders()
    {
        with(configuration) {
            if (!isDoubleDeviceConnection) {
                with(mBinding) {
                    sliderAuxLeft.visibility = View.INVISIBLE
                    sliderAuxRight.visibility= View.INVISIBLE
                    sliderMicLeft.visibility = View.INVISIBLE
                    sliderMicRight.visibility = View.INVISIBLE
                    initCommonSliderForSingleDevice(sliderMicCommon)
                    initCommonSliderForSingleDevice(sliderAuxCommon)
                }
            }
            else
                with(mBinding) {
                    if (!isDualDeviceConfig) {
                        sliderAuxCommon.visibility = View.INVISIBLE
                        sliderMicCommon.visibility = View.INVISIBLE
                    }
                    else {
                        sliderMicLeft.visibility = View.INVISIBLE
                        sliderAuxLeft.visibility = View.INVISIBLE
                        sliderMicRight.visibility = View.INVISIBLE
                        sliderAuxRight.visibility = View.INVISIBLE
                    }
                }
        }
    }
    private fun initCommonSliderForSingleDevice(slider: Slider)
    {
        slider.apply {
            visibility = View.VISIBLE
            isEnabled = true
            trackActiveTintList = initCommonSliderTheme()
            thumbTintList = initCommonSliderTheme()
        }
    }
    private fun initCommonSliderTheme() : ColorStateList
    {
        return if (configuration.hearingAid.side == EarSide.Left)
            AppCompatResources.getColorStateList(
                this@AdvancedActivity,
                R.color.left_device_slider
            )
        else
            AppCompatResources.getColorStateList(
                this@AdvancedActivity,
                R.color.right_device_slider
            )
    }
    fun switchOnCheckedChanged()
    {
        with(configuration) {
            runOnUiThread {
                isDualDeviceConfig = if (!isDualDeviceConfig) {
                    with(mBinding) {
                        sliderFirstAnimationCallback(sliderAuxLeft, R.anim.translate_slider_adv_left_to_right_left)
                        sliderFirstAnimationCallback(sliderMicLeft, R.anim.translate_slider_adv_left_to_right_left)
                        sliderFirstAnimationCallback(sliderAuxRight, R.anim.translate_slider_adv_right_to_left_right)
                        sliderFirstAnimationCallback(sliderMicRight, R.anim.translate_slider_adv_right_to_left_right)
                    }
                    true
                } else {
                    with(mBinding) {
                        sliderSecondAnimationCallback(sliderAuxLeft, R.anim.translate_slider_adv_right_to_left_left)
                        sliderSecondAnimationCallback(sliderMicLeft, R.anim.translate_slider_adv_right_to_left_left)
                        sliderSecondAnimationCallback(sliderAuxRight, R.anim.translate_slider_adv_left_to_right_right)
                        sliderSecondAnimationCallback(sliderMicRight, R.anim.translate_slider_adv_left_to_right_right)
                    false
                    }
                }
            }
        }
    }
    private fun sliderFirstAnimationCallback(slider: Slider, anim: Int)
    {
        with(slider) {
            startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(
                    this@AdvancedActivity,
                       anim
                ).also { it.setAnimationListener(setSlidersFirstAnimation(slider)) })
        }
    }
    private fun sliderSecondAnimationCallback(slider: Slider, anim: Int)
    {
        with(slider) {
            startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(
                    this@AdvancedActivity,
                    anim
                ).also { it.setAnimationListener(setSlidersSecondAnimation(slider)) })
        }
    }
    private fun setSlidersFirstAnimation(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                slider.visibility = View.INVISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {

                startCommonSlidersAnimation(mBinding.sliderAuxCommon)
                startCommonSlidersAnimation(mBinding.sliderMicCommon)

            }
            override fun onAnimationRepeat(animation: Animation?) {

            }
        }
    }
    private fun setSlidersSecondAnimation(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                with(mBinding) {
                    sliderAuxCommon.apply {
                        visibility = View.INVISIBLE; isEnabled = false
                    }
                    sliderMicCommon.apply {
                        visibility = View.INVISIBLE; isEnabled = false
                    }
                }
            }

            override fun onAnimationEnd(animation: Animation?) {
                slider.apply {
                    visibility = View.VISIBLE
                    isEnabled = true
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }
        }
    }
    private fun startCommonSlidersAnimation(slider: Slider)
    {
        slider.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(
                this@AdvancedActivity, R.anim.alpha_blink_anim
            ).also {it.setAnimationListener(

                object: AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        slider.visibility = View.VISIBLE
                        slider.isEnabled = true
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                }
            ) })

    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_advanced)
        mBinding.viewModel = AdvancedActivityViewModel(this)
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> {
                    if (isListenerOnBoard) {
                        mListener.stopListener()
                        isListenerOnBoard = false
                    }
                    initializeNavigationView(ControlActivity::class.java)
                    finish(); true}
                R.id.navbar_connection -> {
                    if (isListenerOnBoard) {
                        mListener.stopListener()
                        isListenerOnBoard = false
                    }
                    initializeNavigationView(ConnectionActivity::class.java)
                    finish(); true}
                else -> true
            }
        }
    }
    private fun setStreamAddressButtonClicked(channel: Int)
    {
        if (channel !in 1..255) {
            Toast.makeText(this, R.string.toast_channel_error_text, Toast.LENGTH_LONG).show()
            mBinding.editTextChannel.setText("")
            return
        }
        val addressShift: Int = channel shl 16
        val dataOr = 0x00CDE6 //52710
        val result = dataOr or addressShift

        with(configuration) {
            if (isConnected)
                if (isDoubleDeviceConnection)
                    doubleDeviceMap.forEach {
                        setStreamAddressButtonClickedCallback(result,channel, it.value) }
                else
                    setStreamAddressButtonClickedCallback(result, channel, hearingAid)
        }
    }
    private fun initSwitch()
    {
        if (configuration.isDoubleDeviceConnection)
            mBinding.switchDual.visibility = View.VISIBLE
    }
    private fun setStreamAddressButtonClickedCallback(result: Int, channel: Int, hearingAid: HearingAid)
    {
        threadPool.execute {
            try {
                with(hearingAid) {
                    streamAddressRaw = result
                    streamAddress = channel
                    wirelessControl?.streamAddress = result
                }
            } catch (_: ArkException) {}
        }
    }
    private fun modeButtonClickedCallback(view: View, hearingAid: HearingAid)
    {
        threadPool.execute {
            try {
                hearingAid.apply {
                    streamChannelList = when (view) {
                        mBinding.buttonModeOne -> 0
                        mBinding.buttonModeTwo -> 1
                        else -> 2
                    }
                    wirelessControl?.streamChannelList = when (view) {
                        mBinding.buttonModeOne -> 0
                        mBinding.buttonModeTwo -> 1
                        else -> 2
                    }
                }
            } catch (_: ArkException) {}
        }
    }
    private inline fun <reified T : AppCompatActivity> initializeNavigationView(activity: Class<T>)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(Intent(this, T::class.java))
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        } else {
            Intent(this, T::class.java).apply {
                val animationBundle = ActivityOptions.makeCustomAnimation(
                    this@AdvancedActivity,
                    R.anim.slide_in,
                    R.anim.slide_out
                ).toBundle()
                startActivity(this, animationBundle)
            }
        }
    }
    private fun micSliderValueChanged(value: Float, earSide: EarSide)
    {
        threadPool.execute {
            try {
                with(configuration) {
                    if (isConnected) {
                        if (!isDoubleDeviceConnection) {
                            hearingAid.wirelessControl?.micAttenuation = value.toInt()
                            hearingAid.micAttenuation = value.toInt()
                        }
                        else {
                            doubleDeviceMap[earSide]?.wirelessControl?.micAttenuation = value.toInt()
                            doubleDeviceMap[earSide]?.micAttenuation = value.toInt()
                        }
                    }
                }
            } catch (ex: ArkException) {
                Log.e("mic", ex.message!!)
            }
        }
    }
    private fun auxSliderValueChanged(value: Float, earSide: EarSide)
    {
        threadPool.execute {
            try {
                with(configuration) {
                    if (isConnected) {
                        if (!isDoubleDeviceConnection) {
                            hearingAid.wirelessControl?.auxAttenuation = value.toInt()
                            hearingAid.auxAttenuation = value.toInt()

                        } else {
                            doubleDeviceMap[earSide]?.wirelessControl?.auxAttenuation = value.toInt()
                            doubleDeviceMap[earSide]?.auxAttenuation = value.toInt()
                        }
                    }
                }

            } catch (ex: ArkException) {
                Log.e("aux", ex.message!!)
            }
        }
    }
    private fun setSelectedModeButtonView(button: Button)
    {
        runOnUiThread {
            button.backgroundTintList = AppCompatResources.getColorStateList(
                this,
                com.google.android.material.R.color.design_default_color_secondary_variant
            )
            button.foregroundTintList = AppCompatResources.getColorStateList(
                this@AdvancedActivity, R.color.white
            )

            mBinding.constraintLayoutMode.children.filter{
                it is Button && it != button }.forEach { setRestModeButtonsDefaultView(it as Button) }
        }
    }
    private fun setRestModeButtonsDefaultView(button: Button)
    {
        button.backgroundTintList = AppCompatResources.getColorStateList(
            this@AdvancedActivity, R.color.default_button_color)
        button.foregroundTintList = AppCompatResources.getColorStateList(
            this@AdvancedActivity, R.color.black)
    }
    fun modeButtonClicked(view: View)
    {
        with(configuration) {
            if (isConnected) {
                setSelectedModeButtonView(view as Button)

                if (!isDoubleDeviceConnection)
                    modeButtonClickedCallback(view, hearingAid)
                else
                    doubleDeviceMap.forEach { modeButtonClickedCallback(view, it.component2()) }
            }
        }
    }
    fun micDualSliderValueChanged(value: Float)
    {
        micLeftSliderValueChanged(value)
        micRightSliderValueChanged(value)
    }
    fun auxDualSliderValueChanged(value: Float)
    {
        auxLeftSliderValueChanged(value)
        auxRightSliderValueChanged(value)
    }
    fun micLeftSliderValueChanged(value: Float)
    {
        micSliderValueChanged(value, EarSide.Left)
    }
    fun micRightSliderValueChanged(value: Float)
    {
        micSliderValueChanged(value, EarSide.Right)
    }
    fun auxLeftSliderValueChanged(value: Float)
    {
        auxSliderValueChanged(value, EarSide.Left)
    }
    fun auxRightSliderValueChanged(value: Float)
    {
        auxSliderValueChanged(value, EarSide.Right)
    }
    fun setStreamAddressButtonClicked(view: View)
    {
        runOnUiThread { (view as ImageButton).startAnimation( android.view.animation.AnimationUtils.loadAnimation(
            this@AdvancedActivity, R.anim.alpha_blink_anim
        ).also {it.setAnimationListener(

            object: AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    val channel: Int

                    try {
                        channel = mBinding.editTextChannel.text.toString().toInt()
                        setStreamAddressButtonClicked(channel)
                    } catch (_: NumberFormatException) {
                        Toast.makeText(this@AdvancedActivity, R.string.toast_channel_error_text, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onAnimationEnd(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
            }
        ) })
        }
    }
}


