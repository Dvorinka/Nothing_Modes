package com.tdvorak.nothingmodes.di

import com.tdvorak.nothingmodes.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @Named("versionName")
    fun provideVersionName(): String = BuildConfig.VERSION_NAME
}
