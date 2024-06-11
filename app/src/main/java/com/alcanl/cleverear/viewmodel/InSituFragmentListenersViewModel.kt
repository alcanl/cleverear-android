package com.alcanl.cleverear.viewmodel

import android.view.View
import android.widget.ImageButton
import com.alcanl.cleverear.fitting.fragment.InSituFragment
import com.alcanl.cleverear.util.EarSide
import java.lang.ref.WeakReference

class InSituFragmentListenersViewModel(fragment: InSituFragment) {
    private val mWeakReference = WeakReference(fragment)
    fun handleEarSideSelectButton(earSide: EarSide)
    {
        mWeakReference.get()?.earSideButtonClicked(earSide)
    }
    fun handlePlaySoundButton()
    {
        mWeakReference.get()?.playSoundButtonClicked()
    }
    fun handleFrequencyButtons(view: View)
    {
        mWeakReference.get()?.frequencyButtonsClicked(view as ImageButton)
    }
    fun handleDecibelButtons(value: Int)
    {
        mWeakReference.get()?.decibelButtonsClicked(value)
    }
    fun handleSaveButton()
    {
        mWeakReference.get()?.saveButtonClicked()
    }
    fun handleAutoFitButton()
    {
        mWeakReference.get()?.autoFitButtonClicked()
    }
}