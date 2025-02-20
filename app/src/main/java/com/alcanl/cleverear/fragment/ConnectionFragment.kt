package com.alcanl.cleverear.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentConnectionBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.util.adapter.DeviceListAdapter
import com.alcanl.cleverear.util.dialog.createDeviceDisconnectSuccessDialog
import com.alcanl.cleverear.util.dialog.showStopConnectionDialog
import com.alcanl.cleverear.util.dpToPx
import com.alcanl.cleverear.util.extension.recognizeEarSide
import com.alcanl.cleverear.util.extension.startAlphaBlinkAnimation
import com.alcanl.cleverear.util.extension.startTextAlphaAnimation
import com.alcanl.cleverear.util.getSpannableDeviceString
import com.alcanl.cleverear.util.listeners.ConnectionFragmentEventListener
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.listeners.event.DeviceFoundEvent
import com.alcanl.cleverear.util.markServiceAsClosed
import com.alcanl.cleverear.util.scan.DeviceScanner
import com.alcanl.cleverear.util.service.AutoConnectService
import com.alcanl.cleverear.viewmodel.ConnectionFragmentListenersViewModel
import com.ark.ArkException
import com.ark.AsyncResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.EventObject
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionFragment : Fragment(), IEventListener {

    @Inject
    lateinit var deviceScanner: DeviceScanner

    @Inject
    lateinit var configuration: ConfigurationModule

    @Inject
    lateinit var deviceListAdapter: DeviceListAdapter

    @Inject
    lateinit var listener: ConnectionFragmentEventListener

    @Inject
    lateinit var handler: Handler

    @Volatile
    private var mDeviceCount = 0

    @Volatile
    private var mBleDeviceSet = HashSet<DiscoveredDeviceInfo>()

    private var mIsDeviceFound = false

    private var isScanContinue = false

    private lateinit var mAsyncResult: AsyncResult

    private lateinit var mSelectedHearingAid: HearingAid

    private lateinit var mBinding: FragmentConnectionBinding

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is ConnectionEvent && event.connectionData == 1 && !configuration.isUserDisconnected)
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(requireContext(), R.string.toast_connection_lost_text, Toast.LENGTH_SHORT).show()

                if (configuration.isServiceOnBoard) {
                    markServiceAsClosed(requireContext())
                    requireActivity().stopService(
                        Intent(
                            requireActivity(),
                            AutoConnectService::class.java
                        )
                    )
                }
                requireActivity().finish()
                configuration.apply {
                    isConnected = false
                    hearingAidMap.filter { ha -> ha.value.address == event.eventSender }
                        .values.first().isConnected = false
                }
            }

        else if (event is DeviceFoundEvent) {
            Thread.sleep(2000)
            mBleDeviceSet.add(event.hearingAid)
            lifecycleScope.launch(Dispatchers.Main) { addHearingAidToListCallback(event.hearingAid) }
        }
    }

    override fun onStart()
    {
        super.onStart()
        initBinding()

        EventBus.getDefault().apply {
            if (!isRegistered(this@ConnectionFragment))
                register(this@ConnectionFragment)
        }

        if (!configuration.isConnected)
            onDetectDeviceButtonClicked()
    }

    override fun onDestroy()
    {
        super.onDestroy()

        EventBus.getDefault().apply {
            if (isRegistered(this@ConnectionFragment))
                unregister(this@ConnectionFragment)
        }

        listener.apply {
            if (isListenerOnBoard())
                stopListener()
        }
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

        listener.apply {
            if (isListenerOnBoard())
                stopListener()
        }

        super.onStop()
    }

    override fun onResume()
    {
        super.onResume()

        configuration.apply {
            if (!isConnected) {
                mBleDeviceSet.clear()
                hearingAidMap.clear()
            }
        }

        listener.apply {
            if (!isListenerOnBoard())
                startEventListener()
        }

        mBinding.apply {

            imageButtonDetect.isEnabled = !configuration.isConnected
            buttonStopConnection.visibility = if (configuration.isConnected) View.VISIBLE else View.INVISIBLE
            groupBoxLayoutDeviceList.visibility = View.INVISIBLE

            configuration.apply {
                if (isConnected) {
                    imageButtonDetect.apply {
                        setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.bluetooth_connected))
                        val params = layoutParams
                        params.width = dpToPx(requireContext(), 200)
                        params.height = dpToPx(requireContext(), 200)
                        layoutParams = params
                    }
                    when {
                        !isDoubleDeviceConnection -> textViewDeviceState.apply {
                            text = getSpannableDeviceString(requireContext(), hearingAidMap[earSide]!!)
                            visibility = View.VISIBLE
                        }
                        else -> {
                            textViewDeviceState.apply {
                                text = getSpannableDeviceString(
                                    requireContext(),
                                    hearingAidMap[EarSide.Left]!!
                                )
                                visibility = View.VISIBLE
                            }
                            textViewDeviceStateSecond.apply {
                                text = getSpannableDeviceString(
                                    requireContext(),
                                    hearingAidMap[EarSide.Right]!!
                                )
                                visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
            textViewDeviceScan.startTextAlphaAnimation(requireContext(), getString(if (configuration.isConnected)  R.string.textview_scan_process_connected_text else
                if (isScanContinue) R.string.textview_scan_process_scanning_text else R.string.textview_detect_device_text))
        }
    }

    private fun alertDialogPositiveButtonCallback()
    {
        mBinding.apply {
            textViewDeviceScan.startTextAlphaAnimation(requireContext(), getString(R.string.textview_detect_device_text))
            textViewDeviceState.apply {
                text = getString(R.string.textview_device_detection_not_found_text)
                visibility = View.INVISIBLE
            }
            textViewDeviceStateSecond.apply {
                text = getString(R.string.textview_device_detection_not_found_text)
                visibility = View.INVISIBLE
            }
            frameLayoutStopConnection.visibility = View.GONE
            imageButtonDetect.isEnabled = true
        }

        createDeviceDisconnectSuccessDialog(requireActivity())
            .apply { show() }.also { handler.postDelayed({ it.dismiss() }, 1500) }
    }

    private fun finalizeConnectionConfiguration()
    {
        mIsDeviceFound = true
        startActivity(Intent(requireActivity(), LoadingActivity::class.java))

        if (isScanContinue)
            stopScanProcess()
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

    private fun putItemToDeviceMap(earSide: EarSide, device: DiscoveredDeviceInfo)
    {
        configuration.apply {
            if (hearingAidMap.containsKey(earSide))
                return

            hearingAidMap[earSide] = HearingAid(
                device.address,
                device.name, device.rssi, device.manufacturerData, earSide
            )
        }
    }

    private fun scanDevicesWithBluetooth()
    {
        try {
            mAsyncResult =
                if (configuration.earSide == EarSide.Right) deviceScanner.scanForRightDevice()
                else deviceScanner.scanForLeftDevice()

        } catch (ex: ArkException) {
            Log.e(ConnectionFragment::class.simpleName, ex.message!!)
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(requireContext(), R.string.toast_connection_problem_text, Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun addHearingAidToListCallback(device: DiscoveredDeviceInfo)
    {
        mBinding.apply {

            if (adapter!!.isEmpty)
                groupBoxLayoutDeviceList.startAlphaBlinkAnimation()

            adapter!!.add(device.toString())

            if (configuration.isDoubleDeviceConnection && device.manufacturerData.recognizeEarSide() == EarSide.Right)
                deviceListAdapter.changeColorToRed(mDeviceCount, device.manufacturerData.substring(0, 6))

            else if (configuration.earSide == EarSide.Right)
                deviceListAdapter.changeColorToRed(mDeviceCount, device.manufacturerData.substring(0, 6))

            adapter!!.notifyDataSetChanged()
            textViewDeviceState.text = String.format("%d %s", ++mDeviceCount, getText(R.string.textview_count_device_found_text))
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
                selectedDevice.manufacturerData, selectedDevice.manufacturerData.recognizeEarSide()
            )

            configuration.hearingAidMap[configuration.earSide] = mSelectedHearingAid
            finalizeConnectionConfiguration()

        } else
            showConnectionProblemToastMessage()
    }

    private fun showConnectionProblemToastMessage()
    {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), R.string.toast_connection_problem_text, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun scanDevices()
    {
        mBleDeviceSet.clear()
        mDeviceCount = 0
        mIsDeviceFound = false

        mBinding.apply {
            groupBoxLayoutDeviceList.visibility = View.INVISIBLE
            textViewDeviceScan.startTextAlphaAnimation(requireContext(),getString(R.string.textview_scan_process_scanning_text))
            textViewDeviceState.text = getString(R.string.textview_device_detection_not_found_text)
            textViewDeviceStateSecond.visibility = View.INVISIBLE
            adapter!!.clear()
            adapter!!.notifyDataSetChanged()
        }

        scanDevicesWithBluetooth()
        isScanContinue = true
    }

    private fun stopScanProcess()
    {
        try {
            isScanContinue = false
            deviceScanner.endScanProcess(mAsyncResult)
            mBinding.apply {
                pulseLayout.stop()
                textViewDeviceScan.startTextAlphaAnimation(requireContext(), getString(R.string.textview_detect_device_text))
            }

        } catch (ex: ArkException) {
            Log.e(ConnectionFragment::class.simpleName, ex.message!!)

        } catch (_: Throwable) {
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(requireContext(),
                    R.string.toast_unknown_problem_text,
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    fun onBackButtonClicked()
    {
        if (isScanContinue)
            stopScanProcess()

        if (configuration.isConnected)
            onStopConnectionButtonClicked()
        else
            requireActivity().finish()
    }

    fun onDetectDeviceButtonClicked()
    {
        mBinding.apply {
            pulseLayout.startAlphaBlinkAnimation()
            textViewDeviceState.visibility = View.VISIBLE
        }

        if (isScanContinue) {
            stopScanProcess()
            return
        }

        if (!configuration.isConnected)
            scanDevices()
    }

    private fun onAlertDialogPositiveButtonClicked()
    {
        mBinding.apply {
            imageButtonDetect.apply {
                setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.bluetooth_not_connected))
                val params = layoutParams
                params.width = dpToPx(requireContext(), 110)
                params.height = dpToPx(requireContext(), 110)
                layoutParams = params
            }
            frameLayoutStopConnection.visibility = View.VISIBLE
            buttonStopConnection.visibility = View.INVISIBLE
        }

        configuration.apply {
            isUserDisconnected = true

            if (isServiceOnBoard) {
                keepConnectionForServiceDestroy = false
                markServiceAsClosed(requireContext())
                requireActivity().stopService(
                    Intent(
                        requireActivity(),
                        AutoConnectService::class.java
                    )
                )
            }
            else
                if (isConnected) {
                    if (isDoubleDeviceConnection)
                        hearingAidMap.forEach { it.value.endConnectionSession() }
                    else
                        hearingAidMap[earSide]?.endConnectionSession()

                    isConnected = false
                }
        }

        handler.postDelayed (::alertDialogPositiveButtonCallback, 5000)
    }

    fun onHearingAidItemClicked(pos: Int)
    {
        if (configuration.isConnected) {
            onStopConnectionButtonClicked()
            return
        }

        if (configuration.isDoubleDeviceConnection) {
            selectDoubleItem(pos)
            return
        }

        selectSingleItem(pos)
    }

    fun onStopConnectionButtonClicked()
    {
        showStopConnectionDialog(requireActivity(), ::onAlertDialogPositiveButtonClicked)
    }
}

