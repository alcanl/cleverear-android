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
import com.alcanl.cleverear.R
import com.alcanl.cleverear.RemoteFittingActivity
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
import com.alcanl.cleverear.fitting.event.listener.FragmentListener
import com.alcanl.cleverear.fitting.setEqualizerBassValue
import com.alcanl.cleverear.fitting.setEqualizerMidValue
import com.alcanl.cleverear.fitting.setEqualizerTrebleValue
import com.alcanl.cleverear.fitting.setWideBandGain
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_DISABLE_EQUALIZER_SLIDERS
import com.alcanl.cleverear.util.WHAT_ENABLE_EQUALIZER_SLIDERS
import com.alcanl.cleverear.util.WHAT_HIDE_LOADING_FRAME
import com.alcanl.cleverear.util.WHAT_SHOW_LOADING_FRAME
import com.alcanl.cleverear.util.extension.handleFittingCheckedChanged
import com.alcanl.cleverear.util.extension.setLayoutDisable
import com.alcanl.cleverear.util.extension.setLayoutEnable
import com.alcanl.cleverear.viewmodel.FittingFragmentListenersViewModel
import com.ark.ParameterSpace
import com.skydoves.powerspinner.IconSpinnerAdapter
import com.skydoves.powerspinner.IconSpinnerItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject


@AndroidEntryPoint
class FittingFragment : Fragment(), FragmentListener {
    @Inject
    lateinit var fittingConfig: FittingConfigurationModule
    @Inject
    lateinit var threadPool : ExecutorService
    @Inject
    lateinit var configuration: ConfigurationModule

