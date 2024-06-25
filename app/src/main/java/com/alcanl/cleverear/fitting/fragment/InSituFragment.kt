package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.collection.IntIntPair
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentInSituBinding
import com.alcanl.cleverear.di.interceptor.DecibelArrayInterceptor
import com.alcanl.cleverear.di.interceptor.FrequencyArrayInterceptor
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_SHOW_IN_SITU_DIALOG
import com.alcanl.cleverear.util.WHAT_UPDATE_CHART
import com.alcanl.cleverear.util.dialog.CustomDialogInSituSoundCheck
import com.alcanl.cleverear.util.extension.startAlphaAnimation
import com.alcanl.cleverear.viewmodel.InSituFragmentListenersViewModel
import com.ark.Manufacturing
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject


@AndroidEntryPoint
class InSituFragment : Fragment() {
    @Inject
    lateinit var fittingConfig : FittingConfigurationModule
    @Inject
    lateinit var configuration : ConfigurationModule
    @Inject
    lateinit var manufacturing: Manufacturing
    @Inject
    lateinit var threadPool : ExecutorService
    @Inject
    @FrequencyArrayInterceptor
    lateinit var frequencyArray: Array<Int>
    @Inject
    @DecibelArrayInterceptor
    lateinit var decibelArray: Array<Int>
    @Inject
    lateinit var lineDataSet: LineDataSet
    @Inject
    lateinit var dataList: ArrayList<Entry>
    @Inject
    lateinit var lineData: LineData
    @Inject
    lateinit var inSituResultMap: MutableMap<Float, Float>

