package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.MainContainerActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutDialogSmsInformationBinding
import com.alcanl.cleverear.util.EMERGENCY_NUMBER
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.viewmodel.SmsInformationDialogListenersViewModel
import java.lang.ref.WeakReference


class CustomDialogSmsInformation (activity: MainContainerActivity) : Dialog(activity, R.style.DialogStyle) {

    private val mWeakReference = WeakReference(activity)
    var binding: LayoutDialogSmsInformationBinding

    init {
        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        binding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_dialog_sms_information,null, false)
        binding.viewModel = SmsInformationDialogListenersViewModel(activity)

        setContentView(binding.root)
    }

    fun saveSmsContactInformationToPref()
    {
        val phoneNumber = (binding.editTextContactNumberNationalCode.text?.toString()
            ?.ifBlank {
                with (mWeakReference.get()!!) {
                    val code = this.getString(R.string.edittext_contact_number_national_code_hint)
                    code.substring(1, code.length)
                } + binding.editTextContactNumber.text?.toString() })

        val editor = mWeakReference.get()?.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)?.edit()
        editor?.putString(EMERGENCY_NUMBER, phoneNumber!!)
        editor?.apply()
    }
}