package com.jetbrains.pluginverifier.repository

import com.google.gson.annotations.SerializedName

data class UpdateInfo(@SerializedName("pluginId") val pluginId: String,
                      @SerializedName("pluginName") val pluginName: String,
                      @SerializedName("version", alternate = arrayOf("pluginVersion")) val version: String,
                      @SerializedName("updateId") val updateId: Int,
                      @SerializedName("vendor") val vendor: String?,
                      @SerializedName("since") val since: String?,
                      @SerializedName("until") val until: String?) {

  override fun toString(): String = "$pluginId:$version (#$updateId)"
}