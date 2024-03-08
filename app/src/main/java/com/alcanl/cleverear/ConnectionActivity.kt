package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityConnectionBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.util.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.util.adapter.DeviceListAdapter
import com.alcanl.cleverear.util.scan.DeviceScanHandler
import com.alcanl.cleverear.util.parseJsonScanData
import com.alcanl.cleverear.sdk.events.ScanEvent
import com.alcanl.cleverear.util.service.AutoConnectService
import com.alcanl.cleverear.viewmodel.ConnectionActivityListenersViewModel
import com.ark.ArkException
import com.ark.AsyncResult
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionActivity : AppCompatActivity() {

    @Inject
    lateinit var deviceConnectionHandler: DeviceScanHandler
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    @Volatile
    private var mDeviceCount = 0
    private var mIsDeviceFound = false
    private var isScanContinue = false
    private lateinit var mAdapter: DeviceListAdapter
    private lateinit var scanOnBoard : Thread
    private lateinit var mAsyncResult: AsyncResult
    private lateinit var mSelectedHearingAid: HearingAid
    private lateinit var mBinding: ActivityConnectionBinding
    @Volatile
    private var mBleDeviceList = HashSet<DiscoveredDeviceInfo>()
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        buttonDetectDeviceClicked()
    }
    override fun onStop()
    {
        if (isScanContinue)
            stopScanButtonClicked()

        super.onStop()
    }
    override fun onResume()
    {
        super.onResume()
        mBinding.bottomNavigationView.selectedItemId = R.id.navbar_connection

        with(mBinding.textViewDeviceState) {
            text = if (configuration.isConnected) getString(R.string.device_connected_text)
                else getString(R.string.textview_device_detection_not_found_text)
        }
    }
    private fun alertDialogYesButtonCallback()
    {
        mBinding.frameLayoutStopConnection.visibility = View.VISIBLE

        if (configuration.isServiceOnBoard) {
            Intent(this, AutoConnectService::class.java).apply {
                stopService(this)
            }
            configuration.isServiceOnBoard = false

        }

        Handler(Looper.myLooper()!!).postDelayed (
            {
                with(configuration) {
                    if (isDoubleDeviceConnection)
                        doubleDeviceMap.forEach {it.value.endConnectionSession()}
                    else
                        hearingAid.endConnectionSession()

                    isConnected = false
                }
                mBinding.apply {
                    textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
                    frameLayoutStopConnection.visibility = View.GONE
                }
                Intent(this, MainActivity::class.java).apply {
                    startActivity(this)

                    finish()
                }
            }, 6000
        )
    }
    private fun finalizeConnectionConfiguration()
    {
        if (isScanContinue)
            stopScanButtonClicked()

        mIsDeviceFound = true
        finish()
        startActivity(Intent(this, LoadingActivity::class.java))
    }
    private fun initialize() {
        initBinding()
        initializeNavigationView()
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> initializeNavigationView(ControlActivity::class.java)
                R.id.navbar_connection -> true
                else -> initializeNavigationView(AdvancedActivity::class.java)
            }
        }
    }
    private inline fun <reified T : AppCompatActivity> initializeNavigationView(activity: Class<T>) : Boolean
    {
        return if (configuration.isConnected) {
            if (VERSION.SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivity(Intent(this, T::class.java))
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out); finish() ;true
            } else {
                Intent(this, T::class.java).apply {
                    val animationBundle = ActivityOptions.makeCustomAnimation(
                        this@ConnectionActivity,
                        R.anim.slide_in,
                        R.anim.slide_out
                    ).toBundle()
                    startActivity(this, animationBundle)
                }; finish(); true
            }
        } else false
    }
    private fun initBinding() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_connection)
        mBinding.viewModel = ConnectionActivityListenersViewModel(this)
        mAdapter = DeviceListAdapter(this)
            .apply {
                if (configuration.isDoubleDeviceConnection) mBinding.listView.choiceMode =
                    ListView.CHOICE_MODE_MULTIPLE
            }
        mBinding.adapter = mAdapter
    }
    private fun isHearingAid(manufacturerCode: String): Boolean {
        var manufacturerCodeSub = ""

        try {
            manufacturerCodeSub = manufacturerCode.substring(0, 6)
        } catch (_: StringIndexOutOfBoundsException) {}

        return if (!configuration.isDoubleDeviceConnection)
            if (configuration.earSide == EarSide.Left) manufacturerCodeSub == MANUFACTURER_CODE_LEFT
            else manufacturerCodeSub == MANUFACTURER_CODE_RIGHT
        else
            manufacturerCodeSub == MANUFACTURER_CODE_RIGHT || manufacturerCodeSub == MANUFACTURER_CODE_LEFT
    }
    private fun putItemToDeviceMap(earSide: EarSide, device: DiscoveredDeviceInfo)
    {
        if (configuration.doubleDeviceMap.containsKey(earSide))
            return

        configuration.doubleDeviceMap[earSide] = HearingAid(
            device.address,
            device.name, device.rssi, device.manufacturerData, earSide
        )
    }
    private fun recognizeEarSide(manufacturerData: String): EarSide
    {
        return if (manufacturerData.substring(0, 6) == MANUFACTURER_CODE_LEFT) EarSide.Left else EarSide.Right
    }
    private fun scanDevicesWithBluetooth()
    {
        try {
            mAsyncResult =
                if (configuration.earSide == EarSide.Right) deviceConnectionHandler.scanForRightDevice()
                else deviceConnectionHandler.scanForLeftDevice()

        } catch (ex: ArkException) {
            Log.e("", ex.message!!)
            Toast.makeText(this, R.string.toast_connection_problem_text, Toast.LENGTH_LONG).show()
        }
    }
    @SuppressLint("SuspiciousIndentation")
    private fun scanThreadCallBack()
    {
        scanOnBoard = Thread.currentThread()
        mBleDeviceList.clear()
        runOnUiThread { mBinding.adapter!!.notifyDataSetChanged() }

        while (!scanOnBoard.isInterrupted) {
            val event = eventHandler.event
            if (event.type == EventType.kScanEvent) {
                Log.e(ScanEvent::class.java.simpleName, event.data)
                val device = parseJsonScanData(event.data, this)
                    if (isHearingAid(device.manufacturerData)) {
                        mBleDeviceList.add(device)
                        runOnUiThread { addHearingAidToListCallback(device) }
                   }
            }
        }
        Log.e("scan", "Scan ended")
    }
    private fun addHearingAidToListCallback(device: DiscoveredDeviceInfo)
    {
        with(mBinding) {

            adapter!!.add(device.toString())

            if (configuration.isDoubleDeviceConnection && recognizeEarSide(device.manufacturerData) == EarSide.Right)
                mAdapter.changeColorToRed(mDeviceCount, device.manufacturerData.substring(0, 6))

            else if (configuration.earSide == EarSide.Right)
                mAdapter.changeColorToRed(mDeviceCount, device.manufacturerData.substring(0, 6))

            adapter!!.notifyDataSetChanged()

            textViewDeviceState.text = String.format(
                "%d %s", ++mDeviceCount,
                getText(R.string.textview_count_device_found_text)
            )
        }
    }
    private fun searchHearingAidInList(pos: Int): DiscoveredDeviceInfo?
    {
        val device = mBinding.adapter!!.getItem(pos)
        return mBleDeviceList.find { it.toString() == device }
    }
    private fun selectDoubleItem(pos: Int)
    {
        val selectedDevice = searchHearingAidInList(pos)

        if (selectedDevice != null) {
            if (selectedDevice.manufacturerData.substring(0, 6) == MANUFACTURER_CODE_RIGHT)
                putItemToDeviceMap(EarSide.Right, selectedDevice)
            else
                putItemToDeviceMap(EarSide.Left, selectedDevice)

            if (configuration.doubleDeviceMap.size == 2)
                finalizeConnectionConfiguration()
        }
        else
            showConnectionProblemToastMessage()
    }
    private fun selectSingleItem(pos: Int)
    {
        val selectedDevice = searchHearingAidInList(pos)

        if (selectedDevice != null) {
            mSelectedHearingAid = HearingAid(
                selectedDevice.address, selectedDevice.name, selectedDevice.rssi,
                selectedDevice.manufacturerData, recognizeEarSide(selectedDevice.manufacturerData)
            )

            configuration.hearingAid = mSelectedHearingAid
            finalizeConnectionConfiguration()

        } else
            showConnectionProblemToastMessage()
    }

    private fun showConnectionProblemToastMessage()
    {
        Toast.makeText(this, R.string.toast_connection_problem_text, Toast.LENGTH_LONG)
            .show()
    }
    fun buttonBackClicked()
    {
        if (isScanContinue)
            stopScanButtonClicked()

        if (configuration.isConnected)
            AlertDialog.Builder(this).setMessage(R.string.dialog_already_connected_message)
                .setTitle(R.string.alert_dialog_connection_problem_title)
                .setPositiveButton(R.string.dialog_yes) {_, _ -> alertDialogYesButtonCallback(); finish()}
                .setCancelable(false)
                .setNegativeButton(R.string.dialog_no) {_, _ -> }
                .create().show()
        else
            finish()
    }
    fun buttonDetectDeviceClicked()
    {
        if (!configuration.isConnected) {
            mBleDeviceList.clear()
            mDeviceCount = 0
            mIsDeviceFound = false
            scanDevicesWithBluetooth()
            with(mBinding) {
                progressBar.visibility = View.VISIBLE
                frameLayoutStopScan.visibility = View.VISIBLE
                imageButtonDetect.isEnabled = false
            }
            scanDevices()
            isScanContinue = true
        } else
            AlertDialog.Builder(this).setMessage(R.string.dialog_already_connected_message)
                .setPositiveButton(R.string.dialog_yes) { _, _ -> alertDialogYesButtonCallback()}
                .setNegativeButton(R.string.dialog_no) {_,_ ->  startActivity(Intent(this, ControlActivity::class.java)); finish()}
                .setCancelable(false)
                .setTitle(R.string.alert_dialog_connection_problem_title).create().show()
    }

    fun hearingAidItemClicked(pos: Int)
    {
        if (configuration.isDoubleDeviceConnection) {
            selectDoubleItem(pos)
            return
        }

        selectSingleItem(pos)
    }
    private fun scanDevices()
    {
        with(mBinding) {
            textViewDeviceState.text =
                getString(R.string.textview_device_detection_not_found_text)
            adapter!!.clear()
            adapter!!.notifyDataSetChanged()
        }
        threadPool.execute { scanThreadCallBack() }
    }

    fun stopScanButtonClicked()
    {
        try {
            @Synchronized
            fun stop() {
                scanOnBoard.interrupt()
                eventHandler.exit()
            }
            stop()
            isScanContinue = false
            deviceConnectionHandler.endScanProcess(mAsyncResult)

            with(mBinding) {
                imageButtonDetect.isEnabled = true
                progressBar.visibility = View.GONE
                frameLayoutStopScan.visibility = View.INVISIBLE
            }
        } catch (ex: ArkException) {
            Log.e("err", ex.message!!)
        } catch (_: Throwable) {
            Toast.makeText(this, R.string.toast_unknown_problem_text, Toast.LENGTH_LONG).show()
        }
    }
}
