package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.SYSTEM_HIGH_PRIORITY
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.databinding.ActivityLoadingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.ConfigurationModule.earSide
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.HA_LEFT_ADDRESS
import com.alcanl.cleverear.util.HA_LEFT_NAME
import com.alcanl.cleverear.util.HA_LEFT_RSSI
import com.alcanl.cleverear.util.HA_RIGHT_ADDRESS
import com.alcanl.cleverear.util.HA_RIGHT_NAME
import com.alcanl.cleverear.util.HA_RIGHT_RSSI
import com.alcanl.cleverear.util.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.util.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.util.service.broadcast.BondHearingAidBroadcastReceiver
import com.alcanl.cleverear.util.listeners.LoadingActivityEventListener
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.service.AutoConnectService
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.EventObject
import javax.inject.Inject

@AndroidEntryPoint
class LoadingActivity : AppCompatActivity(), IEventListener {

    @Inject
    lateinit var configuration: ConfigurationModule

    @Inject
    lateinit var eventHandler: EventHandler

    @Inject
    lateinit var productManager: ProductManager

    @Inject
    lateinit var mBondRequestReceiver: BondHearingAidBroadcastReceiver

    @Inject
    lateinit var listener: LoadingActivityEventListener

    @Inject
    lateinit var handler: Handler

    private val mConnectorCoroutines : MutableList<Deferred<*>> = mutableListOf()

    private lateinit var mBinding : ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        EventBus.getDefault().register(this)

