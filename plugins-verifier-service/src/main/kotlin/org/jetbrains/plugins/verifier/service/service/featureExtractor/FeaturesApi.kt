package org.jetbrains.plugins.verifier.service.service.featureExtractor

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FeaturesApi {

  @Multipart
  @POST("/feature/getUpdatesToExtractFeatures")
  fun getUpdatesToExtractFeatures(@Part("userName") userName: RequestBody,
                                  @Part("password") password: RequestBody): Call<List<Int>>

  @Multipart
  @POST("/feature/receiveExtractedFeatures")
  fun sendExtractedFeatures(@Part("extractedFeatures") extractedFeatures: RequestBody,
                            @Part("userName") userName: RequestBody,
                            @Part("password") password: RequestBody): Call<ResponseBody>

}