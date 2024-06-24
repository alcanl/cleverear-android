package com.alcanl.cleverear.di.module

import android.content.Context
import com.alcanl.cleverear.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSetModule {

    @Singleton
    @Provides
    fun createDataSet(dataList: ArrayList<Entry>, @ApplicationContext context: Context) : LineDataSet
    {
        return LineDataSet(dataList, context.getString(R.string.textview_in_situ_chart_header_text))
    }
}