package com.alcanl.cleverear.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.MainContainerActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentConnectionBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.sdk.events.ScanEvent
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.util.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.util.adapter.DeviceListAdapter
import com.alcanl.cleverear.util.dialog.CustomAlertDialogActiveConnection
import com.alcanl.cleverear.util.extension.startAlphaBlinkAnimation
import com.alcanl.cleverear.util.parseJsonScanData
import com.alcanl.cleverear.util.scan.DeviceScanner
import com.alcanl.cleverear.viewmodel.ConnectionFragmentListenersViewModel
import com.ark.ArkException
import com.ark.AsyncResult
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject


@AndroidEntryPoint
class ConnectionFragment : Fragment() {

    @Inject
    lateinit var deviceScanner: DeviceScanner
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var deviceListAdapter: DeviceListAdapter
    @Volatile
    private var mDeviceCount = 0
    @Volatile
    private var mBleDeviceSet = HashSet<DiscoveredDeviceInfo>()
    private var mIsDeviceFound = false
    private var isScanContinue = false
    private lateinit var scanOnBoard : Thread
    private lateinit var mAsyncResult: AsyncResult
    private lateinit var mSelectedHearingAid: HearingAid
    private lateinit var mBinding: FragmentConnectionBinding
    private val mAlertDialog: CustomAlertDialogActiveConnection by lazy {
        CustomAlertDialogActiveConnection(this)
    }

