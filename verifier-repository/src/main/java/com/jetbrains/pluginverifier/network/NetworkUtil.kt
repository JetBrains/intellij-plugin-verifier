package com.jetbrains.pluginverifier.network

import okhttp3.MediaType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

fun <T> Call<T>.executeSuccessfully(): Response<T> = executeWithInterruptionCheck(
    onSuccess = { success ->
      success
    },
    onProblems = { problems ->
      if (problems.code() == 404) {
        throw NotFound404ResponseException(serverUrl)
      }
      if (problems.code() == 500) {
        throw Server500ResponseException(serverUrl)
      }
      val message = problems.message() ?: problems.errorBody().string().take(100)
      throw NonSuccessfulResponseException(problems.code(), serverUrl, message)
    },
    onFailure = { error ->
      throw RuntimeException("Unable to communicate with $serverUrl: ${error.message}", error)
    }
)

private val <T> Call<T>.serverUrl: String
  get() = "${request().url().host()}:${request().url().port()}"

private fun <T, R> Call<T>.executeWithInterruptionCheck(onSuccess: (Response<T>) -> R,
                                                        onProblems: (Response<T>) -> R,
                                                        onFailure: (Throwable) -> R): R {
  val responseRef = AtomicReference<Response<T>?>(null)
  val errorRef = AtomicReference<Throwable?>(null)
  val finished = AtomicBoolean()

  enqueue(object : Callback<T> {
    override fun onResponse(call: Call<T>, response: Response<T>) {
      responseRef.set(response)
      finished.set(true)
    }

    override fun onFailure(call: Call<T>, error: Throwable) {
      errorRef.set(error)
      finished.set(true)
    }
  })

  while (!finished.get()) {
    if (Thread.currentThread().isInterrupted) {
      cancel()
      throw InterruptedException()
    }
    Thread.sleep(100)
  }

  val response = responseRef.get()
  val error = errorRef.get()
  return if (response != null) {
    if (response.isSuccessful) {
      onSuccess(response)
    } else {
      onProblems(response)
    }
  } else {
    onFailure(error!!)
  }
}