    private lateinit var mSelectedMemory : ParameterSpace
    private lateinit var mBinding: FragmentFittingBinding
    private lateinit var mHandler: FittingHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_fitting, container, false)
        return mBinding.root
    }
    override fun onStart()
    {
        EventBus.getDefault().register(this)
        initialize()
        super.onStart()
    }
    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: FittingEvent)
    {
        burnParametersToAllMemories()
    }

    private fun initialize()
    {
        mBinding.viewModel = FittingFragmentListenersViewModel(this)
        mHandler = FittingHandler(this)
        initializeSpinner()
        initializeSelectedMemory()
        initializeEqualizer()
        initializeFeedbackCancelerSpinnerAdapter()
    }
    private fun initializeSelectedMemory()
    {
        mSelectedMemory = ParameterSpace.value(mBinding.spinnerMemory.selectedIndex)
    }
    private fun initializeEqualizer()
    {
        mBinding.isEqualizerEnabled = with(fittingConfig) {
            deviceInfoExtMap[selectedEarSide]?.isEqualizerEnabled()!!
        }
    }
    private fun initializeMemorySpinnerAdapter(memoryCount: Int)
    {
        try {
            mBinding.apply {
                spinnerMemory.setSpinnerAdapter(IconSpinnerAdapter(spinnerMemory))
                val iconAdapterList = ArrayList<IconSpinnerItem>(memoryCount)
                val itemArray = resources.getStringArray(R.array.programs)

                (0..< memoryCount).forEach {
                    iconAdapterList.add(IconSpinnerItem(text = itemArray[it]))
                }
                spinnerMemory.setItems(iconAdapterList)

                spinnerMemory.selectItemByIndex(
                    with(fittingConfig) {
                        deviceInfoExtMap[selectedEarSide]?.product?.currentMemory?.value() ?: 1
                    })

                spinnerMemory.lifecycleOwner = this@FittingFragment

            }
        } catch (ex: Throwable) {
            Log.e("initializeSpinnerAdapter", ex.message!!)
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
            Log.e("initializeSpinnerAdapter", ex.message!!)
        }
    }
    private fun initializeSpinner()
    {
        val memoryCount = with(configuration) {
            with(fittingConfig) {
                hearingAidMap[selectedEarSide]?.numberOfMemories ?: 8
            }
        }

        initializeMemorySpinnerAdapter(memoryCount)
    }

    @Synchronized
    private fun wideBandSliderValueChangedCallback(value: Int)
    {
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        try {
            fittingConfig.apply {
                deviceInfoExtMap[selectedEarSide]?.let {
                    it.product?.setWideBandGain(value)
                }
            }
        } catch (ex: Throwable) {
            Log.e("wideBandSliderValueChanged", ex.message!!)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun burnParametersToSelectedMemory()
    {
        GlobalScope.launch {
            try {
                mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
                fittingConfig.apply {
                    deviceInfoExtMap[selectedEarSide]?.product?.burnChangesToMemory(mSelectedMemory)
                }
                mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)
            } catch (ex: Throwable) {
                Log.e("burnParametersToSelectedMemory", ex.message!!)
            }
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun burnParametersToAllMemories()
    {
        GlobalScope.launch {
            try {
                mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
                with(fittingConfig) {
                    deviceInfoExtMap[selectedEarSide]?.product?.burnChangesToAllMemories(
                        configuration.hearingAidMap[selectedEarSide]!!
                    )
                }
                mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)
            } catch (ex: Throwable) {
                Log.e("burnParametersToAllMemories", ex.message!!)
            }
        }
    }

    private fun invalidateBindingsCallback()
    {
        mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
        mBinding.invalidateAll()
        runBlocking {
            delay(1500)
        }
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
        @Synchronized
        fun handleSynchronized()
        {
            fittingConfig.apply {
                deviceInfoExtMap[selectedEarSide]?.let {
                    it.product?.setEqualizerBassValue(it.hearingAidDTO!!, value.toInt())
                }
            }
        }
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        threadPool.execute { handleSynchronized() }
    }
    fun equalizerMidSliderValueChanged(value: Float)
    {
        @Synchronized
        fun handleSynchronized()
        {
            fittingConfig.apply {
                deviceInfoExtMap[selectedEarSide]?.let {
                    it.product?.setEqualizerMidValue(it.hearingAidDTO!!, value.toInt())
                }
            }
        }
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        threadPool.execute { handleSynchronized() }
    }
    fun equalizerTrebleSliderValueChanged(value: Float)
    {
        @Synchronized
        fun handleSynchronized()
        {
            fittingConfig.apply {
                deviceInfoExtMap[selectedEarSide]?.let {
                    it.product?.setEqualizerTrebleValue(it.hearingAidDTO!!, value.toInt())
                }
            }
        }
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        threadPool.execute { handleSynchronized() }
    }
    fun wideBandSliderValueChanged(value: Float)
    {
        threadPool.execute { wideBandSliderValueChangedCallback(value.toInt()) }
    }
    fun equalizerCheckedChanged()
    {
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        threadPool.execute {
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
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        mBinding.spinnerFeedbackCanceler.isEnabled = mBinding.checkBoxFeedbackCanceler.isChecked

        threadPool.execute {
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
        (activity as RemoteFittingActivity).isFittingChangesMade = false

        if (button.id == mBinding.buttonBurn.id)
            burnParametersToSelectedMemory()
        else
            burnParametersToAllMemories()
    }
    fun earSideButtonClicked(earSide: EarSide)
    {
        fittingConfig.selectedEarSide = earSide
        threadPool.execute(this::invalidateBindingsCallback)

    }
    fun spinnerItemSelected(pos: Int)
    {
        mSelectedMemory = ParameterSpace.value(pos)
        configuration.hearingAidMap[fittingConfig.selectedEarSide]
            ?.wirelessControl?.currentMemory = ParameterSpace.value(pos)

        threadPool.execute(this::invalidateBindingsCallback)

    }
    fun memoryRetentionCheckedChanged()
    {
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        threadPool.execute {
            fittingConfig.apply {
                mBinding.checkBoxMemoryRetention.handleFittingCheckedChanged(
                    { deviceInfoExtMap[selectedEarSide]?.product?.disableMemoryRetention() },
                    { deviceInfoExtMap[selectedEarSide]?.product?.enableMemoryRetention() })

            }
        }
    }
    fun volumeRetentionCheckedChanged()
    {
        (activity as RemoteFittingActivity).isFittingChangesMade = true
        threadPool.execute {
            fittingConfig.apply {
                mBinding.checkBoxVolumeRetention.handleFittingCheckedChanged(
                    { deviceInfoExtMap[selectedEarSide]?.product?.disableVolumeRetention() },
                    { deviceInfoExtMap[selectedEarSide]?.product?.enableVolumeRetention() })
            }
        }
    }
}