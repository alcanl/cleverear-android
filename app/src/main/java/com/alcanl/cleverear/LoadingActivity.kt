package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityLoadingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
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
    private lateinit var mBluetoothAdapter : BluetoothAdapter
    private var mBondRequestReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()

        with(configuration) {

            if (!isDoubleDeviceConnection)
                handleSingleDeviceConnection()
            else
                handleDoubleDeviceConnection()

        }
    }
    override fun onDestroy()
    {
        super.onDestroy()
        mListener.stopListener()
        if (mBondRequestReceiver != null)
            unregisterReceiver(mBondRequestReceiver)
    }
    private fun handleSingleDeviceConnection()
    {
        with(configuration) {
            checkDeviceBondStatus(hearingAid)

            hearingAid.getPropertiesFromSDK(productManager, eventHandler)
            connectForSingleDevice()

            if (hearingAid.bondStatus == BluetoothDevice.BOND_BONDED) {
                initBondedDeviceCountDownTimer()
                return
            }

            mBondRequestReceiver = handleBondDeviceRequest(hearingAid)
            registerReceiver(
                mBondRequestReceiver,
                IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            )

            initUnBondSingleDeviceCountDownTimer()
        }
    }
    private fun handleDoubleDeviceConnection()
    {
        with(configuration) {
            doubleDeviceMap.forEach { checkDeviceBondStatus(it.value) }

            if (doubleDeviceMap.all { it.value.bondStatus == BluetoothDevice.BOND_BONDED }) {
                doubleDeviceMap.forEach {
                    it.value.getPropertiesFromSDK(productManager, eventHandler)
                    connectToDevice(it.value)
                }
                initBondedDeviceCountDownTimer()
            }

            else if (doubleDeviceMap.all { it.value.bondStatus == BluetoothDevice.BOND_NONE })
                unBoundDoubleDeviceInitialization(doubleDeviceMap[EarSide.Left]!!, true)

            else if (doubleDeviceMap[EarSide.Right]?.bondStatus == BluetoothDevice.BOND_NONE)
                unBoundDoubleDeviceInitialization(doubleDeviceMap[EarSide.Right]!!, false)

            else
                unBoundDoubleDeviceInitialization(doubleDeviceMap[EarSide.Left]!!, false)

        }
    }
    private fun unBoundDoubleDeviceInitialization(hearingAid: HearingAid, isDualUnBound: Boolean)
    {
        hearingAid.apply {
            getPropertiesFromSDK(productManager, eventHandler)
            connectToDevice(this)
            mBondRequestReceiver = handleBondDeviceRequest(this)
            registerReceiver(mBondRequestReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            if (isDualUnBound)
                initUnBondDoubleDeviceCountDownTimer()
            else
                initUnBondSingleDeviceCountDownTimer()
        }
    }
    private fun initUnBondDoubleDeviceCountDownTimer()
    {
        object: CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                commonUnBondDeviceOnTick(millisUntilFinished)

                if (mBondRequestReceiver!!.resultCode == RESULT_OK &&
                    configuration.doubleDeviceMap[EarSide.Left]?.isConnected!!) {

                    mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_device_connected_text)
                    Handler(Looper.myLooper()!!).postDelayed({ startActivity(intent); finish() }
                        , 2000)
                    this.cancel()
                }

            }
            override fun onFinish()
            {
                if (!configuration.doubleDeviceMap[EarSide.Left]?.isConnected!!) {
                    mBinding.textViewConnecting.text =
                        getString(R.string.textview_device_detection_not_connected_text)
                    Handler(Looper.myLooper()!!).postDelayed({ finish() }, 2000)
                }
                else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoadingActivity,
                            R.string.toast_device_must_pair_text,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        threadPool.execute {
                            configuration.doubleDeviceMap[EarSide.Left]?.endConnectionSession()
                            configuration.doubleDeviceMap[EarSide.Left]?.isConnected = false
                        }
                    }
                }
            }
        }.start()
    }
    private fun commonBondDeviceOnTick(millisUntilFinished: Long)
    {
        if (millisUntilFinished in 2000L..2999 || millisUntilFinished in 6000L..<6999)
            mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_connection_in_process_text)

        mBinding.textViewConnecting.text = String.format("${mBinding.textViewConnecting.text}" + ".")
    }
    private fun commonUnBondDeviceOnTick(millisUntilFinished: Long)
    {
        if (millisUntilFinished in 5000L..5999 || millisUntilFinished in 10000L..<10999
            || millisUntilFinished in 16000..16999 || millisUntilFinished in 24000..24999)
            mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_connection_in_process_text)

        mBinding.textViewConnecting.text = String.format("${mBinding.textViewConnecting.text}" + ".")
    }
    private fun initUnBondSingleDeviceCountDownTimer()
    {
        object: CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                commonUnBondDeviceOnTick(millisUntilFinished)

                if (mBondRequestReceiver!!.resultCode == RESULT_OK && configuration.isConnected) {
                    handleBondedConnection()
                    this.cancel()
                }
            }
            override fun onFinish() {

                if (!configuration.isConnected) {
                    mBinding.textViewConnecting.text =
                        getString(R.string.textview_device_detection_not_connected_text)
                    Handler(Looper.myLooper()!!).postDelayed({ finish() }, 2000)
                }
                else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoadingActivity,
                            R.string.toast_device_must_pair_text,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        threadPool.execute {
                            configuration.hearingAid.endConnectionSession()
                            configuration.isConnected = false
                        }
                    }
                }
            }
        }.start()
    }
    private fun initBondedDeviceCountDownTimer()
    {
        object: CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                commonBondDeviceOnTick(millisUntilFinished)
            }
            override fun onFinish() {
                if (configuration.isConnected) {
                    mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_device_connected_text)
                    Handler(Looper.myLooper()!!).postDelayed({ finish()
                        startActivity(Intent
                            (this@LoadingActivity, ControlActivity::class.java)) }, 2000)
                }
                else {
                    mBinding.textViewConnecting.text =
                        getString(R.string.textview_device_detection_not_connected_text)
                    Handler(Looper.myLooper()!!).postDelayed({ finish() }, 1000)
                }
            }
        }.start()
    }
    private fun initialize()
    {
        mBluetoothAdapter = getSystemService(android.bluetooth.BluetoothManager::class.java).adapter
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_loading)
        mListener = LoadingActivityEventListener(this, eventHandler, configuration)
            .apply { startEventListener() }

    }
    private fun connectForSingleDevice()
    {
        connectToDevice(configuration.hearingAid)
    }
    private fun connectToDevice(hearingAid: HearingAid)
    {
        threadPool.execute {
            try {
                hearingAid.communicationAdaptor?.connect()
                runBlocking {
                    delay(10000)
                    readDeviceConfiguration(hearingAid)
                }

            }
            catch (ex: ArkException)
            {
                Log.e("connect", ex.message!!)
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
    private fun handleBondDeviceRequest(hearingAid: HearingAid) : BroadcastReceiver
    {
        return object : BroadcastReceiver() {

            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?)
            {
                val action = intent!!.action

                if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {

                    if (mBluetoothAdapter.getRemoteDevice(hearingAid.address).bondState == BluetoothDevice.BOND_BONDED)
                    {
                        hearingAid.bondStatus = BluetoothDevice.BOND_BONDED
                    }

                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun checkDeviceBondStatus(hearingAid: HearingAid)
    {
        for (device in mBluetoothAdapter.bondedDevices)
            if (hearingAid.address == device.address)
                hearingAid.bondStatus = BluetoothDevice.BOND_BONDED

    }
    private fun handleBondedConnection() {

        mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_device_connected_text)
        Handler(Looper.myLooper()!!).postDelayed({
            startActivity(Intent
                (this@LoadingActivity, ControlActivity::class.java))
                finish()}, 2000)

    }
}
