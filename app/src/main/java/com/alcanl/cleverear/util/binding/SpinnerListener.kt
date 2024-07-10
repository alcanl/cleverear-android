package com.alcanl.cleverear.util.binding

import androidx.databinding.BindingAdapter
import com.skydoves.powerspinner.IconSpinnerItem
import com.skydoves.powerspinner.PowerSpinnerView

object SpinnerListener {

    @JvmStatic
    @BindingAdapter(value = ["onItemSelected"])
    fun setOnValueChangeListener(spinner: PowerSpinnerView, listener: OnItemSelected)
    {
        spinner.setOnSpinnerItemSelectedListener<IconSpinnerItem> { _, _, index, _ ->
            listener.onItemSelected(index)
        }
    }

    interface OnItemSelected {
        fun onItemSelected(value: Int)
    }

}