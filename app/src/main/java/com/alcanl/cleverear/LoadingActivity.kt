package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.SYSTEM_HIGH_PRIORITY
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
import com.alcanl.cleverear.util.checkAndRequestPermissionForN
import com.alcanl.cleverear.util.checkAndRequestPermissionForQ
import com.alcanl.cleverear.util.checkAndRequestPermissionForS
import com.alcanl.cleverear.util.getLeftSideHearingAid
import com.alcanl.cleverear.util.getRightSideHearingAid
import com.alcanl.cleverear.util.hasBluetoothPermission
import com.alcanl.cleverear.util.hasForegroundServicePermission
import com.alcanl.cleverear.util.service.broadcast.BondHearingAidBroadcastReceiver
import com.alcanl.cleverear.util.listeners.LoadingActivityEventListener
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.saveHearingAidInfoToPrefs
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

private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1002

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

    private var mIsBluetoothPermissionsGranted = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()

        if (!mIsBluetoothPermissionsGranted) {
            requestPermissions()
            return
        }

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)

        configuration.apply {
            if (isConnectedBefore)
                loadPrefs()

            if (!isDoubleDeviceConnection)
                handleSingleDeviceConnection()
            else
                handleDoubleDeviceConnection()
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (!listener.isListenerOnBoard())
            listener.startEventListener()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        if (listener.isListenerOnBoard())
            listener.stopListener()

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)

        unregisterReceiver(mBondRequestReceiver)
        handleSharedPrefs()

        configuration.apply {
            if (isDoubleDeviceConnection) {
                if (hasForegroundServicePermission(this@LoadingActivity) &&
                    !isServiceOnBoard && isConnected &&
                    hearingAidMap.all { it.value.bondStatus == BluetoothDevice.BOND_BONDED })
                    startService()
            }

            else if (hasForegroundServicePermission(this@LoadingActivity) &&
                !isServiceOnBoard && isConnected &&
                hearingAidMap[earSide]?.bondStatus == BluetoothDevice.BOND_BONDED)
                startService()
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is ConnectionEvent)
            if (event.connectionData == 3)
                configuration.apply {
                    if (!isDoubleDeviceConnection) {
                        hearingAidMap[earSide]?.isConnected = true
                        isConnected = true
                    }
                    else {
                        if (event.eventSender == hearingAidMap[EarSide.Left]?.address)
                            hearingAidMap[EarSide.Left]?.isConnected = true
                        else
                            hearingAidMap[EarSide.Right]?.isConnected = true

                        if (hearingAidMap.all { it.value.isConnected })
                            isConnected = true
                    }
                }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray)
    {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.toast_no_permission_splash_activity, Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1000)
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startService()
    {
        Intent(this, AutoConnectService::class.java).apply {
            ContextCompat.startForegroundService(this@LoadingActivity, this)
        }
    }

    private fun requestPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkAndRequestPermissionForS(this, BLUETOOTH_PERMISSION_REQUEST_CODE)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            checkAndRequestPermissionForQ(this, BLUETOOTH_PERMISSION_REQUEST_CODE)
        else
            checkAndRequestPermissionForN(this, BLUETOOTH_PERMISSION_REQUEST_CODE)

    }

    private fun handleSharedPrefs()
    {
        configuration.apply {
            if (isConnected)
                if (isDoubleDeviceConnection)
                    hearingAidMap.forEach { saveHearingAidInfoToPrefs(this@LoadingActivity, it.value) }
                else
                    saveHearingAidInfoToPrefs(this@LoadingActivity, hearingAidMap[earSide]!!)
        }
    }

    private fun handleSingleDeviceConnection()
    {
         configuration.apply {
            checkDeviceBondStatus(hearingAidMap[earSide]!!)

            hearingAidMap[earSide]!!.getPropertiesFromSDK(productManager, eventHandler)
            connectToDevice(configuration.hearingAidMap[earSide]!!)

            if (hearingAidMap[earSide]!!.bondStatus == BluetoothDevice.BOND_BONDED) {
                initBondedDeviceCountDownTimer()
                return
            }

            initUnBondSingleDeviceCountDownTimer(hearingAidMap[earSide]!!)
        }
    }
    private fun handleDoubleDeviceConnection()
    {
        configuration.apply {

            hearingAidMap.forEach { checkDeviceBondStatus(it.value) }

            when {
                hearingAidMap.all { it.value.bondStatus == BluetoothDevice.BOND_BONDED } -> {
                    hearingAidMap.forEach {
                        it.value.getPropertiesFromSDK(productManager, eventHandler)
                        connectToDevice(it.value)
                    }
                initBondedDeviceCountDownTimer()
            }

            hearingAidMap.all { it.value.bondStatus == BluetoothDevice.BOND_NONE } ->
                unBoundDoubleDeviceInitialization(hearingAidMap[EarSide.Left]!!, true)

            hearingAidMap[EarSide.Right]?.bondStatus == BluetoothDevice.BOND_NONE ->
                unBoundDoubleDeviceInitialization(hearingAidMap[EarSide.Right]!!, false)

            else -> unBoundDoubleDeviceInitialization(hearingAidMap[EarSide.Left]!!, false)
            }
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
            override fun onFinish() { finalizeLoadingTimer() } }.start()
    }

    private fun finalizeConnectedLoadingTimer()
    {
        mBinding.apply {
            textViewConnecting.text = getString(R.string.textview_device_detection_device_connected_text)
            progressBar.visibility = View.INVISIBLE
            check.check()
        }

        configuration.isUserDisconnected = false

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
        mIsBluetoothPermissionsGranted = hasBluetoothPermission(this)
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
        with(configuration) {
            when {
                isDoubleDeviceConnection -> {
                    hearingAidMap[EarSide.Left] = getLeftSideHearingAid(this@LoadingActivity)
                    hearingAidMap[EarSide.Right] = getRightSideHearingAid(this@LoadingActivity)
                }
                else -> hearingAidMap[earSide] = if (earSide == EarSide.Left)
                        getLeftSideHearingAid(this@LoadingActivity)
                    else
                        getRightSideHearingAid(this@LoadingActivity)
            }
        }
    }
}
