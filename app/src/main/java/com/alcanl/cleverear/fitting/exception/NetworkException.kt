package com.alcanl.cleverear.fitting.exception

class NetworkException(cause: Throwable) : Exception() {
    private val mCause = cause
    override val message: String?
        get() = mCause.message

}