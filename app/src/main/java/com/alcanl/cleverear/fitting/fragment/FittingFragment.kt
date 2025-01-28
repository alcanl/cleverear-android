package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.R
import com.alcanl.cleverear.SelfFittingActivity
import com.alcanl.cleverear.databinding.FragmentFittingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.fitting.burnChangesToAllMemories
import com.alcanl.cleverear.fitting.burnChangesToMemory
import com.alcanl.cleverear.fitting.disableEqualizer
import com.alcanl.cleverear.fitting.disableFeedBackCanceler
import com.alcanl.cleverear.fitting.disableMemoryRetention
import com.alcanl.cleverear.fitting.disableVolumeRetention
import com.alcanl.cleverear.fitting.enableEqualizer
import com.alcanl.cleverear.fitting.enableFeedBackCanceler
import com.alcanl.cleverear.fitting.enableMemoryRetention
import com.alcanl.cleverear.fitting.enableVolumeRetention
import com.alcanl.cleverear.fitting.event.FittingEvent
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.fitting.setEqualizerBassValue
import com.alcanl.cleverear.fitting.setEqualizerMidValue
import com.alcanl.cleverear.fitting.setEqualizerTrebleValue
import com.alcanl.cleverear.fitting.setWideBandGain
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_DISABLE_EQUALIZER_SLIDERS
import com.alcanl.cleverear.util.WHAT_ENABLE_EQUALIZER_SLIDERS
import com.alcanl.cleverear.util.WHAT_HIDE_LOADING_FRAME
import com.alcanl.cleverear.util.WHAT_SHOW_LOADING_FRAME
import com.alcanl.cleverear.util.dialog.CustomDialogProgramPicker
import com.alcanl.cleverear.util.extension.handleFittingCheckedChanged
import com.alcanl.cleverear.util.extension.setLayoutDisable
import com.alcanl.cleverear.util.extension.setLayoutEnable
import com.alcanl.cleverear.viewmodel.FittingFragmentListenersViewModel
import com.ark.ParameterSpace
import com.skydoves.powerspinner.IconSpinnerAdapter
import com.skydoves.powerspinner.IconSpinnerItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.EventObject
import javax.inject.Inject

@AndroidEntryPoint
class FittingFragment : Fragment(), IEventListener {

    @Inject
    lateinit var fittingConfig: FittingConfigurationModule

    @Inject
    lateinit var configuration: ConfigurationModule

    @Inject
    lateinit var mutex: Mutex

    private lateinit var mSelectedMemory : ParameterSpace

    private lateinit var mBinding: FragmentFittingBinding

    private lateinit var mHandler: FittingHandler

    private var mSelectedPickerPosition : Int = 0

    private val mDialogProgramPicker: CustomDialogProgramPicker by lazy {
        CustomDialogProgramPicker(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_fitting, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    override fun onStart()
    {
        EventBus.getDefault().register(this)
        initialize()
        super.onStart()
    }

    override fun onResume()
    {
        (activity as SelfFittingActivity).binding.frameLayoutLoading.visibility = View.GONE
        super.onResume()
    }

    override fun onStop()
    {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is FittingEvent)
            burnButtonsClicked(mBinding.buttonBurnAll)
    }

    private fun initialize()
    {
        mBinding.viewModel = FittingFragmentListenersViewModel(this)
        mHandler = FittingHandler(this)
        initializeSelectedMemory()
        initializeEqualizer()
        initializeFeedbackCancelerSpinnerAdapter()
    }

    private fun initializeSelectedMemory()
    {
        mSelectedMemory = configuration.hearingAidMap[fittingConfig.selectedEarSide]?.currentMemory!!
    }

    private fun initializeEqualizer()
    {
        mBinding.isEqualizerEnabled = with(fittingConfig) {
            deviceInfoExtMap[selectedEarSide]?.isEqualizerEnabled()!!
        }
    }

    private fun initializeFeedbackCancelerSpinnerAdapter()
    {
        try {
            mBinding.apply {
                spinnerFeedbackCanceler.setSpinnerAdapter(IconSpinnerAdapter(spinnerFeedbackCanceler))
                val itemArray = resources.getStringArray(R.array.feedbackCancelerValues)
                val iconAdapterList = ArrayList<IconSpinnerItem>(itemArray.size)

                itemArray.forEach {
                    iconAdapterList.add(IconSpinnerItem(text = it))
                }

                spinnerFeedbackCanceler.setItems(iconAdapterList)
                spinnerFeedbackCanceler.lifecycleOwner = this@FittingFragment

            }
        } catch (ex: Throwable) {
            Log.e(FittingFragment::class.simpleName, ex.message!!)
        }
    }

    private suspend fun wideBandSliderValueChangedCallback(value: Int)
    {
        mutex.withLock {
            try {
                fittingConfig.apply {
                    deviceInfoExtMap[selectedEarSide]?.let {
                        it.product?.setWideBandGain(value)
                    }
                }
            } catch (ex: Throwable) {
                Log.e(FittingFragment::class.simpleName, ex.message!!)
            }
        }
    }

    private fun burnParametersToSelectedMemory()
    {
        try {
            mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
            fittingConfig.apply {
                deviceInfoExtMap[selectedEarSide]?.product?.burnChangesToMemory(mSelectedMemory)
            }
            mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)

        } catch (ex: Throwable) {
            Log.e(FittingFragment::class.simpleName, ex.message!!)
        }
    }

