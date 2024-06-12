package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.di.interceptor.DecibelArrayInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DecibelArrayModule {

    @Provides
    @Singleton
    @DecibelArrayInterceptor
    fun createDecibelValuesArray() : Array<Int>
    {
        return arrayOf(10, 15, 20, 25, 30, 35 ,40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100)
    }
}