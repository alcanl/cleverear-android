package com.alcanl.cleverear.util.extension

import android.view.View
import com.alcanl.cleverear.databinding.FragmentHomeBinding

fun FragmentHomeBinding.showFrames()
{
    frameLayoutDeviceMac.visibility = View.VISIBLE
    frameLayoutLibraryId.visibility = View.VISIBLE
    frameLayoutFirmwareId.visibility = View.VISIBLE
    frameLayoutFirmwareVersion.visibility = View.VISIBLE
    frameLayoutDeviceModel.visibility = View.VISIBLE
}
fun FragmentHomeBinding.hideFrames()
{
    frameLayoutLibraryId.setChildrenInvisible()
    frameLayoutFirmwareId.setChildrenInvisible()
    frameLayoutFirmwareVersion.setChildrenInvisible()
    frameLayoutDeviceModel.setChildrenInvisible()
    frameLayoutDeviceMac.setChildrenInvisible()
}