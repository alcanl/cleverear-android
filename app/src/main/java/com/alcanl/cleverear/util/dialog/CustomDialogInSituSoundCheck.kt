package com.alcanl.cleverear.util.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutDialogInSituCheckBinding
import com.alcanl.cleverear.fitting.fragment.InSituFragment
import com.alcanl.cleverear.viewmodel.InSituSoundCheckDialogListenersViewModel

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

    override fun onStart()
    {
        mBinding.frameLayoutHeader.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.alpha_blink_anim_infinite))
    }
}