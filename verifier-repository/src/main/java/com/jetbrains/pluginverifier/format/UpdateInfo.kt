package com.jetbrains.pluginverifier.format

import com.google.gson.annotations.SerializedName

data class UpdateInfo(@SerializedName("pluginId") val pluginId: String,
                      @SerializedName("pluginName") val pluginName: String,
                      @SerializedName("version") val version: String,
                      @SerializedName("updateId") val updateId: Int) {

  override fun toString(): String = "$pluginId:$version (#$updateId)"
}