    private fun burnParametersToAllMemories()
    {
        try {
            mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
            with(fittingConfig) {
                deviceInfoExtMap[selectedEarSide]?.product?.burnChangesToAllMemories(
                    configuration.hearingAidMap[selectedEarSide]!!
                )
            }
            mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)

        } catch (ex: Throwable) {
            Log.e(FittingFragment::class.simpleName, ex.message!!)
        }
    }

    private suspend fun invalidateBindingsCallback()
    {
        mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
        delay(1500)
        lifecycleScope.launch(Dispatchers.Main) { mBinding.invalidateAll() }
        delay(2000)
        mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)
    }

    private class FittingHandler(fragment: FittingFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {
            when (msg.what) {
                27 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        constraintLayoutLoading.visibility = View.VISIBLE
                        constraintLayoutMain.setLayoutDisable()
                    }
                }
                28 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        constraintLayoutLoading.visibility = View.GONE
                        constraintLayoutMain.setLayoutEnable()
                    }
                }
                29 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        sliderMid.isEnabled = true
                        sliderBass.isEnabled = true
                        sliderTreble.isEnabled = true
                    }
                }
                30 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        sliderMid.isEnabled = false
                        sliderBass.isEnabled = false
                        sliderTreble.isEnabled = false
                    }
                }
            }
        }
    }

    fun equalizerBassSliderValueChanged(value: Float)
    {
        suspend fun handleSynchronized()
        {
            mutex.withLock {
                fittingConfig.apply {
                    deviceInfoExtMap[selectedEarSide]?.let {
                        it.product?.setEqualizerBassValue(it.hearingAidDTO!!, it.equalizerValuesDTO?.valueList!!, value.toInt())
                    }
                }
            }
        }
        (activity as SelfFittingActivity).isFittingChangesMade = true
        lifecycleScope.launch(Dispatchers.IO) { handleSynchronized() }
    }

    fun equalizerMidSliderValueChanged(value: Float)
    {
        suspend fun handleSynchronized()
        {
            mutex.withLock {
                fittingConfig.apply {
                    deviceInfoExtMap[selectedEarSide]?.let {
                        it.product?.setEqualizerMidValue(it.hearingAidDTO!!, it.equalizerValuesDTO?.valueList!!, value.toInt())
                    }
                }
            }
        }
        (activity as SelfFittingActivity).isFittingChangesMade = true
        lifecycleScope.launch(Dispatchers.IO) { handleSynchronized() }
    }

    fun equalizerTrebleSliderValueChanged(value: Float)
    {
        suspend fun handleSynchronized()
        {
            mutex.withLock {
                fittingConfig.apply {
                    deviceInfoExtMap[selectedEarSide]?.let {
                        it.product?.setEqualizerTrebleValue(it.hearingAidDTO!!, it.equalizerValuesDTO?.valueList!!, value.toInt())
                    }
                }
            }
        }
        (activity as SelfFittingActivity).isFittingChangesMade = true
        lifecycleScope.launch(Dispatchers.IO) { handleSynchronized() }
    }

    fun wideBandSliderValueChanged(value: Float)
    {
        (activity as SelfFittingActivity).isFittingChangesMade = true
        lifecycleScope.launch(Dispatchers.IO) { wideBandSliderValueChangedCallback(value.toInt()) }
    }

    fun equalizerCheckedChanged()
    {
        (activity as SelfFittingActivity).isFittingChangesMade = true
        lifecycleScope.launch(Dispatchers.IO) {
            fittingConfig.apply {
                deviceInfoExtMap[selectedEarSide]?.let {
                    mBinding.checkBoxEqualizer.handleFittingCheckedChanged(
                        { mHandler.sendEmptyMessage(WHAT_DISABLE_EQUALIZER_SLIDERS)
                            it.product?.disableEqualizer(mSelectedMemory)  },
                        { mHandler.sendEmptyMessage(WHAT_ENABLE_EQUALIZER_SLIDERS)
                            it.product?.enableEqualizer(mSelectedMemory) })
                }
            }
        }
    }

    fun feedbackCancelerCheckedChanged()
    {
        (activity as SelfFittingActivity).isFittingChangesMade = true
        mBinding.spinnerFeedbackCanceler.isEnabled = mBinding.checkBoxFeedbackCanceler.isChecked

        lifecycleScope.launch(Dispatchers.IO) {
            fittingConfig.apply {
                mBinding.checkBoxFeedbackCanceler.handleFittingCheckedChanged(
                    { deviceInfoExtMap[selectedEarSide]?.product?.disableFeedBackCanceler() },
                    { deviceInfoExtMap[selectedEarSide]?.product?.enableFeedBackCanceler() })
            }
        }
    }

    fun spinnerFeedbackCancelerItemSelected(pos: Int)
    {
        TODO("Not implemented Yet")
    }

    fun burnButtonsClicked(button: AppCompatButton)
    {
        lifecycleScope.launch(Dispatchers.IO) {
            async {
                if (button.id == mBinding.buttonBurn.id)
                    burnParametersToSelectedMemory()
                else
                    burnParametersToAllMemories()
            }.await()

            (activity as SelfFittingActivity).isFittingChangesMade = false
        }
    }

    fun earSideButtonClicked(earSide: EarSide)
    {
        fittingConfig.selectedEarSide = earSide
        lifecycleScope.launch(Dispatchers.Default) { invalidateBindingsCallback() }

    }

    fun numberPickerOnValueChanged(pos: Int)
    {
        mSelectedPickerPosition = pos
        mBinding.spinnerMemory.hint = resources.getStringArray(R.array.programs)[pos]
    }

    fun memoryRetentionCheckedChanged()
    {
        (activity as SelfFittingActivity).isFittingChangesMade = true
        lifecycleScope.launch(Dispatchers.IO) {
            fittingConfig.apply {
                mBinding.checkBoxMemoryRetention.handleFittingCheckedChanged(
                    { deviceInfoExtMap[selectedEarSide]?.product?.disableMemoryRetention() },
                    { deviceInfoExtMap[selectedEarSide]?.product?.enableMemoryRetention() })
            }
        }
    }

    fun volumeRetentionCheckedChanged()
    {
        (activity as SelfFittingActivity).isFittingChangesMade = true
        lifecycleScope.launch(Dispatchers.IO) {
            fittingConfig.apply {
                mBinding.checkBoxVolumeRetention.handleFittingCheckedChanged(
                    { deviceInfoExtMap[selectedEarSide]?.product?.disableVolumeRetention() },
                    { deviceInfoExtMap[selectedEarSide]?.product?.enableVolumeRetention() })
            }
        }
    }

    fun memorySpinnerClicked()
    {
        mDialogProgramPicker.show()
    }

    fun dialogSelectProgramClicked()
    {
        mDialogProgramPicker.dismiss()
        mSelectedMemory = ParameterSpace.value(mSelectedPickerPosition)
        configuration.hearingAidMap[fittingConfig.selectedEarSide]
            ?.wirelessControl?.currentMemory = ParameterSpace.value(mSelectedPickerPosition)

        lifecycleScope.launch(Dispatchers.Default) { invalidateBindingsCallback() }
    }
}