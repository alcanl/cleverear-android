package com.alcanl.cleverear.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Singleton

const val SCHEDULED_THREAD_POOL_SIZE = 5

@Module
@InstallIn(SingletonComponent::class)
object ExecutorServiceModule {

    @Provides
    @Singleton
    fun createExecutorService() : ExecutorService = Executors.newCachedThreadPool()

    @Provides
    fun createScheduledThreadPool() : ScheduledExecutorService = Executors.newScheduledThreadPool(SCHEDULED_THREAD_POOL_SIZE)

}