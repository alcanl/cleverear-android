package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.SelfFittingActivity
import com.alcanl.cleverear.databinding.LayoutAlertDialogFittingNotSavedBinding
import com.alcanl.cleverear.viewmodel.FittingUnsavedDialogListenersViewModel

class CustomAlertDialogFittingUnsaved(activity: SelfFittingActivity): Dialog(activity, R.style.DialogStyle) {
    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        val binding: LayoutAlertDialogFittingNotSavedBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_alert_dialog_fitting_not_saved, null, false)
        binding.viewModel = FittingUnsavedDialogListenersViewModel(activity)
        setContentView(binding.root)
    }
}