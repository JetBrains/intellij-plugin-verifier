package com.jetbrains.pluginverifier.misc

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.pluginverifier.network.threadFactory
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.bouncycastle.cms.CMSSignedData
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors

fun createHttpClient(timeout: Duration = Duration.ofMinutes(5)): HttpClient {
  return HttpClient.newBuilder().connectTimeout(timeout)
          .executor(Executors.newCachedThreadPool(
            threadFactory("plugin-verifier-http-%d", daemon = true)
          ))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build()
}

class RestApis {
  private val httpClient = createHttpClient()
  private val jackson = jacksonObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  fun getRawString(url: String, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<String> {
    val request = get(url, timeout)

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      return RestApiFailed(response.body(), response.statusCode())
    }
    return RestApiOk(response.body())
  }

  fun <T> get(url: String, payloadType: Class<T>, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<T> {
    val request = get(url, timeout)

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      return RestApiFailed(response.body(), response.statusCode())
    }
    val responseJson = response.body()
    val payloadObject = jackson.readValue(responseJson, payloadType)
    return RestApiOk(payloadObject)
  }


  fun <T> getList(url: String, elementClass: Class<T>, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<List<T>> {
    val request = get(url, timeout)

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      return RestApiFailed(response.body(), response.statusCode())
    }
    val responseString = response.body()
    val listTypeToken = TypeFactory.defaultInstance().constructCollectionType(List::class.java, elementClass)
    val list: List<T>  = jackson.readValue(responseString, listTypeToken)
    return RestApiOk(list)
  }

  fun <T> getSigned(url: String, type: Class<T>, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<T> {
    val payloadBytes = when (val byteResult = getBytes(url, timeout)) {
      is RestApiOk<ByteArray> -> byteResult.payload
      is RestApiFailed -> return RestApiFailed(byteResult, byteResult.statusCode)
    }
    val signedContent = CMSSignedData(payloadBytes).signedContent.content as ByteArray
    val result = jackson.readValue(XZCompressorInputStream(signedContent.inputStream()).reader(), type)
    return RestApiOk(result)
  }

  fun <T> getSignedList(url: String, elementClass: Class<T>, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<List<T>> {
    val payloadBytes = when (val byteResult = getBytes(url, timeout)) {
      is RestApiOk<ByteArray> -> byteResult.payload
      is RestApiFailed -> return RestApiFailed(byteResult, byteResult.statusCode)
    }
    val signedContent = CMSSignedData(payloadBytes).signedContent.content as ByteArray
    val list: List<T> = jackson.readValue(XZCompressorInputStream(signedContent.inputStream()).reader(), getTypeTokenForList(elementClass))
    return RestApiOk(list)
  }

  fun getBytes(url: String, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<ByteArray> {
    val httpGet = get(url, timeout)

    val response = httpClient.send(httpGet, HttpResponse.BodyHandlers.ofByteArray())
    if (response.statusCode() != 200) {
      return RestApiFailed(response.body(), response.statusCode())
    }
    return RestApiOk(response.body())
  }

  private fun get(url: String, timeout: Duration): HttpRequest = HttpRequest.newBuilder().GET()
          .uri(URI.create(url))
          .timeout(timeout)
          .build()

  private fun <T> getTypeTokenForList(elementClass: Class<T>): CollectionType =
          TypeFactory.defaultInstance().constructCollectionType(List::class.java, elementClass)
}

sealed class RestApiResult<T>
data class RestApiOk<T>(val payload: T) : RestApiResult<T>()
data class RestApiFailed<T>(val errorPayload: Any, val statusCode: Int) : RestApiResult<T>()