package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.MainContainerActivity
import java.lang.ref.WeakReference

class SmsInformationDialogListenersViewModel(activity: MainContainerActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleButtonSaveContact()
    {
        mWeakReference.get()?.onDialogSaveContactButtonClicked()
    }

    fun handleButtonCancel()
    {
        mWeakReference.get()?.onDialogCancelButtonClicked()
    }

}