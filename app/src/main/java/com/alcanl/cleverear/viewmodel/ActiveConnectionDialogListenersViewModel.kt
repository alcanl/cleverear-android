package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fragment.ConnectionFragment
import java.lang.ref.WeakReference

class ActiveConnectionDialogListenersViewModel(fragment: ConnectionFragment){
    private val mWeakReference = WeakReference(fragment)

    fun handlePositiveButton()
    {
        mWeakReference.get()!!.onAlertDialogPositiveButtonClicked()
    }
    fun handleNegativeButton()
    {
        mWeakReference.get()!!.onAlertDialogNegativeButtonClicked()
    }

}