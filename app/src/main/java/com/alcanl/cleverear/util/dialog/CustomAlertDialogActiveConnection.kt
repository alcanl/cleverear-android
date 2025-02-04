package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutAlertDialogEndConnectionBinding
import com.alcanl.cleverear.viewmodel.ActiveConnectionDialogListenersViewModel
import com.alcanl.cleverear.fragment.ConnectionFragment

class CustomAlertDialogActiveConnection(fragment: ConnectionFragment): Dialog(fragment.requireContext(), R.style.RoundedDialogTheme) {
    init {
        setCancelable(false)
        val binding: LayoutAlertDialogEndConnectionBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_alert_dialog_end_connection, null, false)
        binding.viewModel = ActiveConnectionDialogListenersViewModel(fragment)
        setContentView(binding.root)
    }
}