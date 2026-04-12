package com.smartamenities.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.smartamenities.data.local.db.AmenityDao
import com.smartamenities.data.local.db.SmartAmenitiesDatabase
import com.smartamenities.data.remote.ApiService
import com.smartamenities.data.remote.AuthInterceptor
import com.smartamenities.data.repository.AmenityRepository
import com.smartamenities.data.repository.RemoteAmenityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Hilt DI module — provides/binds all application-level dependencies.
 *
 * TO SWITCH BACK TO MOCK:
 *   Change `RemoteAmenityRepository` to `MockAmenityRepository` in bindAmenityRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAmenityRepository(
        impl: RemoteAmenityRepository
    ): AmenityRepository

    companion object {

        private const val BASE_URL = "http://18.188.216.120/"

        @Provides
        @Singleton
        fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .addInterceptor(authInterceptor)
                .build()

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        @Provides
        @Singleton
        fun provideApiService(retrofit: Retrofit): ApiService =
            retrofit.create(ApiService::class.java)

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): SmartAmenitiesDatabase =
            Room.databaseBuilder(
                context,
                SmartAmenitiesDatabase::class.java,
                "smartamenities.db"
            ).build()

        @Provides
        @Singleton
        fun provideAmenityDao(db: SmartAmenitiesDatabase): AmenityDao = db.amenityDao()

        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences =
            context.getSharedPreferences("smartamenities_prefs", Context.MODE_PRIVATE)
    }
}
