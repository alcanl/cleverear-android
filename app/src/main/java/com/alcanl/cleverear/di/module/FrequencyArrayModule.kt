package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.di.interceptor.FrequencyArrayInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FrequencyArrayModule {

    @Provides
    @Singleton
    @FrequencyArrayInterceptor
    fun createFrequencyArray() : Array<Int>
    {
        return arrayOf(125, 250, 500, 750, 1000, 1500, 2000, 3000, 4000, 6000, 8000)
    }
}