    private var mFrequencyIndex = 0
    private var mDecibelIndex = 2
    private lateinit var mDialog: CustomDialogInSituSoundCheck
    private lateinit var mBinding : FragmentInSituBinding
    private lateinit var mHandler : InSituHandler


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_situ, container, false)
        initialize()
        return mBinding.root
    }
    override fun onDestroy()
    {
        super.onDestroy()
        manufacturing.prepareDeviceForModeling(false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            textViewFrequency.text = String.format(
                getString(R.string.textview_frequency_hertz_text), frequencyArray[mFrequencyIndex])
            textViewDecibel.text = String.format(
                getString(R.string.textview_decibel_text), decibelArray[mDecibelIndex])
        }
        initializeChart()
    }
    private fun initialize()
    {
        mBinding.viewModel = InSituFragmentListenersViewModel(this)
        mBinding.config = fittingConfig
        mHandler = InSituHandler(this)
        mDialog = CustomDialogInSituSoundCheck(this)
        threadPool.execute { initializeDeviceForInSituTest() }
    }
    private fun initializeDeviceForInSituTest() {
        with(fittingConfig) {
            if (configuration.isDoubleDeviceConnection)
                manufacturing.initializeDevice(configuration.doubleDeviceMap[selectedEarSide]?.communicationAdaptor!!)
            else
                manufacturing.initializeDevice(configuration.hearingAid.communicationAdaptor!!)

            manufacturing.prepareDeviceForModeling(true)
            manufacturing.setProduct(deviceInfoExtMap[selectedEarSide]?.product!!)
        }
    }
    private fun initializeChart()
    {
        lineDataSet.apply {
            setColor(context?.getColor(if (fittingConfig.isLeftButtonSelected) R.color.left_device_slider else R.color.right_device_slider)!!)
            setValueTextColor(context?.getColor(if (fittingConfig.isLeftButtonSelected) R.color.left_device_slider else R.color.right_device_slider)!!)
            setDrawCircles(true)
            setDrawValues(true)
            circleRadius = 5F
            setDrawCircleHole(false)
            setCircleColor(context?.getColor(if (fittingConfig.isLeftButtonSelected) R.color.left_device_slider else R.color.right_device_slider)!!)
            mode= LineDataSet.Mode.LINEAR
        }

        mBinding.lineChart.apply {
            val desc = Description()
            desc.text = ""
            description = desc
            setData(this@InSituFragment.lineData)
            with(xAxis) {
                position = XAxis.XAxisPosition.BOTTOM
                setLabelCount(11, true)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return frequencyArray[value.toInt()].toString()
                    }
                }
            }
            axisRight.isEnabled = false
            axisLeft.axisMaximum = 100F
            axisLeft.axisMinimum = 0F
            invalidate()
        }
    }
    private fun changeText(textView: TextView, value: Int)
    {
        textView.startAlphaAnimation(requireContext(),
            String.format(getString(
                if (textView == mBinding.textViewFrequency) R.string.textview_frequency_hertz_text
                else R.string.textview_decibel_text), value))
    }

    private class InSituHandler(fragment: InSituFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {
            when(msg.what) {
                19 -> showDialog()
                20 -> updateChartPoint(msg.obj as IntIntPair)
            }
        }
        private fun showDialog()
        {
            mWeakReference.get()?.let {
                it.mDialog.apply {
                    updateDialogText(it.frequencyArray[it.mFrequencyIndex], it.decibelArray[it.mDecibelIndex])
                    show()
                }
            }
        }
        private fun updateChartPoint(pair: IntIntPair)
        {
            val (index, heardDecibel) = pair
            mWeakReference.get()?.let {
                it.dataList[index] = Entry(index.toFloat(), heardDecibel.toFloat())
                it.lineDataSet.values = it.dataList
                it.lineDataSet.notifyDataSetChanged()
                it.lineData.notifyDataChanged()
                it.mBinding.lineChart.apply {
                    notifyDataSetChanged()
                    invalidate()
                }
            }
        }
    }

    fun earSideButtonClicked(earSide: EarSide)
    {
        manufacturing.prepareDeviceForModeling(false)

        fittingConfig.apply {
            selectedEarSide = earSide
            isRightButtonSelected = earSide == EarSide.Right
            isLeftButtonSelected = earSide == EarSide.Left
        }

        threadPool.execute { initializeDeviceForInSituTest() }
        mBinding.invalidateAll()
    }
    @OptIn(DelicateCoroutinesApi::class)
    fun playSoundButtonClicked()
    {
        mHandler.sendEmptyMessage(WHAT_SHOW_IN_SITU_DIALOG)
        GlobalScope.launch {
            async {
                manufacturing.generateTone(frequencyArray[mFrequencyIndex], decibelArray[mDecibelIndex] - 120)
                delay(1500)
            }.await()
            manufacturing.stopTone()
        }
    }
    fun decibelButtonsClicked(value: Int)
    {
        if (mDecibelIndex <= 0 && value < 0)
            return

        if (mDecibelIndex >= 18 && value > 0)
            return

        mBinding.apply {
            if (value > 0)
                changeText(textViewDecibel, decibelArray[++mDecibelIndex])
            else
                changeText(textViewDecibel, decibelArray[--mDecibelIndex])
        }
    }
    fun frequencyButtonsClicked(imageButton: ImageButton)
    {
        if (imageButton == mBinding.imageButtonIncreaseFrequency) {
            if (mFrequencyIndex >= frequencyArray.size - 1)
                return

            changeText(mBinding.textViewFrequency ,frequencyArray[++mFrequencyIndex])
        }
        else {
            if (mFrequencyIndex <= 0)
                return

            changeText(mBinding.textViewFrequency ,frequencyArray[--mFrequencyIndex])
        }
    }
    fun saveButtonClicked()
    {
        mBinding.lineChart.saveToGallery("TEST")
    }
    fun autoFitButtonClicked()
    {
        TODO("Not Implemented Yet!")
    }
    fun dialogPositiveButtonClicked()
    {
        if (mFrequencyIndex == frequencyArray.size - 1) {
            mFrequencyIndex = 0
            mDecibelIndex = 2
            mDialog.dismiss()
            return
        }
        inSituResultMap[frequencyArray[mFrequencyIndex++].toFloat()] = decibelArray[mDecibelIndex].toFloat()
        mDecibelIndex = 2
        changeText(mBinding.textViewFrequency, frequencyArray[mFrequencyIndex])
        changeText(mBinding.textViewDecibel, decibelArray[mDecibelIndex])

        mDialog.apply {
            updateDialogText(frequencyArray[mFrequencyIndex], decibelArray[mDecibelIndex])
            dismiss()
        }

        mHandler.postDelayed(this::playSoundButtonClicked, 1000)

    }
    fun dialogNegativeButtonClicked()
    {
        if (mDecibelIndex == decibelArray.size - 1) {
            dialogPositiveButtonClicked()
            mDialog.dismiss()
            return
        }

        mHandler.apply {
            sendMessage(obtainMessage(WHAT_UPDATE_CHART, IntIntPair(mFrequencyIndex, decibelArray[++mDecibelIndex])))
            postDelayed(this@InSituFragment::playSoundButtonClicked, 1000)
        }

        changeText(mBinding.textViewDecibel, decibelArray[mDecibelIndex])
        mDialog.apply {
            updateDialogText(frequencyArray[mFrequencyIndex], decibelArray[mDecibelIndex])
            dismiss()
        }

    }
}