package com.alcanl.cleverear.fragment

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutAlertDialogEndConnectionFragmentBinding

class CustomAlertDialogActiveConnection(fragment: ConnectionFragment): Dialog(fragment.requireContext(), R.style.DialogStyle) {
    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        val binding: LayoutAlertDialogEndConnectionFragmentBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_alert_dialog_end_connection_fragment, null, false)
        binding.viewModel = ActiveConnectionDialogListenersViewModelFragment(fragment)
        setContentView(binding.root)
    }
}