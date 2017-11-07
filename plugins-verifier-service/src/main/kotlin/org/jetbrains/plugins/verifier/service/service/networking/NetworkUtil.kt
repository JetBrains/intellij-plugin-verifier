package org.jetbrains.plugins.verifier.service.service.networking

import okhttp3.MediaType
import okhttp3.RequestBody

/**
 * @author Sergey Patrikeev
 */
private val STRING_MEDIA_TYPE: MediaType = MediaType.parse("text/plain")

private val JSON_MEDIA_TYPE: MediaType = MediaType.parse("application/json")

private val BYTE_ARRAY_MEDIA_TYPE: MediaType = MediaType.parse("application/octet-stream")

fun createStringRequestBody(string: String): RequestBody = RequestBody.create(STRING_MEDIA_TYPE, string)

fun createJsonRequestBody(json: String): RequestBody = RequestBody.create(JSON_MEDIA_TYPE, json)

fun createByteArrayRequestBody(byteArray: ByteArray): RequestBody = RequestBody.create(BYTE_ARRAY_MEDIA_TYPE, byteArray)