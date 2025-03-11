package com.alcanl.cleverear.util

import kotlinx.serialization.Serializable

@Serializable
enum class EarSide(private val valTR: String, private val valEN: String, private val valGE: String) {
    Left("Sol", "Left", "Links"), Right("Sağ", "Right", "Rechts");

    fun getTR() = valTR
    fun getEN() = valEN
    fun getGE() = valGE
}