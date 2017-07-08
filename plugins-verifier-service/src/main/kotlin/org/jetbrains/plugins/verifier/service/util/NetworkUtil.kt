package org.jetbrains.plugins.verifier.service.util

import com.jetbrains.pluginverifier.persistence.CompactJson
import okhttp3.MediaType
import okhttp3.RequestBody

/**
 * @author Sergey Patrikeev
 */
private val STRING_MEDIA_TYPE: MediaType = MediaType.parse("text/plain")

private val JSON_MEDIA_TYPE: MediaType = MediaType.parse("application/json")

fun createStringRequestBody(string: String): RequestBody = RequestBody.create(STRING_MEDIA_TYPE, string)

fun createCompactJsonRequestBody(obj: Any): RequestBody = RequestBody.create(JSON_MEDIA_TYPE, CompactJson.toJson(obj))