package com.alcanl.cleverear.fitting

import android.util.Log
import com.ark.Product
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Product.setParameter(parameterName: String, value: Int)
{
    val parameter = this.memories.getItem(currentMemory.value()).parameters.getById(parameterName)

    if (!parameter.isValidIntValue(value)) {
        Log.e("Parameter", "Invalid Int Value For Default Range")
        return
    }

    parameter.value = value
}
fun Product.setParameter(parameterName: String, value: Boolean)
{
    this.memories.getItem(currentMemory.value()).parameters.getById(parameterName).isBooleanValue = value
}
fun Product.setParameter(parameterName: String, value: Double)
{
    val parameter = this.memories.getItem(currentMemory.value()).parameters.getById(parameterName)

    if (!parameter.isValidDoubleValue(value)) {
        Log.e("Parameter", "Invalid Int Value For Default Range")
        return
    }

    parameter.doubleValue = value
}