package com.alcanl.cleverear.di.module

import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object HandlerModule {

    @Provides
    fun createBasicHandler() : Handler = Handler(Looper.myLooper()!!)
}