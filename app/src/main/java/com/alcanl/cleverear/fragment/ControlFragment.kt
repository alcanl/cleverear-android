package com.alcanl.cleverear.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableRow
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.MainContainerActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentControlBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.util.listeners.event.VolumeEvent
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.extension.startVolumeSliderFirstAnimation
import com.alcanl.cleverear.util.extension.startVolumeSliderSecondAnimation
import com.alcanl.cleverear.util.listeners.ControlFragmentEventListener
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.listeners.event.MemoryEvent
import com.alcanl.cleverear.viewmodel.ControlFragmentListenersViewModel
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ParameterSpace
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.EventObject
import javax.inject.Inject

@AndroidEntryPoint
class ControlFragment : Fragment(), IEventListener {

    @Inject
    lateinit var eventHandler: EventHandler

    @Inject
    lateinit var configuration : ConfigurationModule

    @Inject
    lateinit var listener : ControlFragmentEventListener

    @Inject
    lateinit var mutex: Mutex

    lateinit var mBinding: FragmentControlBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_control, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    override fun onStart()
    {
        super.onStart()
        initialize()
        EventBus.getDefault().register(this)
    }
    override fun onResume()
    {
        super.onResume()
        (activity as MainContainerActivity).apply {
            lifecycleScope.launch(Dispatchers.Main) {
                this@apply.binding.frameLayoutLoading.visibility = View.GONE
            }
        }

        if (!configuration.isConnected) {
            activity?.finish()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) { initSliders() }
        lifecycleScope.launch(Dispatchers.Main) { initTextViews() }
        lifecycleScope.launch(Dispatchers.Main) { initActiveMemory(if (configuration.isDoubleDeviceConnection)
            configuration.hearingAidMap[EarSide.Left]?.currentMemory?.value() ?: 0
        else
            configuration.hearingAidMap[configuration.earSide]?.currentMemory?.value() ?: 0)

        }

        if (!listener.isListenerOnBoard())
            listener.startEventListener()
    }
    override fun onStop()
    {
        super.onStop()

        if (listener.isListenerOnBoard())
            listener.stopListener()

    }
    override fun onDestroy() {
        super.onDestroy()

        EventBus.getDefault().unregister(this)
        if (listener.isListenerOnBoard())
            listener.stopListener()

    }
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (isVisible)
            when (event) {
                is VolumeEvent -> onVolumeEventReceived(event)
                is MemoryEvent -> onMemoryEventReceived(event)
                is BatteryEvent -> onBatteryEventReceived(event)
                is ConnectionEvent -> onConnectionEventReceived(event)
            }
    }
    private fun onBatteryEventReceived(event: BatteryEvent)
    {
        lifecycleScope.launch(Dispatchers.Main) {

            with(configuration) {
                if (!isDoubleDeviceConnection) {
                    if (earSide == EarSide.Left)
                        mBinding.batteryViewLeftDevice.batteryLevel = event.batteryData
                    else
                        mBinding.batteryViewRightDevice.batteryLevel = event.batteryData

                    hearingAidMap[earSide]?.batteryLevel = event.batteryData
                } else
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address) {
                        mBinding.batteryViewLeftDevice.batteryLevel = event.batteryData
                        hearingAidMap[EarSide.Left]?.batteryLevel = event.batteryData
                    }
                    else {
                        mBinding.batteryViewRightDevice.batteryLevel = event.batteryData
                        hearingAidMap[EarSide.Right]?.batteryLevel = event.batteryData
                    }
            }
        }
    }
    private fun onVolumeEventReceived(event: VolumeEvent)
    {
        lifecycleScope.launch(Dispatchers.Main) {

            with(configuration) {
                if (!isDoubleDeviceConnection) {
                    hearingAidMap[earSide]?.volume = event.volumeData
                    mBinding.sliderCommon.value = hearingAidMap[earSide]?.volume?.toFloat() ?: 0F
                }
                else
                    if (isDualDeviceConfig) {
                        hearingAidMap.forEach {  it.value.volume = event.volumeData  }
                        mBinding.sliderCommon.value = hearingAidMap[EarSide.Left]?.volume!!.toFloat()
                    }
                    else
                        if (event.eventSender == hearingAidMap[EarSide.Left]?.address) {
                            hearingAidMap[EarSide.Left]?.volume = event.volumeData
                            mBinding.sliderLeft.value = hearingAidMap[EarSide.Left]?.volume!!.toFloat()
                        }
                        else {
                            hearingAidMap[EarSide.Right]?.volume = event.volumeData
                            mBinding.sliderRight.value = hearingAidMap[EarSide.Right]?.volume!!.toFloat()
                        }
            }
        }
    }
    private fun onMemoryEventReceived(event: MemoryEvent)
    {
        lifecycleScope.launch(Dispatchers.Default) {

            with(configuration) {
                if (!isDoubleDeviceConnection)
                    hearingAidMap[earSide]?.currentMemory = ParameterSpace.value(event.memoryData)
                else
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address!!)
                        hearingAidMap[EarSide.Left]?.currentMemory = ParameterSpace.value(event.memoryData)
                    else
                        hearingAidMap[EarSide.Right]?.currentMemory = ParameterSpace.value(event.memoryData)
            }
            setSelectedMemoryButtonView(getMemoryEventButton(event.memoryData), event.memoryData)
        }
    }
    private fun onConnectionEventReceived(event: ConnectionEvent)
    {
        if (event.connectionData == 1)
            lifecycleScope.launch(Dispatchers.Main) {

                Toast.makeText(requireContext(), R.string.toast_connection_lost_text, Toast.LENGTH_SHORT).show()
                requireActivity().finish()
                configuration.apply {
                    isConnected = false
                    hearingAidMap.filter { ha -> ha.value.address == event.eventSender }
                        .values.first().isConnected = false
                }
            }
    }
    private fun getMemoryEventButton(memoryState: Int) : Button
    {
        return when (memoryState) {
            0 -> mBinding.buttonOne
            1 -> mBinding.buttonTwo
            2 -> mBinding.buttonThree
            3 -> mBinding.buttonFour
            4 -> mBinding.buttonFive
            5 -> mBinding.buttonSix
            6 -> mBinding.buttonSeven
            else -> mBinding.buttonEight
        }
    }
    private fun initBatteryViews()
    {
        if (configuration.isDoubleDeviceConnection) {
            with(configuration.hearingAidMap) {

                mBinding.batteryViewLeftDevice. apply {
                    batteryLevel = this@with[EarSide.Left]?.batteryLevel!!
                    mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
                }
                mBinding.batteryViewRightDevice.apply {
                    batteryLevel = this@with[EarSide.Right]?.batteryLevel!!
                    mBinding.batteryViewRightDevice.visibility = View.VISIBLE
                }
            }
        }
        else {

            with(configuration.hearingAidMap[configuration.earSide]!!) {
                if (side == EarSide.Left)
                    mBinding.batteryViewLeftDevice.apply {
                        batteryLevel = this@with.batteryLevel
                        visibility = View.VISIBLE
                    }
                else
                    mBinding.batteryViewRightDevice.apply {
                        batteryLevel = this@with.batteryLevel
                        visibility = View.VISIBLE
                    }
            }
        }
    }
    private fun initBinding()
    {
        mBinding.viewModel = ControlFragmentListenersViewModel(this)
    }
    private fun initDoubleDeviceConnectionSliders()
    {
        mBinding.sliderLeft.apply {
            visibility = View.VISIBLE
            mBinding.textViewLeftSeekBar.visibility = View.VISIBLE
        }
        mBinding.sliderRight.apply {
            visibility = View.VISIBLE
            mBinding.textViewRightSeekBar.visibility = View.VISIBLE
        }
        mBinding.sliderCommon.visibility = View.INVISIBLE
        mBinding.textViewCommon.visibility = View.INVISIBLE
    }
    private fun initialize()
    {
        initBinding()
        initMemoryButtons()
        initBatteryViews()
        initSwitch()

    }

    private fun initTextViews()
    {
        if (configuration.isDualDeviceConfig) {
            mBinding.apply {
                textViewLeftSeekBar.visibility = View.INVISIBLE
                textViewRightSeekBar.visibility = View.INVISIBLE
            }
        }
    }

    private fun initMemoryButtons()
    {
        lifecycleScope.launch(Dispatchers.Main) {
            initMemoryButtons(if (!configuration.isDoubleDeviceConnection) configuration.hearingAidMap[configuration.earSide]!!
            else configuration.hearingAidMap[EarSide.Left]!!)
        }
    }

    private fun initMemoryButtons(hearingAid: HearingAid)
    {
        checkEnabledMemoryCount(hearingAid).also {

            if (it > 4) {
                initMemoryButtons(4, mBinding.tableLayout.getChildAt(0) as TableRow)
                initMemoryButtons(it - 4, mBinding.tableLayout.getChildAt(1) as TableRow)
            } else
                initMemoryButtons(it, mBinding.tableLayout.getChildAt(0) as TableRow)
        }
    }
    private fun checkEnabledMemoryCount(hearingAid: HearingAid) : Int
    {
        var count = 0
        try {
            (0..<hearingAid.numberOfMemories).forEach {
                if (hearingAid.wirelessControl?.memoryEnabled(
                        ParameterSpace.value(it)
                    )!!
                ) ++count
            }
        } catch (_: ArkException) {}

        return count
    }
    private fun initMemoryButtons(bound: Int, tableRow: TableRow)
    {
        (0..<bound).forEach { tableRow.getChildAt(it).visibility = View.VISIBLE }
    }

    private fun initSliders()
    {
        if (configuration.isConnected) {
            if (!configuration.isDoubleDeviceConnection)
                initSliders(mBinding.sliderCommon, configuration.hearingAidMap[configuration.earSide]!!)

            else
                initDoubleDeviceConnectionSliders()

            if (configuration.isDualDeviceConfig)
                mBinding. apply {
                    sliderLeft.visibility = View.INVISIBLE
                    sliderRight.visibility = View.INVISIBLE
                    sliderCommon.visibility = View.VISIBLE
                }
        }
    }
    private fun initSliders(slider: Slider,  hearingAid: HearingAid)
    {
        slider.apply {
            value = hearingAid.volume.toFloat()
            trackActiveTintList = AppCompatResources.getColorStateList(
                requireContext(), if (hearingAid.side == EarSide.Left) R.color.left_device_slider
                else R.color.right_device_slider
            )
            thumbTintList = AppCompatResources.getColorStateList(
                requireContext(), if (hearingAid.side == EarSide.Left) R.color.left_device_slider
                else R.color.right_device_slider
            )
            mBinding.textViewCommon.text = getString(if (hearingAid.side == EarSide.Left) R.string.textview_left_device_text
            else R.string.textview_right_device_text)
        }
    }
    private fun initSwitch()
    {
        if (configuration.isDoubleDeviceConnection)
            mBinding.switchDualConfiguration.visibility = View.VISIBLE
    }
    private fun setButtonsDefaultColor(hearingAid: HearingAid, except: Int)
    {
        hearingAid.numberOfMemories.also {

            if (it > 4) {
                setButtonsDefaultColor(4, mBinding.tableLayout.getChildAt(0) as TableRow, except)
                setButtonsDefaultColor(it - 4, mBinding.tableLayout.getChildAt(1) as TableRow, except - 4)
            } else
                setButtonsDefaultColor(it, mBinding.tableLayout.getChildAt(0) as TableRow, except)
        }
    }
    private fun setButtonsDefaultColor(bound: Int, tableRow: TableRow, except: Int)
    {
        (0..<bound).filter { it != except  }.forEach { tableRow.getChildAt(it).apply {
            backgroundTintList = AppCompatResources.getColorStateList(
                requireContext(), R.color.default_button_color)
            foregroundTintList = AppCompatResources.getColorStateList(
                requireContext(), R.color.black)
        }
        }
    }

    private suspend fun sliderLeftEarOnValueChangedCallback(value: Float)
    {
        mutex.withLock {
            try {
                configuration.apply {
                    if (isConnected) {
                        if (!isDoubleDeviceConnection)
                            hearingAidMap[earSide]?.apply {
                                wirelessControl?.volume = value.toInt()
                                volume = value.toInt()
                            }
                        else
                            hearingAidMap[EarSide.Left]?.apply {
                                wirelessControl?.volume = value.toInt()
                                volume = value.toInt()
                            }
                    }
                }
            } catch (_: ArkException) {}
        }
    }
    private fun initActiveMemory(num: Int)
    {
        val button = with(mBinding) {
            when (num) {
                0 -> buttonOne
                1 -> buttonTwo
                2 -> buttonThree
                3 -> buttonFour
                4 -> buttonFive
                5 -> buttonSix
                6 -> buttonSeven
                else -> buttonEight
            }
        }
        setSelectedMemoryButtonView(button, num)
    }

    private suspend fun sliderRightEarOnValueChangedCallback(value: Float)
    {
        mutex.withLock {
            try {
                configuration.apply {
                    if (isConnected) {
                        if (!isDoubleDeviceConnection)
                            hearingAidMap[earSide]?.apply {
                                wirelessControl?.volume = value.toInt()
                                volume = value.toInt()
                            }
                        else
                            hearingAidMap[EarSide.Right]?.apply {
                                wirelessControl?.volume = value.toInt()
                                volume = value.toInt()
                            }
                    }
                }
            } catch (_: ArkException) {}
        }
    }
    fun memoryButtonClicked(buttonNumber: Int, button: Button)
    {
        try {
            with(configuration) {
                if (isDoubleDeviceConnection)
                    hearingAidMap.forEach {
                        it.value.wirelessControl?.currentMemory = ParameterSpace.value(buttonNumber)
                    }
                else
                    hearingAidMap[earSide]?.wirelessControl?.currentMemory = ParameterSpace.value(buttonNumber)
            }
            setSelectedMemoryButtonView(button, buttonNumber)
        } catch (_: ArkException) {}
    }
    private fun setSelectedMemoryButtonView(button: Button, num : Int)
    {
        lifecycleScope.launch(Dispatchers.Main) {

            button.backgroundTintList = AppCompatResources.getColorStateList(
                requireContext(),
                com.google.android.material.R.color.design_default_color_secondary_variant
            )
            button.foregroundTintList = AppCompatResources.getColorStateList(
                requireContext(), R.color.white)

            setButtonsDefaultColor(if (!configuration.isDoubleDeviceConnection) configuration.hearingAidMap[configuration.earSide]!!
            else configuration.hearingAidMap[EarSide.Left]!!, num)
        }
    }
    fun sliderDualOnValueChanged(value: Float)
    {
        with(configuration) {
            if (isDoubleDeviceConnection) {
                sliderLeftEarOnValueChanged(value)
                sliderRightEarOnValueChanged(value)
            }
            else if (earSide == EarSide.Left)
                sliderLeftEarOnValueChanged(value)
            else
                sliderRightEarOnValueChanged(value)
        }
    }
    fun sliderLeftEarOnValueChanged(value: Float)
    {
        lifecycleScope.launch(Dispatchers.Default) { sliderLeftEarOnValueChangedCallback(value) }
    }

    fun sliderRightEarOnValueChanged(value: Float)
    {
        lifecycleScope.launch(Dispatchers.Default) { sliderRightEarOnValueChangedCallback(value) }
    }
    fun switchOnCheckedChanged()
    {
        if (!configuration.isDualDeviceConfig) {

            lifecycleScope.launch(Dispatchers.Main) {
                mBinding.apply {
                    sliderLeft.startVolumeSliderFirstAnimation(requireContext(), this, R.anim.translate_left_to_right_left_slider)
                    sliderRight.startVolumeSliderFirstAnimation(requireContext(), this, R.anim.translate_right_to_left_right_slider)
                }
            }
            configuration.isDualDeviceConfig = true
        }
        else {
            lifecycleScope.launch(Dispatchers.Main) {

                mBinding.apply {
                    sliderLeft.startVolumeSliderSecondAnimation(requireContext(), this, R.anim.translate_right_to_left_left_slider)
                    sliderRight.startVolumeSliderSecondAnimation(requireContext(), this, R.anim.translate_left_to_right_right_slider)
                }
            }
            configuration.isDualDeviceConfig = false
        }
    }
}