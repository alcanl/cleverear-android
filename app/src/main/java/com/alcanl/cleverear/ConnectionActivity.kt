package com.alcanl.cleverear

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityConnectionBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.DEVICE_MAC_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_MAC_KEY_R
import com.alcanl.cleverear.helpers.DEVICE_MANU_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_MANU_KEY_R
import com.alcanl.cleverear.helpers.DEVICE_NAME_DEFAULT_L
import com.alcanl.cleverear.helpers.DEVICE_NAME_DEFAULT_R
import com.alcanl.cleverear.helpers.DEVICE_NAME_EDITED
import com.alcanl.cleverear.helpers.DEVICE_NAME_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_NAME_KEY_R
import com.alcanl.cleverear.helpers.DEVICE_NAME_PREF
import com.alcanl.cleverear.helpers.DEVICE_RSSI_KEY_L
import com.alcanl.cleverear.helpers.DEVICE_RSSI_KEY_R
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.helpers.connection.DeviceConnectionHandler
import com.alcanl.cleverear.helpers.parseJsonScanData
import com.alcanl.cleverear.sdk.events.ScanEvent
import com.alcanl.cleverear.viewmodel.ConnectionActivityListenersViewModel
import com.ark.ArkException
import com.ark.AsyncResult
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityConnectionBinding
    @Inject
    lateinit var deviceConnectionHandler: DeviceConnectionHandler
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    @Volatile
    var mBleDeviceList = ArrayList<DiscoveredDeviceInfo>()
    @Volatile
    private var mDeviceCount = 0
    private var isScanContinue = false
    private lateinit var mPrefs: SharedPreferences
    private lateinit var scanOnBoard : Future<*>
    private lateinit var mAsyncResult: AsyncResult
    private lateinit var mSelectedHearingAid: HearingAid
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        loadPreferences()
        buttonDetectDeviceClicked()
    }

    override fun onStop() {
        super.onStop()
        if (isScanContinue)
            stopScanButtonClicked()
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
        with(configuration) {
            if (isDoubleDeviceConnection)
                doubleDeviceMap.forEach {it.value.endConnectionSession()}
            else
                hearingAid.endConnectionSession()

            isConnected = false
        }
        mBinding.textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
    }
    private fun finalizeConnectionConfiguration() {

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
        mBinding.adapter =
            ArrayAdapter(this, R.layout.listview_item_layout, ArrayList<String>(emptyList()))
                .apply {
                    if (configuration.isDoubleDeviceConnection) mBinding.listView.choiceMode =
                        ListView.CHOICE_MODE_MULTIPLE
                }
    }
    private fun isDuplicate(device: DiscoveredDeviceInfo): Boolean {
        var value = true

        for (deviceWrapper in mBleDeviceList)
            if (deviceWrapper.address == device.address) {
                value = false
                break
            }

        return value
    }

    private fun isHearingAid(manufacturerCode: String): Boolean {
        var manufacturerCodeSub = ""

        try {
            manufacturerCodeSub = manufacturerCode.substring(0, 6)
        } catch (_: StringIndexOutOfBoundsException) {
        }

        return if (!configuration.isDoubleDeviceConnection)
            if (configuration.earSide == EarSide.Left) manufacturerCodeSub == MANUFACTURER_CODE_LEFT
            else manufacturerCodeSub == MANUFACTURER_CODE_RIGHT
        else
            manufacturerCodeSub == MANUFACTURER_CODE_RIGHT || manufacturerCodeSub == MANUFACTURER_CODE_LEFT
    }
    private fun loadPreferences()
    {
        mPrefs = getSharedPreferences(DEVICE_NAME_PREF, MODE_PRIVATE)
    }
    private fun isConnectedBefore(deviceName: String) : Boolean
    {
        return if (!configuration.isDoubleDeviceConnection)
                    if (configuration.earSide == EarSide.Left)
                        mPrefs.getString(DEVICE_NAME_KEY_L, DEVICE_NAME_DEFAULT_L)!! == deviceName
                    else
                        mPrefs.getString(DEVICE_NAME_KEY_R, DEVICE_NAME_DEFAULT_R)!! == deviceName
                else
                        mPrefs.getString(DEVICE_NAME_KEY_L, DEVICE_NAME_DEFAULT_L)!! == deviceName
                                || mPrefs.getString(DEVICE_NAME_KEY_R, DEVICE_NAME_DEFAULT_R)!! == deviceName

    }
    private fun handleConnectedBeforeDevice()
    {
        with(configuration) {
            if (!isDoubleDeviceConnection) {
                hearingAid = if (earSide == EarSide.Left) getConnectedBeforeLeftDevice() else
                    getConnectedBeforeRightDevice()

                finalizeConnectionConfiguration()
            }
            else {
                doubleDeviceMap[EarSide.Right] = getConnectedBeforeRightDevice()
                doubleDeviceMap[EarSide.Left] = getConnectedBeforeLeftDevice()

                if (doubleDeviceMap.size == 2)
                    finalizeConnectionConfiguration()
            }
        }
    }
    private fun getConnectedBeforeLeftDevice() : HearingAid
    {
        val name = mPrefs.getString(DEVICE_NAME_KEY_L, "")!!
        val rssi = mPrefs.getInt(DEVICE_RSSI_KEY_L, 0)
        val address = mPrefs.getString(DEVICE_MAC_KEY_L, "")
        val manufacturer = mPrefs.getString(DEVICE_MANU_KEY_L, "")

        return HearingAid(address!!, name, rssi, manufacturer!!, EarSide.Left)
    }
    private fun getConnectedBeforeRightDevice() : HearingAid
    {
        val name = mPrefs.getString(DEVICE_NAME_KEY_R, "")!!
        val rssi = mPrefs.getInt(DEVICE_RSSI_KEY_R, 0)
        val address = mPrefs.getString(DEVICE_MAC_KEY_R, "")
        val manufacturer = mPrefs.getString(DEVICE_MANU_KEY_R, "")

        return HearingAid(address!!, name, rssi, manufacturer!!, EarSide.Right)
    }
    private fun putItemToDeviceMap(earSide: EarSide, device: DiscoveredDeviceInfo) {
        if (configuration.doubleDeviceMap.containsKey(earSide))
            return

        configuration.doubleDeviceMap[earSide] = HearingAid(
            device.address,
            device.name, device.rssi, device.manufacturerData, earSide
        )
    }
    private fun recognizeEarSide(str: String): EarSide {
        return if (str.substring(0, 6) == MANUFACTURER_CODE_LEFT) EarSide.Left else EarSide.Right
    }


    private fun scanDevicesWithBluetooth() {
        try {
            mAsyncResult =
                if (configuration.earSide == EarSide.Right) deviceConnectionHandler.scanForRightDevice()
                else deviceConnectionHandler.scanForLeftDevice()

        } catch (ex: ArkException) {
            Log.e("", ex.message!!)
            Toast.makeText(this, R.string.toast_connection_problem_text, Toast.LENGTH_LONG).show()
        }
    }
    private fun scanThreadCallBack()
    {
            while (!configuration.isConnected) {
                val event = eventHandler.event
                if (event.type == EventType.kScanEvent) {
                    Log.e(ScanEvent::class.java.simpleName, event.data)
                    val device = parseJsonScanData(event.data, this)
                    if (isHearingAid(device.manufacturerData)) {
                        mBleDeviceList.add(device)
                        Log.e(String::class.java.simpleName, mBleDeviceList.size.toString())
                        if (!isDuplicate(device)) {
                            runOnUiThread {
                                mBinding.adapter!!.add(if (isConnectedBefore(device.name))
                                    DEVICE_NAME_EDITED
                                        else device.name)
                                mBinding.adapter!!.notifyDataSetChanged()
                                mBinding.textViewDeviceState.text = String.format(
                                    "%d %s", ++mDeviceCount,
                                    getText(R.string.textview_count_device_found_text)
                                )
                            }
                        }
                    }
                }
            }
    }
    private fun searchHearingAidInList(pos: Int): DiscoveredDeviceInfo? {
        val device = mBinding.adapter!!.getItem(pos)
        return mBleDeviceList.find { it.name == device.toString() }
    }

    private fun selectDoubleItem(pos: Int) {
        val selectedDevice = searchHearingAidInList(pos)

        if (selectedDevice != null) {
            if (selectedDevice.manufacturerData.substring(0, 6) == MANUFACTURER_CODE_RIGHT)
                putItemToDeviceMap(EarSide.Right, selectedDevice)
            else
                putItemToDeviceMap(EarSide.Left, selectedDevice)


            if (configuration.doubleDeviceMap.size == 2) {
                finalizeConnectionConfiguration()
            }
        } else
            showConnectionProblemToastMessage()
    }
    private fun selectSingleItem(pos: Int) {
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
    fun buttonDetectDeviceClicked() {

        if (!configuration.isConnected) {
            mBleDeviceList.clear()
            mDeviceCount = 0
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
                .setNegativeButton(R.string.dialog_no) {_,_ ->  }
                .setCancelable(true)
                .setTitle(R.string.alert_dialog_connection_problem_title).create().show()
    }

    fun hearingAidItemClicked(pos: Int)
    {
        if (mBinding.adapter!!.getItem(pos)!! == DEVICE_NAME_EDITED) {
            handleConnectedBeforeDevice()
            return
        }

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
        scanOnBoard = threadPool.submit { scanThreadCallBack() }
    }

    fun stopScanButtonClicked()
    {
        isScanContinue = false
        scanOnBoard.cancel(false)
        deviceConnectionHandler.endScanProcess(mAsyncResult)

        with(mBinding) {
            imageButtonDetect.isEnabled = true
            progressBar.visibility = View.GONE
            frameLayoutStopScan.visibility = View.INVISIBLE
        }
    }


}
