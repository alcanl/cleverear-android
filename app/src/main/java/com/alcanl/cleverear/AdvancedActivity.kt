package com.alcanl.cleverear

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.ark.ArkException
import com.ark.EventHandler
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
                mListener.startEventListener()
                isListenerOnBoard = true
            }
    }

    override fun onPause()
    {
        super.onPause()
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
        mListener = AdvancedActivityEventListener(this, eventHandler, threadPool, configuration)

    }

    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_advanced)
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> {
                    initializeNavigationView(ControlActivity::class.java); finish(); true}
                R.id.navbar_connection -> {
                    initializeNavigationView(ConnectionActivity::class.java); finish(); true}
                else -> true
            }
        }
    }
    private fun setStreamAddressButtonClicked(channel: Int)
    {
        if (channel !in 1..255) {
            Toast.makeText(this, "Channel must be between 1 and 255", Toast.LENGTH_LONG).show()
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
        with(hearingAid) {
            streamAddressRaw = result
            streamAddress = channel
            wirelessControl?.streamAddress = result
        }
    }

    private fun modeButtonClickedCallback(view: View, hearingAid: HearingAid)
    {
        hearingAid.streamChannelList = when (view) {
            mBinding.buttonModeOne -> 1
            mBinding.buttonModeTwo -> 2
            else -> 3
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
        try {
            if (configuration.isConnected) {
                if (!configuration.isDoubleDeviceConnection) {
                    configuration.hearingAid.wirelessControl?.micAttenuation = value.toInt()

                } else
                    configuration.doubleDeviceMap[earSide]?.wirelessControl?.micAttenuation = value.toInt()
            }
        } catch (_: ArkException) {}
    }
    private fun auxSliderValueChanged(value: Float, earSide: EarSide)
    {
        try {
            if (configuration.isConnected) {
                if (!configuration.isDoubleDeviceConnection) {
                    configuration.hearingAid.wirelessControl?.auxAttenuation = value.toInt()

                } else
                    configuration.doubleDeviceMap[earSide]?.wirelessControl?.auxAttenuation = value.toInt()
            }
        } catch (_: ArkException) {}
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
        threadPool.execute { micLeftSliderValueChanged(value) }
        threadPool.execute { micRightSliderValueChanged(value) }
    }
    fun auxDualSliderValueChanged(value: Float)
    {
        threadPool.execute { auxLeftSliderValueChanged(value) }
        threadPool.execute { auxRightSliderValueChanged(value) }
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
    fun setStreamAddressButtonClicked()
    {
        val channel: Int

        try {
            channel = mBinding.editTextChannel.text.toString().toInt()
            setStreamAddressButtonClicked(channel)
        } catch (_: NumberFormatException) {
            Toast.makeText(this, "Illegal Arguments for Channel", Toast.LENGTH_LONG).show()
        }
    }
}


