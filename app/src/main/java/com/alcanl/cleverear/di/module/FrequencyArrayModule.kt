package com.alcanl.cleverear.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import javax.inject.Singleton

@Module
@InstallIn(FragmentComponent::class)
object FrequencyArrayModule {

    @Provides
    @Singleton
    fun createFrequencyArray() : Array<Int>
    {
        return arrayOf(125, 250, 500, 750, 1000, 1500, 2000, 3000, 4000, 6000, 8000)
    }
}