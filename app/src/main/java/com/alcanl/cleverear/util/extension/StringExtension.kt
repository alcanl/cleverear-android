package com.alcanl.cleverear.util.extension

import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.MANUFACTURER_CODE_LEFT

fun String.recognizeEarSide(): EarSide
{
    return if (this.substring(0, 6) == MANUFACTURER_CODE_LEFT) EarSide.Left else EarSide.Right
}