package com.smartamenities.di

import android.content.Context
import android.content.SharedPreferences
import com.smartamenities.data.repository.AmenityRepository
import com.smartamenities.data.repository.MockAmenityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module — provides/binds all application-level dependencies.
 *
 * TO SWITCH TO REAL BACKEND:
 *   Change `MockAmenityRepository` to `RemoteAmenityRepository`.
 *   That's the only line you'll need to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAmenityRepository(
        impl: MockAmenityRepository
    ): AmenityRepository

    companion object {
        /** SharedPreferences used by UserDataStore for local auth/session storage. */
        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences =
            context.getSharedPreferences("smartamenities_prefs", Context.MODE_PRIVATE)
    }
}
