package com.alcanl.cleverear.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InSituResultMapModule {

    @Provides
    @Singleton
    fun createResultMap() : MutableMap<Float, Float> = HashMap()
}