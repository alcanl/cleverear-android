package com.alcanl.cleverear.util.scan

import com.alcanl.cleverear.util.EarSide
import com.ark.AsyncResult
import com.ark.CommunicationPort
import com.ark.ProductManager
import com.ark.WirelessProgrammerType
import javax.inject.Inject

class DeviceScanner @Inject constructor(){
    @Inject
    lateinit var productManager: ProductManager

    private fun scanDevice(earSide: EarSide) : AsyncResult
    {
        return productManager.beginScanForWirelessDevices(WirelessProgrammerType.kPlatformDefault, "",
            if (earSide == EarSide.Left) CommunicationPort.kLeft else CommunicationPort.kRight, "", false)
    }
    fun scanForLeftDevice() : AsyncResult
    {
        return scanDevice(EarSide.Left)
    }
    fun scanForRightDevice() : AsyncResult
    {
        return scanDevice(EarSide.Right)
    }
    fun endScanProcess(asyncResult: AsyncResult)
    {
        productManager.endScanForWirelessDevices(asyncResult)
    }
}