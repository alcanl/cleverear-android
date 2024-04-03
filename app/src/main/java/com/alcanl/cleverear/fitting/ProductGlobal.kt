package com.alcanl.cleverear.fitting

import android.util.Log
import com.ark.Product

fun Product.getParameter(parameterName: String)
{
    val parameterValue = this.memories.getItem(currentMemory.value()).parameters.getById(parameterName)

    parameterValue.isBooleanValue

}
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