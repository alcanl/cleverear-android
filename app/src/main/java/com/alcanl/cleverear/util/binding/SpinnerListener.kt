package com.alcanl.cleverear.util.binding

import androidx.databinding.BindingAdapter
import com.skydoves.powerspinner.PowerSpinnerView

object SpinnerListener {

    @JvmStatic
    @BindingAdapter(value = ["app:onItemSelected"])
    fun setOnValueChangeListener(spinner: PowerSpinnerView, listener: OnItemSelected)
    {
        spinner.setOnSpinnerItemSelectedListener<Int> { _,_,index,_ ->
            listener.onItemSelected(index)
        }
    }

    interface OnItemSelected {
        fun onItemSelected(value: Int)
    }

    @JvmStatic
    @BindingAdapter(value = ["app:selectedItemPosition"])
    fun setSelectedItemPosition(spinner: PowerSpinnerView, pos: Int)
    {
        spinner.selectItemByIndex(pos)
    }

}