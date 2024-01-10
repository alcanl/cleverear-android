package com.alcanl.cleverear

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityConnectionBinding
import com.alcanl.cleverear.entity.DiscoveredDevice
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.BUNDLE_EAR_SIDE_KEY
import com.alcanl.cleverear.helpers.DOUBLE_DEVICE_SELECTION
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HEARING_AID
import com.alcanl.cleverear.helpers.LEFT
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.helpers.parseJsonScanData
import com.alcanl.cleverear.sdk.events.ScanEvent
import com.alcanl.cleverear.viewmodel.ConnectionActivityListenersViewModel
import com.ark.ArkException
import com.ark.AsyncResult
import com.ark.CommunicationPort
import com.ark.EventHandler
import com.ark.EventType
import com.ark.ProductManager
import com.ark.WirelessProgrammerType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.concurrent.thread

@AndroidEntryPoint
class ConnectionActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityConnectionBinding
    @Inject
    lateinit var mProductManager : ProductManager
    @Inject
    lateinit var mEventHandler: EventHandler
    @Volatile
    private var mIsDeviceFound = false
    private var mDeviceCount = 0
    private var mIsConnected = false
    private var mBluetoothPermission = false
    private var mIsDoubleDeviceConnection = false
    private val mBleDeviceList = ArrayList<DiscoveredDevice>()
    private lateinit var mEarSide : EarSide
    private lateinit var mAsyncResult : AsyncResult
    private lateinit var mSelectedHearingAid : HearingAid

    private var mBluetoothPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())


    {isGranted : Boolean -> if (isGranted)
    {
        val bluetoothManager = ContextCompat.getSystemService(this, BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter
        mBluetoothPermission = true
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            mActivityResultLauncher.launch(enableBluetoothIntent)
        }
        else
            scanDeviceWithBluetooth()
    }
    else
        mBluetoothPermission = false
    }
    private val mActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
        result : ActivityResult ->
        if (result.resultCode == RESULT_OK)
            scanDeviceWithBluetooth()
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        getEarSideSelection()
        buttonDetectDevice()
    }
    override fun onResume()
    {
        super.onResume()
        mBleDeviceList.clear()
        mBinding.adapter!!.clear()
        mBinding.textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
    }
    private fun initialize()
    {
        initBinding()
        mIsDoubleDeviceConnection = intent.getBooleanExtra(DOUBLE_DEVICE_SELECTION, false)
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_connection)
        mBinding.viewModel = ConnectionActivityListenersViewModel(this)
        mBinding.adapter = ArrayAdapter(this, R.layout.listview_item_layout, ArrayList<String>(emptyList()))
            .apply { if (mIsDoubleDeviceConnection) mBinding.listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE}

    }
    private fun getEarSideSelection()
    {
        mEarSide = when {
            intent.getStringExtra(BUNDLE_EAR_SIDE_KEY) == LEFT -> EarSide.Left
            else -> EarSide.Right
        }
    }
    private fun isDuplicate(device: DiscoveredDevice): Boolean
    {
        var value = true

        for (deviceWrapper in mBleDeviceList)
            if (deviceWrapper.address == device.address) {
                value = false
                break
        }

        return value
    }
    private fun isHearingAid(manufacturerCode: String) : Boolean
    {
        val manufacturerCodeSub = manufacturerCode.substring(0, 6)

        return if (mEarSide == EarSide.Left) manufacturerCodeSub == MANUFACTURER_CODE_LEFT
                else manufacturerCodeSub == MANUFACTURER_CODE_RIGHT
    }
    private fun scanDeviceWithBluetooth()
    {
        try {
                mAsyncResult = mProductManager.beginScanForWirelessDevices(
                WirelessProgrammerType.kPlatformDefault, "",
                if (mEarSide == EarSide.Left) CommunicationPort.kLeft else CommunicationPort.kRight,
                "",
                false
                )

        } catch (e: ArkException) {
            Log.e("",e.message!!)
            Toast.makeText(this, "Failed to connecting the device", Toast.LENGTH_LONG).show()
        }

    }

    private fun scanBluetooth()
    {
        val bluetoothManager = ContextCompat.getSystemService(this, BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null)
            Toast.makeText(this, R.string.toast_bluetooth_not_found_text, Toast.LENGTH_LONG).show()
        else
            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                mBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            }
            else {
                mBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
            }
    }
    private fun selectDoubleItem(pos: Int)
    {
        //
    }
    private fun selectSingleItem(pos: Int)
    {
        val device = mBinding.adapter!!.getItem(pos)
        val selectedDevice = mBleDeviceList.find { it.name == device.toString() }

        if (selectedDevice != null) {
            mSelectedHearingAid = HearingAid(
                selectedDevice.address, selectedDevice.name, selectedDevice.rssi,
                selectedDevice.manufacturerData, mEarSide
            )

            val bundle = Bundle()
            bundle.putSerializable(HEARING_AID, mSelectedHearingAid)
            mIsDeviceFound = true

            stopScanButtonClicked()

            Intent(this, ControlActivity::class.java).apply {
                putExtras(bundle)
                setResult(RESULT_OK, this)
                startActivity(this)
            }
        }
        else
            AlertDialog.Builder(this).setMessage(R.string.alert_dialog_connection_problem_message)
                .setTitle(R.string.alert_dialog_connection_problem_title)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_dialog_connection_problem_ok_button) {_,_ -> }
                .create().show()
    }
    fun buttonDetectDevice()
    {
        mIsDeviceFound = false
        scanBluetooth()

        if(!mIsConnected)
            mBinding.progressBar.visibility = View.VISIBLE

        scanDevices()
        mBinding.frameLayoutStopScan.visibility = View.VISIBLE
        mBinding.imageButtonDetect.isEnabled = false
    }
    fun hearingAidItemClicked(pos: Int)
    {
        if (mIsDoubleDeviceConnection) {
            selectDoubleItem(pos)
            return
        }

        selectSingleItem(pos)
    }
    fun buttonBack()
    {
        finish()
    }
    private fun scanDevices()
    {

        mBinding.adapter!!.notifyDataSetChanged()

        try {
            thread(isDaemon = false, start = true) {
                while (!mIsDeviceFound) {
                    val event = mEventHandler.event
                    if (event.type == EventType.kScanEvent) {
                        Log.e(ScanEvent::class.java.simpleName, event.data)
                        val device = parseJsonScanData(
                            event.data,
                            this
                        )
                        mBleDeviceList.add(device)
                        Log.e(String::class.java.simpleName, mBleDeviceList.size.toString())
                        if (!isDuplicate(device) && isHearingAid(device.manufacturerData)) {
                            runOnUiThread {
                                mBinding.adapter!!.add(device.name); mBinding.adapter!!.notifyDataSetChanged()
                                mBinding.textViewDeviceState.text = String.format(
                                    "%d %s", ++mDeviceCount,
                                    getText(R.string.textview_count_device_found_text)
                                )
                            }
                        }
                    }
                }
            }
        } catch (_ : IndexOutOfBoundsException)
        {
            Toast.makeText(this, "ScanMemoryLeak", Toast.LENGTH_LONG).show()
        }
    }
    fun stopScanButtonClicked()
    {
        mProductManager.endScanForWirelessDevices(mAsyncResult)
        mBinding.imageButtonDetect.isEnabled = true
        mBinding.progressBar.visibility = View.GONE
        mBinding.frameLayoutStopScan.visibility = View.INVISIBLE
    }
}