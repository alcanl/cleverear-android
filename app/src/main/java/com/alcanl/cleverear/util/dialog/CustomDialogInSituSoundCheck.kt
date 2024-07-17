package com.alcanl.cleverear.util.dialog

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.view.animation.LinearInterpolator
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutDialogInSituCheckBinding
import com.alcanl.cleverear.fitting.fragment.InSituFragment
import com.alcanl.cleverear.viewmodel.InSituSoundCheckDialogListenersViewModel
import kotlin.random.Random


class CustomDialogInSituSoundCheck (fragment: InSituFragment) : Dialog(fragment.requireContext(), R.style.DialogStyle) {

    private var mBinding: LayoutDialogInSituCheckBinding

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

    override fun onStart() {
        super.onStart()
        val progressAnim = ObjectAnimator.ofFloat(mBinding.audioWaveView, "progress", 0F, 100F).apply {
            interpolator = LinearInterpolator()
            duration = 2000
        }
        mBinding.audioWaveView.setRawData(Random.Default.nextBytes(75)) { progressAnim.start() }
    }

}