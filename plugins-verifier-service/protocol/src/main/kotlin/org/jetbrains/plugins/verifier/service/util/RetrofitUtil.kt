package org.jetbrains.plugins.verifier.service.util

import com.jetbrains.pluginverifier.persistence.GsonHolder
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.IOException

object MultipartUtil {

  fun createJsonPart(partName: String, obj: Any): MultipartBody.Part =
      createJsonPart(partName, GsonHolder.GSON.toJson(obj))

  fun createJsonPart(partName: String, json: String): MultipartBody.Part =
      MultipartBody.Part.createFormData(partName, null, RequestBody.create(MediaTypes.JSON, json))

  fun createFilePart(partName: String, file: File): MultipartBody.Part =
      MultipartBody.Part.createFormData(partName, file.name, RequestBody.create(MediaTypes.OCTET_STREAM, file))

}

object MediaTypes {
  val JSON: MediaType = MediaType.parse("application/json")
  val OCTET_STREAM: MediaType = MediaType.parse("application/octet-stream")
}

fun <T> Call<T>.executeSuccessfully(): Response<T> {
  val server = "${this.request().url().host()}:${this.request().url().port()}"
  val response: Response<T>?
  try {
    response = this.execute()
  } catch(e: IOException) {
    throw RuntimeException("The server $server is not available", e)
  }
  if (response.isSuccessful) {
    return response
  }
  if (response.code() == 500) {
    throw RuntimeException("The server $server has faced unexpected problems (500 Internal Server Error)")
  }
  throw RuntimeException("The response status code is ${response.code()}: ${response.errorBody().string()}")
}