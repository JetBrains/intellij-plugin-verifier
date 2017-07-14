package org.jetbrains.plugins.verifier.service.service.verifier

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VerificationApi {

  @POST("/verification/getUpdatesToCheck")
  @Multipart
  fun getUpdatesToCheck(@Part("availableIde") availableIde: RequestBody,
                        @Part("userName") userName: RequestBody,
                        @Part("password") password: RequestBody): Call<List<Int>>

  @POST("/verification/receiveUpdateCheckResult")
  @Multipart
  fun sendUpdateCheckResult(@Part("checkResults") checkResult: RequestBody,
                            @Part("userName") userName: RequestBody,
                            @Part("password") password: RequestBody): Call<ResponseBody>

}