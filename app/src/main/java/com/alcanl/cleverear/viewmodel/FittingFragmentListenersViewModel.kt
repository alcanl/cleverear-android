package com.alcanl.cleverear.viewmodel

import android.view.View
import com.alcanl.cleverear.fitting.fragment.FittingFragment
import com.alcanl.cleverear.util.EarSide
import java.lang.ref.WeakReference

class FittingFragmentListenersViewModel(fragment: FittingFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleCheckBoxCheckedChanged()
    {

    }
    fun handleEarSideSelectButton(earSide: EarSide)
    {

    }
    fun handleSliderOnValueChanged(view: View)
    {

    }
    fun handleSpinnerItemSelect(pos: Int)
    {

    }
}