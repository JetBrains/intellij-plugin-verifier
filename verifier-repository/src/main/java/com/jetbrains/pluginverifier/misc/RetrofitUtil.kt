package com.jetbrains.pluginverifier.misc

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
fun makeOkHttpClient(needLog: Boolean, timeOut: Long, timeUnit: TimeUnit): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(timeOut, timeUnit)
    .readTimeout(timeOut, timeUnit)
    .writeTimeout(timeOut, timeUnit)
    .addInterceptor(HttpLoggingInterceptor().setLevel(if (needLog) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
    .build()
