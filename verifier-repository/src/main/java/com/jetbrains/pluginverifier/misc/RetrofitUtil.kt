package com.jetbrains.pluginverifier.misc

import com.google.common.util.concurrent.ThreadFactoryBuilder
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun makeOkHttpClient(needLog: Boolean, timeOut: Long, timeUnit: TimeUnit): OkHttpClient = OkHttpClient.Builder()
    .dispatcher(Dispatcher(Executors.newCachedThreadPool(
        ThreadFactoryBuilder()
            .setNameFormat("Dispatcher")
            .setDaemon(true)
            .build())
    ))
    .connectTimeout(timeOut, timeUnit)
    .readTimeout(timeOut, timeUnit)
    .writeTimeout(timeOut, timeUnit)
    .addInterceptor(HttpLoggingInterceptor().setLevel(if (needLog) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
    .build()
