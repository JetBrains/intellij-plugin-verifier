package com.jetbrains.pluginverifier.misc

import com.google.common.net.HttpHeaders.LOCATION
import com.google.common.util.concurrent.ThreadFactoryBuilder
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Creates [OkHttpClient] used to make network requests.
 *
 * @param needLog - whether to log HTTP requests and responses to console.
 * May be useful for debugging.
 * @param timeOut - timeout for requests and responses
 * @param timeUnit - time unit of [timeOut]
 */
fun createOkHttpClient(
    needLog: Boolean,
    timeOut: Long,
    timeUnit: TimeUnit
) = OkHttpClient.Builder()
    .dispatcher(Dispatcher(
        Executors.newCachedThreadPool(
            ThreadFactoryBuilder()
                .setNameFormat("Dispatcher")
                .setDaemon(true)
                .build()
        )
    ))
    .addInterceptor { chain: Interceptor.Chain ->
      // Manually handle PUT redirect,
      // can be removed when this issue will be fixed https://github.com/square/okhttp/issues/3111
      val request = chain.request()
      val response = chain.proceed(request)
      if (response.code() != 307 && response.code() != 308) {
        return@addInterceptor response
      }
      val location = response.header(LOCATION) ?: return@addInterceptor response
      val redirectedRequest = request.newBuilder().url(location).removeHeader("Authorization").build()
      chain.proceed(redirectedRequest)
    }
    .connectTimeout(timeOut, timeUnit)
    .readTimeout(timeOut, timeUnit)
    .writeTimeout(timeOut, timeUnit)
    .addInterceptor(HttpLoggingInterceptor().setLevel(
        if (needLog) {
          HttpLoggingInterceptor.Level.BASIC
        } else {
          HttpLoggingInterceptor.Level.NONE
        }
    ))
    .build()!!
