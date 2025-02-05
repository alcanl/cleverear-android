package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.EarSideSelectionActivity
import com.alcanl.cleverear.util.EarSide
import java.lang.ref.WeakReference

class EarSideSelectionActivityListenersViewModel (activity : EarSideSelectionActivity){
    private val mWeakReference = WeakReference(activity)

    fun handleLeftEarButton()
    {
        mWeakReference.get()?.onEarButtonClicked(EarSide.Left)
    }
    fun handleRightEarButton()
    {
        mWeakReference.get()?.onEarButtonClicked(EarSide.Right)
    }
    fun handleBackButton()
    {
        mWeakReference.get()?.onBackButtonClicked()
    }
}