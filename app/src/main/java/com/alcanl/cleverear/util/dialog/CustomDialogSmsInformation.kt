package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.MainContainerActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutDialogSmsInformationBinding
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.viewmodel.SmsInformationDialogListenersViewModel
import java.lang.ref.WeakReference


class CustomDialogSmsInformation (activity: MainContainerActivity) : Dialog(activity, R.style.DialogStyle) {

    private val mWeakReference = WeakReference(activity)
    private var mBinding: LayoutDialogSmsInformationBinding

    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        mBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_dialog_sms_information,null, false)
        mBinding.viewModel = SmsInformationDialogListenersViewModel(activity)

        setContentView(mBinding.root)
    }

    fun saveSmsContactInformationToPref()
    {
        val numberNationalCode = mBinding.editTextContactNumberNationalCode.text?.toString()
            ?.ifBlank { "90" }?.toInt()
        val number = mBinding.editTextContactNumber.text?.toString()
            ?.ifBlank { "90" }?.toInt()
        val prefs = mWeakReference.get()?.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)
    }
}