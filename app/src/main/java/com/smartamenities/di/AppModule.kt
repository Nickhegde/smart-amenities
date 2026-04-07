package com.smartamenities.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.smartamenities.data.local.db.AmenityDao
import com.smartamenities.data.local.db.SmartAmenitiesDatabase
import com.smartamenities.data.remote.ApiService
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

        // Automatically picks the right host:
        //   Emulator → 10.0.2.2 (special alias for host machine localhost)
        //   Physical device → Mac's LAN IP (must be on same WiFi)
        private val BASE_URL: String get() {
            val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk")
            return if (isEmulator) "http://10.0.2.2:8000/" else "http://192.168.4.27:8000/"
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
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