    override fun onStart()
    {
        super.onStart()
        initBinding()
        if (!configuration.isConnected)
            buttonDetectDeviceClicked()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_connection, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    override fun onStop()
    {
        if (isScanContinue)
            stopScanProcess()

        super.onStop()
    }

    override fun onResume()
    {
        super.onResume()

        with(mBinding) {
            imageButtonDetect.isEnabled = !configuration.isConnected
            buttonStopConnection.visibility = if (configuration.isConnected) View.VISIBLE else View.INVISIBLE
            imageViewStopConnection.visibility = if (configuration.isConnected) View.VISIBLE else View.INVISIBLE
            textViewDeviceState.text = getString(if (configuration.isConnected)  R.string.device_connected_text else R.string.textview_device_detection_not_found_text)
            textViewScanProcess.text = getString(if (configuration.isConnected)  R.string.textview_scan_process_connected_text else
                if (isScanContinue) R.string.textview_scan_process_scanning_text else R.string.textview_detect_device_text)
        }
    }
    private fun finalizeConnectionConfiguration()
    {
        if (isScanContinue)
            stopScanProcess()

        mIsDeviceFound = true
        startActivity(Intent(activity, LoadingActivity::class.java))
    }

    private fun initBinding()
    {
        mBinding.viewModel = ConnectionFragmentListenersViewModel(this)
        deviceListAdapter.apply {
            if (configuration.isDoubleDeviceConnection) mBinding.listView.choiceMode =
                ListView.CHOICE_MODE_MULTIPLE
        }
        mBinding.adapter = deviceListAdapter
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
        if (configuration.hearingAidMap.containsKey(earSide))
            return

        configuration.hearingAidMap[earSide] = HearingAid(
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
                if (configuration.earSide == EarSide.Right) deviceScanner.scanForRightDevice()
                else deviceScanner.scanForLeftDevice()

        } catch (ex: ArkException) {
            Log.e("", ex.message!!)
            activity?.runOnUiThread {
                Toast.makeText(activity, R.string.toast_connection_problem_text, Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun scanThreadCallBack()
    {
        scanOnBoard = Thread.currentThread()
        mBleDeviceSet.clear()
        activity?.runOnUiThread {
            mBinding.adapter!!.clear()
            mBinding.adapter!!.notifyDataSetChanged() }

        while (!scanOnBoard.isInterrupted) {
            val event = eventHandler.event
            if (event.type == EventType.kScanEvent) {
                Log.e(ScanEvent::class.java.simpleName, event.data)
                val device = parseJsonScanData(event.data, requireContext())
                if (isHearingAid(device.manufacturerData)) {
                    mBleDeviceSet.add(device)
                    activity?.runOnUiThread { addHearingAidToListCallback(device) }
                }
            }
        }
        Log.e("scan", "Scan ended")
    }

    @SuppressLint("DefaultLocale")
    private fun addHearingAidToListCallback(device: DiscoveredDeviceInfo)
    {
        with(mBinding) {

            adapter!!.add(device.toString())

            if (configuration.isDoubleDeviceConnection && recognizeEarSide(device.manufacturerData) == EarSide.Right)
                deviceListAdapter.changeColorToRed(mDeviceCount, device.manufacturerData.substring(0, 6))

            else if (configuration.earSide == EarSide.Right)
                deviceListAdapter.changeColorToRed(mDeviceCount, device.manufacturerData.substring(0, 6))

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
        return mBleDeviceSet.find { it.toString() == device }
    }

    private fun selectDoubleItem(pos: Int)
    {
        val selectedDevice = searchHearingAidInList(pos)

        if (selectedDevice != null) {
            if (selectedDevice.manufacturerData.substring(0, 6) == MANUFACTURER_CODE_RIGHT)
                putItemToDeviceMap(EarSide.Right, selectedDevice)
            else
                putItemToDeviceMap(EarSide.Left, selectedDevice)

            if (configuration.hearingAidMap.size == 2)
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

            configuration.hearingAidMap[configuration.earSide] = mSelectedHearingAid
            finalizeConnectionConfiguration()

        } else
            showConnectionProblemToastMessage()
    }

    private fun showConnectionProblemToastMessage()
    {
        activity?.runOnUiThread {
            Toast.makeText(activity, R.string.toast_connection_problem_text, Toast.LENGTH_LONG)
                .show()
        }
    }

    fun buttonBackClicked()
    {
        if (isScanContinue)
            stopScanProcess()

        if (configuration.isConnected)
            mAlertDialog.show()
        else
            activity?.finish()
    }

    fun buttonDetectDeviceClicked()
    {
        mBinding.frameLayoutStartScan.startAlphaBlinkAnimation()

        if (isScanContinue) {
            stopScanProcess()
            return
        }

        if (!configuration.isConnected) {
            mBleDeviceSet.clear()
            mDeviceCount = 0
            mIsDeviceFound = false
            scanDevicesWithBluetooth()
            with(mBinding) {
                progressBar.visibility = View.VISIBLE
                textViewScanProcess.text = getString(R.string.textview_scan_process_scanning_text)
            }
            scanDevices()
            isScanContinue = true
        }
    }

    fun alertDialogPositiveButtonClicked()
    {
        mAlertDialog.dismiss()
        mBinding.frameLayoutStopConnection.visibility = View.VISIBLE
        mBinding.buttonStopConnection.visibility = View.INVISIBLE
        mBinding.imageViewStopConnection.visibility = View.INVISIBLE

        if (configuration.isServiceOnBoard) {
            (activity as MainContainerActivity).stopService()
            configuration.isServiceOnBoard = false
        }

        Handler(Looper.myLooper()!!).postDelayed (
            {
                with(configuration) {
                    if (isDoubleDeviceConnection)
                        hearingAidMap.forEach {it.value.endConnectionSession()}
                    else
                        hearingAidMap[earSide]?.endConnectionSession()

                    isConnected = false
                }
                mBinding.apply {
                    textViewScanProcess.text = getString(R.string.textview_detect_device_text)
                    textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
                    frameLayoutStopConnection.visibility = View.GONE
                    imageButtonDetect.isEnabled = true
                }
            }, 6000
        )
    }

    fun alertDialogNegativeButtonClicked()
    {
        mAlertDialog.dismiss()
    }

    fun hearingAidItemClicked(pos: Int)
    {
        if (configuration.isConnected) {
            mAlertDialog.show()
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
        mBinding.textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
        threadPool.execute { scanThreadCallBack() }
    }

    private fun stopScanProcess()
    {
        try {
            @Synchronized
            fun stop() {
                scanOnBoard.interrupt()
                eventHandler.exit()
            }

            stop()
            isScanContinue = false
            deviceScanner.endScanProcess(mAsyncResult)
            mBinding.textViewScanProcess.text = getString(R.string.textview_detect_device_text)
            mBinding.progressBar.visibility = View.GONE

        } catch (ex: ArkException) {
            Log.e("err", ex.message!!)
        } catch (_: Throwable) {
            activity?.runOnUiThread {
                Toast.makeText(activity,
                    R.string.toast_unknown_problem_text,
                    Toast.LENGTH_LONG).show()
            }

        }
    }

    fun stopConnectionButtonClicked()
    {
        mAlertDialog.show()
    }
}

