package com.alcanl.cleverear.util.binding

import androidx.databinding.BindingAdapter
import com.wisal.android.views.batteryview.BatteryView

object BatteryViewValueBinder {

    @JvmStatic
    @BindingAdapter(value = ["battery_level"])
    fun bindValue(batteryView: BatteryView, value: Int)
    {
        batteryView.batteryLevel = value
    }
}