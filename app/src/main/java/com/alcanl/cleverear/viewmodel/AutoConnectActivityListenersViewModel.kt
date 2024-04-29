package com.alcanl.cleverear.viewmodel

import androidx.core.content.ContextCompat.getString
import com.alcanl.cleverear.AutoConnectActivity
import com.alcanl.cleverear.R
import java.lang.ref.WeakReference

class AutoConnectActivityListenersViewModel(activity: AutoConnectActivity) {
    private val mWeakReference = WeakReference(activity)

    fun getLeftEarModelNumber() : String
    {
        val model = mWeakReference.get()?.getLeftEarModelNumber()!!

        return model.ifBlank { getString(mWeakReference.get()!!, R.string.textview_no_previous_device_info) }
    }
    fun getRightEarModelNumber() : String
    {
        val model = mWeakReference.get()?.getRightEarModelNumber()!!

        return model.ifBlank { getString(mWeakReference.get()!!, R.string.textview_no_previous_device_info) }
    }
    fun handleLeftEarButtonClick()
    {
        mWeakReference.get()?.leftEarButtonClicked()
    }
    fun handleRightEarButtonClick()
    {
        mWeakReference.get()?.rightEarButtonClicked()
    }
    fun handleControlButtonClick()
    {
        mWeakReference.get()?.controlButtonClicked()
    }
    fun handleRemoteFittingButtonClick()
    {
        mWeakReference.get()?.remoteFittingButtonClicked()
    }
}