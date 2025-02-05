package com.alcanl.cleverear.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class AtomicDouble @Inject constructor (initialValue: Double) {

    @Inject
    lateinit var mutex : Mutex

    private var value = initialValue

    suspend fun get(): Double = mutex.withLock { value }

    suspend fun set(newValue: Double)
    {
        mutex.withLock { value = newValue }
    }
}