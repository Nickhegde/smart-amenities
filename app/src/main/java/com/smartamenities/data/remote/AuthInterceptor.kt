package com.smartamenities.data.remote

import com.smartamenities.data.local.UserDataStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that attaches `Authorization: Bearer <token>` to every
 * request whose path contains "admin". Public endpoints (route, auth) are
 * left untouched.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val userDataStore: UserDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = userDataStore.getToken()

        return if (token != null && original.url.pathSegments.contains("admin")) {
            val request = original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        } else {
            chain.proceed(original)
        }
    }
}
