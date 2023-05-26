/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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

const val octetStreamMediaTypeValue = "application/octet-stream"

const val jsonMediaTypeValue = "application/json"

const val jarContentMediaTypeValue: String = "application/java-archive"

const val xJarContentMediaTypeValue: String = "application/x-java-archive"

fun createStringRequestBody(string: String): RequestBody = string.toRequestBody(stringMediaType)

fun createJsonRequestBody(json: String): RequestBody = json.toRequestBody(jsonMediaType)