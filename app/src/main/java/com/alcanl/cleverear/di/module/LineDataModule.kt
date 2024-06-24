package com.alcanl.cleverear.di.module

import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LineDataModule {
    @Provides
    @Singleton
    fun createLineData(lineDataSet: LineDataSet) : LineData = LineData(lineDataSet)
}