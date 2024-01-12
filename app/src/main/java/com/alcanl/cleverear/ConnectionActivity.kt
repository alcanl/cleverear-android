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
import javax.inject.Inject
import kotlin.concurrent.thread

@AndroidEntryPoint
class ConnectionActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityConnectionBinding
    @Inject
    lateinit var productManager : ProductManager
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var deviceConnectionHandler: DeviceConnectionHandler
    @Volatile
    private var mBleDeviceList = ArrayList<DiscoveredDeviceInfo>()
    @Volatile
    private var mDeviceCount = 0
    private var mIsDeviceFound = false
    private var mIsConnected = false
    private var mBluetoothPermission = false
    private var mIsDoubleDeviceConnection = false
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
            this.scanDevicesWithBluetooth()
    }
    else
        mBluetoothPermission = false }
    private val mActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
        result : ActivityResult ->
        if (result.resultCode == RESULT_OK)
            this.scanDevicesWithBluetooth()
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        getEarSideSelection()
        buttonDetectDevice()
    }
    override fun onPause()
    {
        super.onPause()
        val sharedPrefEditor = getSharedPreferences(SHARED_PREF_FILE_NAME, MODE_PRIVATE).edit()
        sharedPrefEditor.putBoolean(IS_DOUBLE_DEVICE, mIsDoubleDeviceConnection).apply()
        stopScanButtonClicked()

    }
    private fun finalizeConnectionConfiguration(bundle: Bundle)
    {
        stopScanButtonClicked()

        Intent(this, ControlActivity::class.java).apply {
            putExtras(bundle)
            setResult(RESULT_OK, this)
            startActivity(this)
        }
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
    private fun putItemToDeviceMap(earSide: EarSide, device: DiscoveredDeviceInfo)
    {
        if (mDoubleDeviceMap.containsKey(earSide))
            return

        mDoubleDeviceMap[earSide] = HearingAid(device.address,
            device.name, device.rssi, device.manufacturerData, earSide)
    }
    private fun recognizeEarSide(str: String) : EarSide
    {
        return if (str.substring(0,6) == MANUFACTURER_CODE_LEFT) EarSide.Left else EarSide.Right
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
    private fun scanDevicesWithBluetooth()
    {
        try {
            mAsyncResult = if (mEarSide == EarSide.Right) deviceConnectionHandler.scanForLeftDevice()
            else deviceConnectionHandler.scanForRightDevice()

        } catch (ex: ArkException) {
            Log.e("",ex.message!!)
            Toast.makeText(this, R.string.toast_connection_problem_text, Toast.LENGTH_LONG).show()
        }
    }
    private fun scanThreadCallBack()
    {
        while (!mIsDeviceFound) {
            val event = eventHandler.event
            if (event.type == EventType.kScanEvent) {
                Log.e(ScanEvent::class.java.simpleName, event.data)
                val device = parseJsonScanData(event.data, this)
                if (isHearingAid(device.manufacturerData)) {
                    mBleDeviceList.add(device)
                    Log.e(String::class.java.simpleName, mBleDeviceList.size.toString())
                    if (!isDuplicate(device)) {
                        runOnUiThread {
                            mBinding.adapter!!.add(device.name); mBinding.adapter!!.notifyDataSetChanged()
                            mBinding.textViewDeviceState.text = String.format(
                                "%d %s", ++mDeviceCount,
                                getText(R.string.textview_count_device_found_text)) } } } } }
    }
    private fun searchHearingAidInList(pos: Int): DiscoveredDeviceInfo?
    {
        val device = mBinding.adapter!!.getItem(pos)
        return mBleDeviceList.find { it.name == device.toString() }
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
    private fun showConnectionProblemToastMessage()
    {
        Toast.makeText(this, R.string.alert_dialog_connection_problem_message, Toast.LENGTH_LONG).show()
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
        stopScanButtonClicked()
        finish()
    }
    private fun scanDevices()
    {
        mDeviceCount = 0
        mBleDeviceList.clear()
        mBinding.textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
        mBinding.adapter!!.clear()
        mBinding.adapter!!.notifyDataSetChanged()

        thread(start = true, isDaemon = true) { scanThreadCallBack() }
    }
    fun stopScanButtonClicked()
    {
        mIsDeviceFound = true
        deviceConnectionHandler.endScanProcess(mAsyncResult)
        mBinding.imageButtonDetect.isEnabled = true
        mBinding.progressBar.visibility = View.GONE
        mBinding.frameLayoutStopScan.visibility = View.INVISIBLE
    }
}