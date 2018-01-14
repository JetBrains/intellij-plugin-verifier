package com.jetbrains.pluginverifier.network

import okhttp3.MediaType
import okhttp3.RequestBody

val stringMediaType: MediaType = MediaType.parse("text/plain")

val jsonMediaType: MediaType = MediaType.parse("application/json")

val byteArrayMediaType: MediaType = MediaType.parse("application/octet-stream")

val jarContentMediaType: MediaType = MediaType.parse("application/java-archive")

fun createStringRequestBody(string: String): RequestBody = RequestBody.create(stringMediaType, string)

fun createJsonRequestBody(json: String): RequestBody = RequestBody.create(jsonMediaType, json)

fun createByteArrayRequestBody(byteArray: ByteArray): RequestBody = RequestBody.create(byteArrayMediaType, byteArray)