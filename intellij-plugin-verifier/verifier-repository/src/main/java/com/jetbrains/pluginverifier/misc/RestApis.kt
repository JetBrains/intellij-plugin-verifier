package com.jetbrains.pluginverifier.misc

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
                  ThreadFactoryBuilder()
                          .setNameFormat("ok-http-thread-%d")
                          .setDaemon(true)
                          .build()
          ))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build()
}

class RestApis {
  private val httpClient = createHttpClient()
  private val gson = Gson()

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
    val payloadObject = gson.fromJson(responseJson, payloadType)
    return RestApiOk(payloadObject)
  }


  fun <T> getList(url: String, elementClass: Class<T>, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<List<T>> {
    val request = get(url, timeout)

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      return RestApiFailed(response.body(), response.statusCode())
    }
    val responseString = response.body()
    @Suppress("UNCHECKED_CAST")
    val listTypeToken = TypeToken
            .getParameterized(List::class.java, elementClass) as TypeToken<List<T>>

    val list = gson.fromJson(responseString, listTypeToken)
    return RestApiOk(list)
  }

  fun <T> getSigned(url: String, type: Class<T>, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<T> {
    val payloadBytes = when (val byteResult = getBytes(url, timeout)) {
      is RestApiOk<ByteArray> -> byteResult.payload
      is RestApiFailed -> return RestApiFailed(byteResult, byteResult.statusCode)
    }
    val signedContent = CMSSignedData(payloadBytes).signedContent.content as ByteArray
    val obj = gson.fromJson(XZCompressorInputStream(signedContent.inputStream()).reader(), getTypeToken(type))
    return RestApiOk(obj)
  }

  fun <T> getSignedList(url: String, elementClass: Class<T>, timeout: Duration = Duration.ofMinutes(5)): RestApiResult<List<T>> {
    val payloadBytes = when (val byteResult = getBytes(url, timeout)) {
      is RestApiOk<ByteArray> -> byteResult.payload
      is RestApiFailed -> return RestApiFailed(byteResult, byteResult.statusCode)
    }
    val signedContent = CMSSignedData(payloadBytes).signedContent.content as ByteArray
    val list = gson.fromJson(XZCompressorInputStream(signedContent.inputStream()).reader(), getTypeTokenForList(elementClass))
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

  @Suppress("UNCHECKED_CAST")
  private fun <T> getTypeTokenForList(elementClass: Class<T>): TypeToken<List<T>> {
    return TypeToken.getParameterized(List::class.java, elementClass) as TypeToken<List<T>>
  }

  private fun <T> getTypeToken(elementClass: Class<T>): TypeToken<T> {
    return TypeToken.get(elementClass)
  }
}

sealed class RestApiResult<T>
data class RestApiOk<T>(val payload: T) : RestApiResult<T>()
data class RestApiFailed<T>(val errorPayload: Any, val statusCode: Int) : RestApiResult<T>()