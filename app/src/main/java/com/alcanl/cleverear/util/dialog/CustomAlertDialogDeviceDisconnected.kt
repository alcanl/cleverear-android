package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.SelfFittingActivity
import com.alcanl.cleverear.databinding.LayoutAlertDialogDeviceDisconnectedBinding
import com.alcanl.cleverear.viewmodel.DeviceDisconnectedDialogListenersViewModel


class CustomAlertDialogDeviceDisconnected(activity: SelfFittingActivity) : Dialog(activity, R.style.DialogStyle) {
    val binding : LayoutAlertDialogDeviceDisconnectedBinding
    init {

        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        binding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_alert_dialog_device_disconnected, null, false)
        binding.viewModel = DeviceDisconnectedDialogListenersViewModel(activity)

        setContentView(binding.root)
    }
}