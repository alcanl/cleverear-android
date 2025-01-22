package com.alcanl.cleverear.util.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
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

    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var view : View? = convertView

        if (view == null)
            view = LayoutInflater.from(context).inflate(R.layout.listview_item_layout, null)

        val textViewAdapter = view?.findViewById<TextView>(R.id.textViewAdapter)

        if (mIndex == position && mManufacturerData == MANUFACTURER_CODE_RIGHT)
            textViewAdapter?.setTextColor(context.getColor(R.color.right_device_slider))
        else
            textViewAdapter?.setTextColor(context.getColor(R.color.left_device_slider))

        textViewAdapter?.text = getItem(position)
        return view!!
    }
}