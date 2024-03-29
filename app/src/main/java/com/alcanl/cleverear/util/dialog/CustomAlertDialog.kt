package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.ConnectionActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutAlertDialogEndConnectionBinding
import com.alcanl.cleverear.viewmodel.AlertDialogListenersViewModel

class CustomAlertDialog(activity: ConnectionActivity): Dialog(activity, R.style.DialogStyle) {
    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        val binding: LayoutAlertDialogEndConnectionBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_alert_dialog_end_connection, null, false)
        binding.viewModel = AlertDialogListenersViewModel(activity)
        setContentView(binding.root)
    }
}