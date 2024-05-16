package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.SplashActivity
import com.alcanl.cleverear.databinding.LayoutAlertDialogConnectedBeforeBinding
import com.alcanl.cleverear.viewmodel.ConnectedBeforeDialogListenersViewModel

class CustomAlertDialogConnectedBefore(activity: SplashActivity): Dialog(activity, R.style.DialogStyle) {
    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        val binding: LayoutAlertDialogConnectedBeforeBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_alert_dialog_connected_before, null, false)
        binding.viewModel = ConnectedBeforeDialogListenersViewModel(activity)
        setContentView(binding.root)
    }
}