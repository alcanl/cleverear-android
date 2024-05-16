package com.alcanl.cleverear.viewmodel

import android.view.View
import com.alcanl.cleverear.ConnectionActivity
import java.lang.ref.WeakReference

class ActiveConnectionDialogListenersViewModel(activity: ConnectionActivity){
    private val mWeakReference = WeakReference(activity)

    fun handlePositiveButton()
    {
        mWeakReference.get()!!.alertDialogPositiveButtonClicked()
    }
    fun handleNegativeButton(view: View)
    {
        mWeakReference.get()!!.alertDialogNegativeButtonClicked(view)
    }

}