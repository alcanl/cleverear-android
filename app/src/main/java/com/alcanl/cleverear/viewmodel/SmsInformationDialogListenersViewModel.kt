package com.alcanl.cleverear.viewmodel

import android.view.View
import androidx.appcompat.widget.AppCompatButton
import com.alcanl.cleverear.MainContainerActivity
import java.lang.ref.WeakReference

class SmsInformationDialogListenersViewModel(activity: MainContainerActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleDialogButtons(view : View)
    {
        mWeakReference.get()?.onSMSContactDialogButtonClicked(view as AppCompatButton)
    }
}