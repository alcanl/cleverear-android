package com.alcanl.cleverear

import android.content.Intent
import android.content.SharedPreferences.Editor
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityLoadingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.DEVICE_MAC_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_MAC_KEY_R
import com.alcanl.cleverear.helpers.DEVICE_MANU_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_MANU_KEY_R
import com.alcanl.cleverear.helpers.DEVICE_NAME_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_NAME_KEY_R
import com.alcanl.cleverear.helpers.DEVICE_NAME_PREF
import com.alcanl.cleverear.helpers.DEVICE_RSSI_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_RSSI_KEY_R
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.listeners.LoadingActivityEventListener
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import javax.inject.Inject
@AndroidEntryPoint
class LoadingActivity : AppCompatActivity() {
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var productManager: ProductManager

    private lateinit var mListener: LoadingActivityEventListener
    private lateinit var mBinding : ActivityLoadingBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        initCountDownTimer()
    }
    override fun onDestroy()
    {
        super.onDestroy()
        with(configuration) {

            if (isConnected) {
                val editor = getSharedPreferences(DEVICE_NAME_PREF, MODE_PRIVATE).edit()
                if (!isDoubleDeviceConnection)
                    if (earSide == EarSide.Left)
                        putPreferencesLeftDevice(editor, configuration.hearingAid)
                    else
                        putPreferencesRightDevice(editor, configuration.hearingAid)
                else {
                    putPreferencesLeftDevice(editor, doubleDeviceMap[EarSide.Left]!!)
                    putPreferencesRightDevice(editor, doubleDeviceMap[EarSide.Right]!!)
                }

            }
        }

        mListener.stopListener()
    }
    private fun putPreferencesLeftDevice(editor: Editor, hearingAid: HearingAid)
    {
        with(editor) {
            putString(DEVICE_NAME_KEY_L, hearingAid.name)
            putString(DEVICE_MAC_KEY_L, hearingAid.address)
            putInt(DEVICE_RSSI_KEY_L, hearingAid.rssi)
            putString(DEVICE_MANU_KEY_L, hearingAid.manufacturerData)
            apply()
        }
    }
    private fun putPreferencesRightDevice(editor: Editor, hearingAid: HearingAid)
    {
        with(editor) {
            putString(DEVICE_NAME_KEY_R, hearingAid.name)
            putString(DEVICE_MAC_KEY_R, hearingAid.address)
            putInt(DEVICE_RSSI_KEY_R, hearingAid.rssi)
            putString(DEVICE_MANU_KEY_R, hearingAid.manufacturerData)
            apply()
        }
    }
    private fun initCountDownTimer()
    {
        object: CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                if (millisUntilFinished in 2000L..2999 || millisUntilFinished in 6000L..<6999)
                    mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_connection_in_process_text)

                mBinding.textViewConnecting.text = String.format("${mBinding.textViewConnecting.text}" + ".")
            }
            override fun onFinish()
            {
                if (configuration.isConnected)
                    {
                        mBinding.textViewConnecting.text =
                            getString(R.string.textview_device_detection_device_connected_text)
                        Handler(Looper.myLooper()!!).postDelayed( {finish(); startActivity(Intent
                            (this@LoadingActivity, ControlActivity::class.java))}, 1000)
                    }
                    else {
                        mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_not_connected_text)
                    Handler(Looper.myLooper()!!).postDelayed( {finish()}, 1000)
                        }
            }
        }.start()
    }
    private fun initialize()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_loading)
        mListener = LoadingActivityEventListener(this, eventHandler, threadPool, configuration)
            .apply { startEventListener() }
        getDevicePropertiesFromSDK()

    }
    private fun getDevicePropertiesFromSDK()
    {
        with(configuration) {

            if (!isDoubleDeviceConnection) {
                hearingAid.getPropertiesFromSDK(productManager, eventHandler)
                connectForSingleDevice()
            }
            else {
                configuration.doubleDeviceMap.forEach { it.value.getPropertiesFromSDK(productManager, eventHandler) }
                connectForDoubleDevices()
            }
        }
    }
    private fun connectForSingleDevice()
    {
        connectToDevice(configuration.hearingAid)
    }
    private fun connectForDoubleDevices()
    {
        configuration.doubleDeviceMap.forEach { connectToDevice(it.value) }
    }
    private fun connectToDevice(hearingAid: HearingAid)
    {
        threadPool.execute {
            try {
                hearingAid.communicationAdaptor?.connect()
                runBlocking {
                    delay(7000)
                    readDeviceConfiguration(hearingAid)
                }

            }
            catch (ex: ArkException)
            {
                runOnUiThread {
                    Toast.makeText(this, R.string.toast_problem_occured_text, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun readDeviceConfiguration(hearingAid: HearingAid)
    {
        hearingAid.readDeviceConfiguration()
    }
}
