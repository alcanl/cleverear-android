package com.alcanl.cleverear.util.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Menu
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutDialogInSituCheckBinding
import com.alcanl.cleverear.fitting.fragment.InSituFragment
import com.alcanl.cleverear.viewmodel.InSituSoundCheckDialogListenersViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class CustomDialogInSituSoundCheck (fragment: InSituFragment) : Dialog(fragment.requireContext(), R.style.DialogStyle) {
    private var mBinding: LayoutDialogInSituCheckBinding
    private lateinit var mDeferred : Deferred<*>
    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        mBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_dialog_in_situ_check, null, false)
        mBinding.viewModel = InSituSoundCheckDialogListenersViewModel(fragment)
        setContentView(mBinding.root)
    }
    @SuppressLint("DefaultLocale")
    fun updateDialogText(frequency: Int, decibel: Int)
    {
        mBinding.textViewSoundProperties.text = String.format(context.getString(R.string.dialog_textview_check_text), frequency, decibel)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStart() {
        super.onStart()

        mDeferred = GlobalScope.async {
            while (true) {
                mBinding.audioWaveView.progress++
                runBlocking {
                    delay(150)
                }
                if (mBinding.audioWaveView.progress >= 100F)
                    mBinding.audioWaveView.progress = 0F
            }
        }
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        super.onPanelClosed(featureId, menu)
        if (mDeferred.isActive)
            mDeferred.cancel()

    }
}