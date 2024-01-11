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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityConnectionBinding
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.BUNDLE_EAR_SIDE_KEY
import com.alcanl.cleverear.helpers.DOUBLE_DEVICE_MAP
import com.alcanl.cleverear.helpers.DOUBLE_DEVICE_SELECTION
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HEARING_AID
import com.alcanl.cleverear.helpers.IS_DOUBLE_DEVICE
import com.alcanl.cleverear.helpers.LEFT
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.helpers.SHARED_PREF_FILE_NAME
import com.alcanl.cleverear.helpers.connection.DeviceConnectionHandler
import com.alcanl.cleverear.helpers.parseJsonScanData
import com.alcanl.cleverear.sdk.events.ScanEvent
import com.alcanl.cleverear.viewmodel.ConnectionActivityListenersViewModel
import com.ark.ArkException
import com.ark.AsyncResult
import com.ark.EventHandler
import com.ark.EventType
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityConnectionBinding
    @Inject
    lateinit var productManager : ProductManager
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var deviceConnectionHandler: DeviceConnectionHandler
    @Inject
    lateinit var threadPool: ExecutorService
    @Volatile
    private var mIsDeviceFound = false
    private var mIsConnected = false
    private var mBluetoothPermission = false
    private var mIsDoubleDeviceConnection = false
    private val mBleDeviceList = ArrayList<DiscoveredDeviceInfo>()
    private val mDoubleDeviceMap = HashMap<EarSide, HearingAid>()
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
            scanDevicesWithBluetooth()
    }
    else
        mBluetoothPermission = false }
    private val mActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
        result : ActivityResult ->
        if (result.resultCode == RESULT_OK)
            scanDevicesWithBluetooth()
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        getEarSideSelection()
        buttonDetectDevice()
    }
    override fun onStop()
    {
        super.onStop()
        val sharedPrefEditor = getSharedPreferences(SHARED_PREF_FILE_NAME, MODE_PRIVATE).edit()
        sharedPrefEditor.putBoolean(IS_DOUBLE_DEVICE, mIsDoubleDeviceConnection).apply()
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
    private fun isDuplicate(device: DiscoveredDeviceInfo): Boolean
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
        var manufacturerCodeSub = ""

        try {
            manufacturerCodeSub = manufacturerCode.substring(0, 6)
        } catch (_ : StringIndexOutOfBoundsException) {}

        return if (!mIsDoubleDeviceConnection)
                    if (mEarSide == EarSide.Left) manufacturerCodeSub == MANUFACTURER_CODE_LEFT
                    else manufacturerCodeSub == MANUFACTURER_CODE_RIGHT
               else
                    manufacturerCodeSub == MANUFACTURER_CODE_RIGHT || manufacturerCodeSub == MANUFACTURER_CODE_LEFT
    }
    private fun scanSingleDeviceWithBluetooth()
    {
        try {
            mAsyncResult = if (mEarSide == EarSide.Right) deviceConnectionHandler.scanForLeftDevice()
            else deviceConnectionHandler.scanForRightDevice()

        } catch (ex: ArkException) {
            Log.e("",ex.message!!)
            Toast.makeText(this, "Failed to connecting the device", Toast.LENGTH_LONG).show()
        }
    }
    private fun scanDevicesWithBluetooth()
    {
        scanSingleDeviceWithBluetooth()
        /*when {
            !mIsDoubleDeviceConnection -> scanSingleDeviceWithBluetooth()
            else -> scanDoubleDevicesWithBluetooth()
        }*/
    }
    /*private fun scanDoubleDevicesWithBluetooth()
    {
        thread(isDaemon = true, start = true) {
             try {
                 mAsyncResult = deviceConnectionHandler.scanForLeftDevice()
                 Thread.sleep(4000)
                 deviceConnectionHandler.endScanProcess(mAsyncResult)
                 Thread.sleep(1000)
                 mAsyncResult = deviceConnectionHandler.scanForRightDevice()
                 Thread.sleep(4000)
                 deviceConnectionHandler.endScanProcess(mAsyncResult)

            } catch (ex: ArkException) {
                 Log.e("",ex.message!!)
                 Toast.makeText(this, "Failed to connecting the device", Toast.LENGTH_LONG).show()
            }
        }
    }*/

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
        val selectedDevice = searchHearingAidInList(pos)

        if (selectedDevice != null) {
            if (selectedDevice.manufacturerData.substring(0, 6) == MANUFACTURER_CODE_RIGHT)
                putItemToDeviceMap(EarSide.Right, selectedDevice)
            else
                putItemToDeviceMap(EarSide.Left, selectedDevice)


            if (mDoubleDeviceMap.size == 2) {
                val bundle = Bundle()
                bundle.putSerializable(DOUBLE_DEVICE_MAP, mDoubleDeviceMap)
                finalizeConnectionConfiguration(bundle)
            }
        }
        else
            showConnectionProblemToastMessage()
    }
    private fun putItemToDeviceMap(earSide: EarSide, device: DiscoveredDeviceInfo)
    {
        if (mDoubleDeviceMap.containsKey(earSide))
            return

        mDoubleDeviceMap[earSide] = HearingAid(device.address,
            device.name, device.rssi, device.manufacturerData, earSide)
    }
    private fun searchHearingAidInList(pos: Int): DiscoveredDeviceInfo?
    {
        val device = mBinding.adapter!!.getItem(pos)
        return mBleDeviceList.find { it.name == device.toString() }
    }
    private fun selectSingleItem(pos: Int)
    {
        val selectedDevice = searchHearingAidInList(pos)

        if (selectedDevice != null) {
            mSelectedHearingAid = HearingAid(
                selectedDevice.address, selectedDevice.name, selectedDevice.rssi,
                selectedDevice.manufacturerData, recognizeEarSide(selectedDevice.manufacturerData)
            )

            val bundle = Bundle()
            bundle.putSerializable(HEARING_AID, mSelectedHearingAid)
            finalizeConnectionConfiguration(bundle)

        }
        else
            showConnectionProblemToastMessage()
    }
    private fun recognizeEarSide(str: String) : EarSide
    {
        return if (str.substring(0,6) == MANUFACTURER_CODE_LEFT) EarSide.Left else EarSide.Right
    }
    private fun showConnectionProblemToastMessage()
    {
        Toast.makeText(this, R.string.alert_dialog_connection_problem_message, Toast.LENGTH_LONG).show()
    }
    private fun finalizeConnectionConfiguration(bundle: Bundle)
    {
        mIsDeviceFound = true

        stopScanButtonClicked()

        Intent(this, ControlActivity::class.java).apply {
            putExtras(bundle)
            setResult(RESULT_OK, this)
            startActivity(this)
        }
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
        productManager.endScanForWirelessDevices(mAsyncResult)
        finish()
    }
    private fun scanDevices()
    {
        var deviceCount = 0
        mBinding.textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
        mBinding.adapter!!.clear()
        mBinding.adapter!!.notifyDataSetChanged()

        try {
            threadPool.execute {
                while (!mIsDeviceFound) {
                    val event = eventHandler.event
                    if (event.type == EventType.kScanEvent) {
                        Log.e(ScanEvent::class.java.simpleName, event.data)
                        val device = parseJsonScanData(event.data, this)
                        mBleDeviceList.add(device)
                        Log.e(String::class.java.simpleName, mBleDeviceList.size.toString())
                        if (!isDuplicate(device) && isHearingAid(device.manufacturerData)) {
                            runOnUiThread {
                                mBinding.adapter!!.add(device.name); mBinding.adapter!!.notifyDataSetChanged()
                                mBinding.textViewDeviceState.text = String.format(
                                    "%d %s", ++deviceCount,
                                    getText(R.string.textview_count_device_found_text)) } } } }
            }
        } catch (_ : IndexOutOfBoundsException)
        {
            Toast.makeText(this, "ScanMemoryLeak", Toast.LENGTH_LONG).show()
        }
    }
    fun stopScanButtonClicked()
    {
        productManager.endScanForWirelessDevices(mAsyncResult)
        mBinding.imageButtonDetect.isEnabled = true
        mBinding.progressBar.visibility = View.GONE
        mBinding.frameLayoutStopScan.visibility = View.INVISIBLE
    }
}