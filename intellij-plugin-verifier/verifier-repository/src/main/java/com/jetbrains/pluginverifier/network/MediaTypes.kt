package com.jetbrains.pluginverifier.network

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

val stringMediaType: MediaType = "text/plain".toMediaTypeOrNull()!!

val jsonMediaType: MediaType = "application/json".toMediaTypeOrNull()!!

val octetStreamMediaType: MediaType = "application/octet-stream".toMediaTypeOrNull()!!

val jarContentMediaType: MediaType = "application/java-archive".toMediaTypeOrNull()!!

val xJarContentMediaType: MediaType = "application/x-java-archive".toMediaTypeOrNull()!!

fun createStringRequestBody(string: String): RequestBody = string.toRequestBody(stringMediaType)

fun createJsonRequestBody(json: String): RequestBody = json.toRequestBody(jsonMediaType)