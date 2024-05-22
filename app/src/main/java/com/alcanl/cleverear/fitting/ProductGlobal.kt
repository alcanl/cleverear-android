package com.alcanl.cleverear.fitting

import com.ark.ArkException
import com.ark.Parameter
import com.ark.Product

private fun Product.isSystemParameter(parameterName: String) : Boolean
{
    return this.systemMemory.parameters.containsId(parameterName)
}
private fun setParameter(parameter: Parameter, value: Any)
{
    if (value is Int && parameter.isValidIntValue(value))
        parameter.value = value
    else if (value is Double && parameter.isValidDoubleValue(value))
        parameter.doubleValue = value
    else if (value is Boolean && parameter.isValidBooleanValue(value))
        parameter.isBooleanValue = value
    else
        throw ArkException("Invalid Parameter Name And Value Combination")

}
fun Product.setParameter(parameterName: String, value: Any)
{
    if (isSystemParameter(parameterName)) {
        setParameter(systemMemory.parameters.getById(parameterName), value)
    }
    else {
        setParameter(
            this.memories.getItem(currentMemory.value()).parameters.getById(parameterName),
            value
        )
    }
}
fun Product.readDoubleParameter(parameterName: String) : Double
{
    if (isSystemParameter(parameterName))
        return this.systemMemory.parameters.getById(parameterName).doubleValue

    return this.memories.getItem(currentMemory.value()).parameters.getById(parameterName).doubleValue
}

fun Product.readIntParameter(parameterName: String) : Int
{
    if (isSystemParameter(parameterName))
        return this.systemMemory.parameters.getById(parameterName).value

    return this.memories.getItem(currentMemory.value()).parameters.getById(parameterName).value
}

