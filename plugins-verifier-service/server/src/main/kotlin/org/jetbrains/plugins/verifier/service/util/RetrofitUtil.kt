package org.jetbrains.plugins.verifier.service.util

import retrofit2.Call
import retrofit2.Response
import java.io.IOException

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
  val errorString = response.errorBody().string()
  throw RuntimeException("The response status code is ${response.code()}: ${if (errorString.length > 100) errorString.substring(0, 100) + "..." else errorString}")
}