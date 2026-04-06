package com.smartamenities.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.smartamenities.data.local.db.AmenityDao
import com.smartamenities.data.local.db.AppDatabase
import com.smartamenities.data.repository.AmenityRepository
import com.smartamenities.data.repository.RoomAmenityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAmenityRepository(
        impl: RoomAmenityRepository
    ): AmenityRepository

    companion object {

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "smart_amenities.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        @Singleton
        fun provideAmenityDao(db: AppDatabase): AmenityDao = db.amenityDao()

        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences =
            context.getSharedPreferences("smartamenities_prefs", Context.MODE_PRIVATE)
    }
}
