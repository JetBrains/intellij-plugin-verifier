package com.jetbrains.pluginverifier.client.network

import com.github.salomonbrys.kotson.fromJson
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.persistence.GsonHolder
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.Status
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

val LOG: Logger = LoggerFactory.getLogger(VerifierService::class.java)

object MediaTypes {
  val JSON: MediaType = MediaType.parse("application/json")
  val OCTET_STREAM: MediaType = MediaType.parse("application/octet-stream")
}

object MultipartUtil {

  fun createJsonPart(partName: String, obj: Any): MultipartBody.Part =
      createJsonPart(partName, GsonHolder.GSON.toJson(obj))

  fun createJsonPart(partName: String, json: String): MultipartBody.Part =
      MultipartBody.Part.createFormData(partName, null, RequestBody.create(MediaTypes.JSON, json))

  fun createFilePart(partName: String, file: File): MultipartBody.Part =
      MultipartBody.Part.createFormData(partName, file.name, RequestBody.create(MediaTypes.OCTET_STREAM, file))

}

val REQUEST_PERIOD: Long = 5000

fun <T> Call<T>.executeSuccessfully(): Response<T> {
  val server = "${this.request().url().host()}:${this.request().url().port()}"
  val response: Response<T>?
  try {
    response = this.execute()
  } catch(e: IOException) {
    throw RuntimeException("The server $server is not available")
  }
  if (response.isSuccessful) {
    return response
  }
  if (response.code() == 500) {
    throw RuntimeException("The server $server has faced unexpected problems (500 Internal Server Error)")
  }
  throw RuntimeException("The response status code is ${response.code()}: ${response.errorBody().string()}")
}

fun parseTaskId(response: Response<ResponseBody>): TaskId = parseResponse(response)

inline fun <reified T : Any> parseResponse(response: Response<ResponseBody>): T {
  if (response.isSuccessful) {
    return GsonHolder.GSON.fromJson(response.body().string())
  }
  throw IllegalStateException("The response status code is ${response.code()}: ${response.errorBody().string()}")
}

inline fun <reified T : Any> waitCompletion(service: VerifierService, taskId: TaskId): T {
  val resultType = ParameterizedTypeImpl.make(Result::class.java, arrayOf(T::class.java), null)

  while (true) {
    val response: Response<ResponseBody> = service.taskResultsService.getTaskResult(taskId).executeSuccessfully()
    val json = response.body().string()

    val result: Result<T> = GsonHolder.GSON.fromJson(json, resultType)

    val taskStatus = result.taskStatus
    when (taskStatus.status) {
      Status.WAITING, Status.RUNNING -> {
        LOG.debug("The task status: $taskStatus")
        Thread.sleep(REQUEST_PERIOD)
      }
      Status.COMPLETE -> {
        LOG.debug("The task $taskId is complete.")
        return result.result!!
      }
    }
  }
}


/**
 * @author Sergey Patrikeev
 */
class VerifierService(host: String) {

  val enqueueTaskService: EnqueueTaskApi = Retrofit.Builder()
      .baseUrl(host)
      .client(makeClient())
      .build()
      .create(EnqueueTaskApi::class.java)

  val taskResultsService: TaskResultsApi = Retrofit.Builder()
      .baseUrl(host)
      .addConverterFactory(GsonConverterFactory.create(GsonHolder.GSON))
      .client(makeClient())
      .build()
      .create(TaskResultsApi::class.java)

  val reportsService: ReportsApi = Retrofit.Builder()
      .baseUrl(host)
      .addConverterFactory(GsonConverterFactory.create(GsonHolder.GSON))
      .client(makeClient())
      .build()
      .create(ReportsApi::class.java)

  private fun makeClient(): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.MINUTES)
      .readTimeout(5, TimeUnit.MINUTES)
      .writeTimeout(5, TimeUnit.MINUTES)
      .addInterceptor(HttpLoggingInterceptor().setLevel(if (LOG.isDebugEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
      .build()

}

interface TaskResultsApi {
  @Multipart
  @POST("/verifier/taskStatus")
  fun getTaskResult(@Part("taskId") taskId: TaskId): Call<ResponseBody>

  @Multipart
  @POST("/verifier/cancelTask")
  fun cancelTask(@Part("taskId") taskId: TaskId): Call<ResponseBody>

}

interface ReportsApi {
  @Multipart
  @POST("/reports/upload")
  fun uploadReport(@Part reportFile: MultipartBody.Part): Call<ResponseBody>

  @GET("/reports/list")
  fun listReports(): Call<List<IdeVersion>>

  @GET("/reports/get")
  fun get(@Query("ideVersion") ideVersion: String): Call<ResponseBody>
}

interface EnqueueTaskApi {

  /**
   * The check-ide command.
   * The result is of type [com.jetbrains.pluginverifier.configurations.CheckIdeResults]
   */
  @Multipart
  @POST("/verifier/checkIde")
  fun checkIde(@Part ideFile: MultipartBody.Part, @Part parameters: MultipartBody.Part): Call<ResponseBody>

  /**
   * The check-trunk-api command.
   * The result is of type [org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults]
   */
  @Multipart
  @POST("/verifier/checkTrunkApi")
  fun checkTrunkApi(@Part ideFile: MultipartBody.Part, @Part parameters: MultipartBody.Part): Call<ResponseBody>

  /**
   * The check plugin against <since; until> builds range.
   * The result is of type [org.jetbrains.plugins.verifier.service.results.CheckPluginAgainstSinceUntilBuildsResults]
   */
  @Multipart
  @POST("/verifier/checkPluginAgainstSinceUntilBuilds")
  fun checkPluginAgainstSinceUntilBuilds(@Part pluginFile: MultipartBody.Part, @Part parameters: MultipartBody.Part): Call<ResponseBody>

  /**
   * The check-plugin command.
   * The result is of type [com.jetbrains.pluginverifier.configurations.CheckPluginResults]
   */
  @POST("/verifier/checkPlugin")
  fun checkPlugin(@Body body: MultipartBody): Call<ResponseBody>

}