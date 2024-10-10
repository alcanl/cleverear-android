package com.alcanl.cleverear.fitting.fragment

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.collection.IntIntPair
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.alcanl.cleverear.MainActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.SelfFittingActivity
import com.alcanl.cleverear.databinding.FragmentInSituBinding
import com.alcanl.cleverear.di.interceptor.DecibelArrayInterceptor
import com.alcanl.cleverear.di.interceptor.FrequencyArrayInterceptor
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.fitting.event.InSituEvent
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_INVALIDATE_BINDING
import com.alcanl.cleverear.util.WHAT_INVALIDATE_CHART
import com.alcanl.cleverear.util.WHAT_SHOW_IN_SITU_DIALOG
import com.alcanl.cleverear.util.WHAT_SOUND_GENERATE_ERROR
import com.alcanl.cleverear.util.WHAT_UPDATE_CHART
import com.alcanl.cleverear.util.dialog.CustomDialogInSituSoundCheck
import com.alcanl.cleverear.util.extension.startAlphaAnimation
import com.alcanl.cleverear.viewmodel.InSituFragmentListenersViewModel
import com.ark.ArkException
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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.EventObject
import java.util.concurrent.ExecutorService
import javax.inject.Inject


@AndroidEntryPoint
class InSituFragment : Fragment(), IEventListener {
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
    private val mDialog: CustomDialogInSituSoundCheck by lazy {
        CustomDialogInSituSoundCheck(this)
    }
    private lateinit var mBinding : FragmentInSituBinding
    private lateinit var mHandler : InSituHandler
    private var mFrequencyIndex = 0
    private var mDecibelIndex = 2


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_situ, container, false)
        initialize()
        return mBinding.root
    }

    override fun onStart()
    {
        EventBus.getDefault().register(this)
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
    override fun onPause()
    {
        try {
            manufacturing.prepareDeviceForModeling(false)
        } catch (ex: ArkException) {
            Log.e("onPause", ex.message!!)
        }
        super.onPause()
    }

    override fun onDestroy() {
        try {
            manufacturing.prepareDeviceForModeling(false)
        } catch (ex: ArkException) {
            Log.e("onPause", ex.message!!)
        }
        super.onDestroy()
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

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is InSituEvent)
            saveButtonClicked()
    }

    private fun initialize()
    {
        mBinding.viewModel = InSituFragmentListenersViewModel(this)
        mBinding.config = fittingConfig
        mHandler = InSituHandler(this)
        threadPool.execute { initializeDeviceForInSituTest() }
    }
    private fun initializeDeviceForInSituTest() {
        try {
            with(fittingConfig) {
                manufacturing.initializeDevice(configuration.hearingAidMap[selectedEarSide]?.communicationAdaptor!!)
                manufacturing.prepareDeviceForModeling(true)
                manufacturing.setProduct(deviceInfoExtMap[selectedEarSide]?.product!!)
            }
        } catch (_: Throwable) { mHandler.sendEmptyMessage(WHAT_SOUND_GENERATE_ERROR) }
    }
    private fun initializeChart()
    {
        lineDataSet.apply {
            setColor(context?.getColor(if (fittingConfig.selectedEarSide == EarSide.Left) R.color.left_device_slider else R.color.right_device_slider)!!)
            setValueTextColor(context?.getColor(if (fittingConfig.selectedEarSide == EarSide.Left) R.color.left_device_slider else R.color.right_device_slider)!!)
            setDrawCircles(true)
            setDrawValues(true)
            circleRadius = 5F
            setDrawCircleHole(false)
            setCircleColor(context?.getColor(if (fittingConfig.selectedEarSide == EarSide.Left) R.color.left_device_slider else R.color.right_device_slider)!!)
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
    private fun saveMediaToStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context?.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    }

    private class InSituHandler(fragment: InSituFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {
            when(msg.what) {
                19 -> showDialog()
                20 -> updateChartPoint(msg.obj as IntIntPair)
                23 -> handleLibraryError()
                24 -> mWeakReference.get()?.initializeChart()
                25 -> mWeakReference.get()?.mBinding?.invalidateAll()
            }
        }
        private fun handleLibraryError()
        {
            mWeakReference.get()?.let {
                Toast.makeText(it.requireContext(), R.string.toast_unknown_problem_text,
                    Toast.LENGTH_LONG).show()
                Intent(it.requireContext(), MainActivity::class.java).apply {
                    it.activity?.startActivity(this)
                    it.activity?.finish()
                }
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
        try {
            manufacturing.prepareDeviceForModeling(false)
        } catch (ex: ArkException) {
            Log.e("earSideButtonClicked", ex.message!!)
        }

        fittingConfig.selectedEarSide = earSide

        threadPool.apply {
            execute {
                submit { initializeDeviceForInSituTest() }.get()
                mHandler.apply {
                    sendEmptyMessage(WHAT_INVALIDATE_BINDING)
                    sendEmptyMessage(WHAT_INVALIDATE_CHART)
                }
            }
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    fun playSoundButtonClicked()
    {
        (activity as SelfFittingActivity).isInSituInProgress = true
        mHandler.sendEmptyMessage(WHAT_SHOW_IN_SITU_DIALOG)
        GlobalScope.launch {
            try {
                async {
                    try {
                        manufacturing.generateTone(
                            frequencyArray[mFrequencyIndex],
                            decibelArray[mDecibelIndex] - 120
                        )
                        delay(1500)
                    } catch (_: Throwable) {
                        mHandler.sendEmptyMessage(WHAT_SOUND_GENERATE_ERROR)
                    }
                }.await()
                manufacturing.stopTone()
            } catch (_: Throwable) { mHandler.sendEmptyMessage(WHAT_SOUND_GENERATE_ERROR) }
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
    @OptIn(DelicateCoroutinesApi::class)
    fun saveButtonClicked()
    {
        (activity as SelfFittingActivity).isInSituInProgress = false

        GlobalScope.launch {
            saveMediaToStorage(mBinding.lineChart.chartBitmap)
        }

        Toast.makeText(requireContext(), R.string.toast_test_result_saved_text, Toast.LENGTH_LONG).show()
    }
    fun autoFitButtonClicked()
    {
        TODO("Not Implemented Yet!")
    }
    fun dialogPositiveButtonClicked()
    {
        try {
            if (mFrequencyIndex == frequencyArray.size - 1) {
                manufacturing.prepareDeviceForModeling(false)
                mFrequencyIndex = 0
                mDecibelIndex = 2
                mDialog.dismiss()
                return
            }
            inSituResultMap[frequencyArray[mFrequencyIndex++].toFloat()] =
                decibelArray[mDecibelIndex].toFloat()
            mDecibelIndex = 2
            changeText(mBinding.textViewFrequency, frequencyArray[mFrequencyIndex])
            changeText(mBinding.textViewDecibel, decibelArray[mDecibelIndex])

            mDialog.apply {
                updateDialogText(frequencyArray[mFrequencyIndex], decibelArray[mDecibelIndex])
                dismiss()
            }

            mHandler.postDelayed(this::playSoundButtonClicked, 1000)

        } catch (ex: Throwable) {
            Log.e("dialogPositiveButtonClicked", ex.message!!)
        }

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
    fun dialogEndTestButtonClicked()
    {
        try {
            (activity as SelfFittingActivity).isInSituInProgress = false
            manufacturing.prepareDeviceForModeling(false)
            mFrequencyIndex = 0
            mDecibelIndex = 2
            mDialog.dismiss()
            mHandler.sendEmptyMessage(WHAT_INVALIDATE_CHART)

        } catch (ex: Throwable) {
            Log.e(this.javaClass.name, "End Test Error")
        }
    }
}