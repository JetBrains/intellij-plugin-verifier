package org.jetbrains.plugins.verifier.service.client

import com.github.salomonbrys.kotson.fromJson
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.persistence.GsonHolder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.api.TaskStatus
import org.jetbrains.plugins.verifier.service.util.executeSuccessfully
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

val CLIENT_SIDE_VERSION: String = "1.0"

private val LOG: Logger = LoggerFactory.getLogger(VerifierService::class.java)

private val REQUEST_PERIOD: Long = 5000

fun parseTaskId(response: Response<ResponseBody>): TaskId = parseResponse(response)

inline fun <reified T : Any> parseResponse(response: Response<ResponseBody>): T {
  if (response.isSuccessful) {
    return GsonHolder.GSON.fromJson(response.body().string())
  }
  throw IllegalStateException("The response status code is ${response.code()}: ${response.errorBody().string()}")
}

internal inline fun <reified T : Any> waitCompletion(service: VerifierService, taskId: TaskId): T {
  val resultType: Type = ParameterizedTypeImpl.make(Result::class.java, arrayOf(T::class.java), null)

  var progress: Double = 0.0
  while (true) {
    val response: Response<ResponseBody> = service.taskResultsService.getTaskResult(taskId).executeSuccessfully()
    val json = response.body().string()

    val result: Result<T> = GsonHolder.GSON.fromJson(json, resultType)

    val taskStatus = result.taskStatus

    if (taskStatus.progress - progress > 0.05) {
      progress = taskStatus.progress
      LOG.info("The task progress ${"%.1f".format(progress * 100)}%: ${taskStatus.progressText} (${taskStatus.elapsedTime() / 1000} seconds)")
    }

    val exhaustedWhen = when (taskStatus.state) {
      TaskStatus.State.WAITING, TaskStatus.State.RUNNING -> {
        LOG.debug("The task is not finished yet.")
        Thread.sleep(REQUEST_PERIOD)
      }
      TaskStatus.State.SUCCESS -> {
        LOG.info("The task $taskId is completed successfully.")
        return result.result!!
      }
      TaskStatus.State.ERROR -> {
        throw RuntimeException("The task $taskId is completed with error: ${result.errorMessage!!}")
      }
      TaskStatus.State.CANCELLED -> {
        throw RuntimeException("The task $taskId was cancelled")
      }
    }
  }
}


/**
 * @author Sergey Patrikeev
 */
class VerifierService(val host: String) {

  val statusService: StatusApi = Retrofit.Builder()
      .baseUrl(host)
      .addConverterFactory(GsonConverterFactory.create(GsonHolder.GSON))
      .client(makeClient())
      .build()
      .create(StatusApi::class.java)

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

interface StatusApi {
  @GET("/status/supportedClients")
  fun getSupportedClients(): Call<List<String>>
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
   * The result is of type [com.jetbrains.pluginverifier.configurations.CheckRangeResults]
   */
  @Multipart
  @POST("/verifier/checkPluginRange")
  fun checkPluginRange(@Part pluginFile: MultipartBody.Part, @Part parameters: MultipartBody.Part): Call<ResponseBody>

  /**
   * The check-plugin command.
   * The result is of type [com.jetbrains.pluginverifier.configurations.CheckPluginResults]
   */
  @POST("/verifier/checkPlugin")
  fun checkPlugin(@Body body: MultipartBody): Call<ResponseBody>

}