package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.di.interceptor.DecibelArrayInterceptor
import com.alcanl.cleverear.di.interceptor.FrequencyArrayInterceptor
import com.github.mikephil.charting.data.Entry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataListModule {

    @Singleton
    @Provides
    fun createDataList(@FrequencyArrayInterceptor frequencyArray: Array<Int>,
                          @DecibelArrayInterceptor decibelArray: Array<Int>) : ArrayList<Entry>
    {
        val dataList = ArrayList<Entry>()
        for (i in frequencyArray.indices)
            dataList.add(Entry(decibelArray[0].toFloat(), frequencyArray[i].toFloat()))

        return dataList
    }
}