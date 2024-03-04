package com.alcanl.cleverear.helpers.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.alcanl.cleverear.R

class DeviceListAdapter(context: Context) : ArrayAdapter<String>(context, R.layout.listview_item_layout, ArrayList<String>(emptyList())) {
    private var isRightDevice = false
    fun isRightDevice(boolean: Boolean) {
        isRightDevice = boolean
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getView(position, convertView, parent)

        val tv = v.findViewById<TextView>(R.id.textViewAdapter)

        if (isRightDevice) {
            tv.setTextColor(context.getColor(R.color.right_device_slider))
        }
        else
            tv.setTextColor(context.getColor(R.color.left_device_slider))

        return v
    }
}