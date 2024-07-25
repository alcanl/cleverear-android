package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.SelfFittingActivity
import com.alcanl.cleverear.databinding.LayoutAlertDialogInSituInProgressBinding
import com.alcanl.cleverear.viewmodel.InSituInProgressDialogListenersViewModel

class CustomAlertDialogInSituInProgress(activity: SelfFittingActivity): Dialog(activity, R.style.DialogStyle) {
    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        val binding: LayoutAlertDialogInSituInProgressBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_alert_dialog_in_situ_in_progress, null, false)
        binding.viewModel = InSituInProgressDialogListenersViewModel(activity)
        setContentView(binding.root)
    }
}