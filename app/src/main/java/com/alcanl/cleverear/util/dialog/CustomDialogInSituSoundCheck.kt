package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutDialogInSituCheckBinding
import com.alcanl.cleverear.fitting.fragment.InSituFragment
import com.alcanl.cleverear.viewmodel.InSituSoundCheckDialogListenersViewModel

class CustomDialogInSituSoundCheck (fragment: InSituFragment) : Dialog(fragment.requireContext(), R.style.DialogStyle) {
    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        val binding: LayoutDialogInSituCheckBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_dialog_in_situ_check, null, false)
        binding.viewModel = InSituSoundCheckDialogListenersViewModel(fragment)
        setContentView(binding.root)
    }
}