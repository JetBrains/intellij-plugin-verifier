package org.jetbrains.plugins.verifier.service.service

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
fun makeClient(needLog: Boolean): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.MINUTES)
    .readTimeout(5, TimeUnit.MINUTES)
    .writeTimeout(5, TimeUnit.MINUTES)
    .followRedirects(false)
    .addInterceptor(HttpLoggingInterceptor().setLevel(if (needLog) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
    .build()

val STRING_MEDIA_TYPE: MediaType = MediaType.parse("text/plain")

fun createStringRequestBody(string: String): RequestBody = RequestBody.create(STRING_MEDIA_TYPE, string)