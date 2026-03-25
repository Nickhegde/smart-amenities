package com.smartamenities.di

import com.smartamenities.data.repository.AmenityRepository
import com.smartamenities.data.repository.MockAmenityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module — tells the injector which concrete class to use
 * when something asks for an AmenityRepository.
 *
 * TO SWITCH TO REAL BACKEND:
 *   Change  `MockAmenityRepository`  to  `RemoteAmenityRepository`
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
}
