package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.SelfFittingActivity
import java.lang.ref.WeakReference

class DeviceDisconnectedDialogListenersViewModel(activity: SelfFittingActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleOkButtonClick()
    {
        mWeakReference.get()?.disconnectDialogOkButtonClicked()
    }
}