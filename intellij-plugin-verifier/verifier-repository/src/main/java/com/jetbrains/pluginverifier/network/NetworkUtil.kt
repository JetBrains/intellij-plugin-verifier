package com.jetbrains.pluginverifier.network

import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Executes this [Call] and returns its [Response].
 * Throws an exception if the call has failed.
 */
@Throws(
  InterruptedException::class,
  NotFound404ResponseException::class,
  ServerInternalError500Exception::class,
  ServerUnavailable503Exception::class,
  NonSuccessfulResponseException::class,
  FailedRequestException::class
)
fun <T> Call<T>.executeSuccessfully(): Response<T> = executeWithInterruptionCheck(
  onSuccess = { success -> success },
  onProblems = { problems ->
    if (problems.code() == 404) {
      throw NotFound404ResponseException(serverUrl)
    }
    if (problems.code() == 500) {
      throw ServerInternalError500Exception(serverUrl)
    }
    if (problems.code() == 503) {
      throw ServerUnavailable503Exception(serverUrl)
    }
    val message = problems.message() ?: problems.errorBody().string().take(100)
    throw NonSuccessfulResponseException(serverUrl, problems.code(), message)
  },
  onFailure = { error -> throw FailedRequestException(serverUrl, error) }
)

private val <T> Call<T>.serverUrl: String
  get() = "${request().url().host()}:${request().url().port()}"

@Throws(InterruptedException::class)
private fun <T, R> Call<T>.executeWithInterruptionCheck(
  onSuccess: (Response<T>) -> R,
  onProblems: (Response<T>) -> R,
  onFailure: (Throwable) -> R
): R {
  val responseRef = AtomicReference<Response<T>?>()
  val errorRef = AtomicReference<Throwable?>()
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
    checkIfInterrupted { cancel() }

    //Wait a little bit for the request to complete.
    try {
      Thread.sleep(100)
    } catch (ie: InterruptedException) {
      //Cancel this Call<T> if the thread has been interrupted
      cancel()
      throw ie
    }
  }

  /**
   * Rethrow the InterruptedException as indication
   * that this Call<T> has been cancelled.
   */
  if (isCanceled) {
    throw InterruptedException()
  }

  //The last check on whether the thread has been interrupted,
  //after the Call has completed.
  checkIfInterrupted()

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

/**
 * Copies [inputStream] to [destinationFile].
 * Updates the copying [progress].
 * Closes the [inputStream] on completion.
 * Throws [InterruptedException] if the copying
 * has been cancelled.
 */
@Throws(InterruptedException::class)
fun copyInputStreamToFileWithProgress(
  inputStream: InputStream,
  expectedSize: Long,
  destinationFile: File,
  progress: (Double) -> Unit
) {
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

  progress(0.0)
  inputStream.use { input ->
    destinationFile.outputStream().buffered().use { output ->
      checkIfInterrupted()
      var count: Long = 0
      while (true) {
        val n = input.read(buffer)
        if (n == -1) break
        output.write(buffer, 0, n)
        count += n
        if (expectedSize > 0) {
          progress(count.toDouble() / expectedSize)
        }
      }
    }
  }
  progress(1.0)
}
