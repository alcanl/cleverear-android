package com.alcanl.cleverear.viewmodel

import androidx.core.content.ContextCompat.getString
import com.alcanl.cleverear.AutoConnectActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.util.getLeftHearingAidModelNumber
import com.alcanl.cleverear.util.getRightHearingAidModelNumber
import java.lang.ref.WeakReference

class AutoConnectActivityListenersViewModel(activity: AutoConnectActivity) {
    private val mWeakReference = WeakReference(activity)

    fun getLeftEarModelNumber() : String
    {
        val model = getLeftHearingAidModelNumber(mWeakReference.get()!!)
        return model.ifBlank { getString(mWeakReference.get()!!, R.string.textview_no_previous_device_info) }
    }
    fun getRightEarModelNumber() : String
    {
        val model = getRightHearingAidModelNumber(mWeakReference.get()!!)
        return model.ifBlank { getString(mWeakReference.get()!!, R.string.textview_no_previous_device_info) }
    }
    fun handleLeftEarButtonClick()
    {
        mWeakReference.get()?.onLeftEarButtonClicked()
    }
    fun handleRightEarButtonClick()
    {
        mWeakReference.get()?.onRightEarButtonClicked()
    }
    fun handleControlButtonClick()
    {
        mWeakReference.get()?.onControlButtonClicked()
    }
    fun handleRemoteFittingButtonClick()
    {
        mWeakReference.get()?.onRemoteFittingButtonClicked()
    }
    fun handleBackButton()
    {
        mWeakReference.get()?.onBackButtonClicked()
    }
}