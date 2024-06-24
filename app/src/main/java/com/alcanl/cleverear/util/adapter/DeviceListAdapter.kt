package com.alcanl.cleverear.util.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.alcanl.cleverear.R
import com.alcanl.cleverear.util.MANUFACTURER_CODE_RIGHT
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceListAdapter @Inject constructor(@ApplicationContext context: Context) : ArrayAdapter<String>(context, R.layout.listview_item_layout, ArrayList<String>(emptyList())) {
    private var mIndex = 0
    private var mManufacturerData = ""

    fun changeColorToRed(index: Int, manufacturerData: String)
    {
        mIndex = index
        mManufacturerData = manufacturerData
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getView(position, convertView, parent)

        val tv = v.findViewById<TextView>(R.id.textViewAdapter)

        if (mIndex == position && mManufacturerData == MANUFACTURER_CODE_RIGHT) {
            tv.setTextColor(context.getColor(R.color.right_device_slider))
        }
        else
            tv.setTextColor(context.getColor(R.color.left_device_slider))

        return v
    }
}