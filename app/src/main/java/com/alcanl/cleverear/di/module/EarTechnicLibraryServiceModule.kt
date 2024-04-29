package com.alcanl.cleverear.di.module

import com.alcanl.cleverear.api.IEarTechnicLibraryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object EarTechnicLibraryServiceModule {
    @Provides
    fun create(retrofit: Retrofit) : IEarTechnicLibraryService
    {
        return retrofit.create(IEarTechnicLibraryService::class.java)
    }
}