        with(configuration) {
            if (isConnectedBefore)
                loadPrefs()

            if (!isDoubleDeviceConnection)
                handleSingleDeviceConnection()
            else
                handleDoubleDeviceConnection()

        }
    }

    override fun onResume() {
        super.onResume()

        if (!listener.isListenerOnBoard())
            listener.startEventListener()
    }
    override fun onStop()
    {
        super.onStop()
        if (listener.isListenerOnBoard())
            listener.stopListener()

    }
    override fun onDestroy()
    {
        super.onDestroy()
        if (listener.isListenerOnBoard())
            listener.stopListener()

        EventBus.getDefault().unregister(this)
        unregisterReceiver(mBondRequestReceiver)
        handleSharedPrefs()

        with(configuration) {
            if (isDoubleDeviceConnection) {
                if (!isServiceOnBoard && isConnected && hearingAidMap.all { it.value.bondStatus == BluetoothDevice.BOND_BONDED })
                    startService()
            }

            else if (!isServiceOnBoard && isConnected && hearingAidMap[earSide]?.bondStatus == BluetoothDevice.BOND_BONDED)
                startService()

        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is ConnectionEvent)
            if (event.connectionData == 3)
                with(configuration) {
                    if (!isDoubleDeviceConnection) {
                        hearingAidMap[earSide]?.isConnected = true
                        isConnected = true
                    }
                    else {
                        if (event.eventSender == hearingAidMap[EarSide.Left]?.address) {
                            hearingAidMap[EarSide.Left]?.isConnected = true
                        }
                        else
                            hearingAidMap[EarSide.Right]?.isConnected = true

                        if (hearingAidMap.all { it.value.isConnected })
                            isConnected = true
                    }
                }
    }
    private fun startService()
    {
        Intent(this, AutoConnectService::class.java).apply {
            ContextCompat.startForegroundService(this@LoadingActivity, this)
        }
    }
    private fun handleSharedPrefs()
    {
        with(configuration) {
            if (isConnected) {

                val prefs = getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)
                val editor = prefs.edit()

                if (isDoubleDeviceConnection)
                    hearingAidMap.forEach { handleSharedPrefs(editor, it.value) }

                else
                    handleSharedPrefs(editor, hearingAidMap[earSide]!!)

                editor.apply()
            }
        }
    }
    private fun handleSharedPrefs(editor: Editor, hearingAid: HearingAid)
    {
        editor.putString(if (hearingAid.side == EarSide.Left) HA_LEFT_ADDRESS else HA_RIGHT_ADDRESS, hearingAid.address)
        editor.putString(if (hearingAid.side == EarSide.Left) HA_LEFT_NAME else HA_RIGHT_NAME, hearingAid.name)
        editor.putInt(if (hearingAid.side == EarSide.Left) HA_LEFT_RSSI else HA_RIGHT_RSSI, hearingAid.rssi)
        editor.putString(if (hearingAid.side == EarSide.Left) MANUFACTURER_CODE_LEFT else MANUFACTURER_CODE_RIGHT, hearingAid.manufacturerData)
    }
    private fun handleSingleDeviceConnection()
    {
        with(configuration) {
            checkDeviceBondStatus(hearingAidMap[earSide]!!)

            hearingAidMap[earSide]!!.getPropertiesFromSDK(productManager, eventHandler)
            connectForSingleDevice()

            if (hearingAidMap[earSide]!!.bondStatus == BluetoothDevice.BOND_BONDED) {
                initBondedDeviceCountDownTimer()
                return
            }

            initUnBondSingleDeviceCountDownTimer(hearingAidMap[earSide]!!)
        }
    }
    private fun handleDoubleDeviceConnection()
    {
        with(configuration) {
            hearingAidMap.forEach { checkDeviceBondStatus(it.value) }

            if (hearingAidMap.all { it.value.bondStatus == BluetoothDevice.BOND_BONDED }) {
                hearingAidMap.forEach {
                    it.value.getPropertiesFromSDK(productManager, eventHandler)
                    connectToDevice(it.value)
                }
                initBondedDeviceCountDownTimer()
            }

            else if (hearingAidMap.all { it.value.bondStatus == BluetoothDevice.BOND_NONE })
                unBoundDoubleDeviceInitialization(hearingAidMap[EarSide.Left]!!, true)

            else if (hearingAidMap[EarSide.Right]?.bondStatus == BluetoothDevice.BOND_NONE)
                unBoundDoubleDeviceInitialization(hearingAidMap[EarSide.Right]!!, false)

            else
                unBoundDoubleDeviceInitialization(hearingAidMap[EarSide.Left]!!, false)

        }
    }
    private fun unBoundDoubleDeviceInitialization(hearingAid: HearingAid, isDualUnBound: Boolean)
    {
        hearingAid.apply {
            configuration.earSide = hearingAid.side
            getPropertiesFromSDK(productManager, eventHandler)
            connectToDevice(this)

            if (isDualUnBound)
                initUnBondDoubleDeviceCountDownTimer()
            else
                initUnBondSingleDeviceCountDownTimer(hearingAid)

        }
    }
    private fun initUnBondDoubleDeviceCountDownTimer()
    {
        object: CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                commonUnBondDeviceOnTick(millisUntilFinished)

                if (configuration.hearingAidMap[EarSide.Left]?.bondStatus == BluetoothDevice.BOND_BONDED &&
                    configuration.hearingAidMap[EarSide.Left]?.isConnected!!) {

                    mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_device_connected_text)
                    handler.postDelayed({ startActivity(
                        Intent(this@LoadingActivity, LoadingActivity::class.java))
                        finish() }
                        , 3000)
                    this.cancel()
                }

            }
            override fun onFinish()
            {
                if (!configuration.hearingAidMap[EarSide.Left]?.isConnected!!) {
                    mBinding.textViewConnecting.text =
                        getString(R.string.textview_device_detection_not_connected_text)
                    handler.postDelayed({ finish() }, 3000)
                }
                else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoadingActivity,
                            R.string.toast_device_must_pair_text,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    finish()
                    lifecycleScope.launch(Dispatchers.IO) {
                        configuration.hearingAidMap[EarSide.Left]?.endConnectionSession()
                        configuration.hearingAidMap[EarSide.Left]?.isConnected = false
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
    private fun initUnBondSingleDeviceCountDownTimer(hearingAid: HearingAid)
    {
        object: CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                commonUnBondDeviceOnTick(millisUntilFinished)

                if (hearingAid.bondStatus == BluetoothDevice.BOND_BONDED && configuration.isConnected && mConnectorCoroutines.all { it.isCompleted }) {
                    finalizeConnectedLoadingTimer()
                    this.cancel()
                }
            }
            override fun onFinish() {

                if (!configuration.isConnected) {
                    mBinding.textViewConnecting.text =
                        getString(R.string.textview_device_detection_not_connected_text)
                    handler.postDelayed({ finish() }, 2000)
                }
                else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoadingActivity,
                            R.string.toast_device_must_pair_text,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    finish()
                    lifecycleScope.launch(Dispatchers.IO) {
                        configuration.hearingAidMap[earSide]?.endConnectionSession()
                        configuration.isConnected = false
                    }
                }
            }
        }.start()
    }
    private fun initBondedDeviceCountDownTimer()
    {
        object: CountDownTimer(12000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                commonBondDeviceOnTick(millisUntilFinished)
                if (configuration.isConnected && mConnectorCoroutines.all { it.isCompleted }) {
                    finalizeConnectedLoadingTimer()
                    this.cancel()
                }
            }
            override fun onFinish()
            {
                finalizeLoadingTimer()
            }
        }.start()
    }
    private fun finalizeConnectedLoadingTimer()
    {
        mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_device_connected_text)
        handler.postDelayed({
            if (!configuration.isRemoteFittingConnection)
                if (configuration.isConnectedBefore)
                    Intent(this@LoadingActivity, MainContainerActivity::class.java)
                        .apply { startActivity(this); finish() }
                else
                    finish()
            else
                Intent(this@LoadingActivity, SelfFittingActivity::class.java
            ).apply { startActivity(this) };  finish() }, 3000)
    }
    private fun finalizeLoadingTimer()
    {
        if (configuration.isConnected)
            finalizeConnectedLoadingTimer()
        else {
            mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_not_connected_text)
            handler.postDelayed({ finish() }, 3000)
        }
    }
    private fun initialize()
    {
        registerReceiver(mBondRequestReceiver, IntentFilter(ACTION_BOND_STATE_CHANGED)
            .apply { priority = SYSTEM_HIGH_PRIORITY })
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_loading)
        listener.startEventListener()
    }
    private fun connectForSingleDevice()
    {
        connectToDevice(configuration.hearingAidMap[earSide]!!)
    }

    private fun connectToDevice(hearingAid: HearingAid)
    {
        val adapter = getSystemService(BluetoothManager::class.java).adapter
        mConnectorCoroutines.add( lifecycleScope.async(Dispatchers.IO) { connectToDeviceCallback(adapter, hearingAid) } )
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectToDeviceCallback(adapter: BluetoothAdapter, hearingAid: HearingAid)
    {
        try {
            hearingAid.communicationAdaptor?.connect()
            delay(6000)

            if (hearingAid.isConnected && hearingAid.bondStatus == BluetoothDevice.BOND_NONE)
                adapter.getRemoteDevice(hearingAid.address).createBond()

            else if (hearingAid.isConnected && hearingAid.bondStatus == BluetoothDevice.BOND_BONDED) {
                hearingAid.readDeviceConfiguration()
                if (hearingAid.isEddystoneTLM)
                    configuration.eddyStoneMap[hearingAid.side] = EddystoneHearingAid()
            }

        }
        catch (ex: ArkException) {
            Log.e(LoadingActivity::class.simpleName, ex.message!!)
            runOnUiThread {
                Toast.makeText(this, R.string.toast_problem_occurred_text, Toast.LENGTH_LONG).show()
            }
        }
        catch (ex: InterruptedException) {
            Log.e(LoadingActivity::class.simpleName, ex.message!!)
        }
    }
    @SuppressLint("MissingPermission")
    private fun checkDeviceBondStatus(hearingAid: HearingAid)
    {
        val adapter = getSystemService(BluetoothManager::class.java).adapter

        if (adapter.bondedDevices.any { hearingAid.address == it.address })
            hearingAid.bondStatus = BluetoothDevice.BOND_BONDED

    }
    private fun loadPrefs()
    {
        val prefs = getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)

        with(configuration) {
            if (isDoubleDeviceConnection) {
                hearingAidMap[EarSide.Left] = getLeftDevice(prefs)
                hearingAidMap[EarSide.Right] = getRightDevice(prefs)
            }
            else
                hearingAidMap[earSide] = if (earSide == EarSide.Left) getLeftDevice(prefs) else getRightDevice(prefs)
        }

    }
    private fun getLeftDevice(prefs: SharedPreferences) : HearingAid
    {
        val deviceAddress = prefs.getString(HA_LEFT_ADDRESS, "")
        val deviceName = prefs.getString(HA_LEFT_NAME, "")
        val deviceRSSI = prefs.getInt(HA_LEFT_RSSI, 0)
        val deviceManu = prefs.getString(MANUFACTURER_CODE_LEFT, "")

        return HearingAid(deviceAddress!!, deviceName!!, deviceRSSI, deviceManu!!, EarSide.Left)

    }

    private fun getRightDevice(prefs: SharedPreferences) : HearingAid
    {
        val deviceAddress = prefs.getString(HA_RIGHT_ADDRESS, "")
        val deviceName = prefs.getString(HA_RIGHT_NAME, "")
        val deviceRSSI = prefs.getInt(HA_RIGHT_RSSI, 0)
        val deviceManu = prefs.getString(MANUFACTURER_CODE_RIGHT, "")

        return HearingAid(deviceAddress!!, deviceName!!, deviceRSSI, deviceManu!!, EarSide.Right)

    }
}
