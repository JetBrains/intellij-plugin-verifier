package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults

/**
 * @author Sergey Patrikeev
 */
data class CheckRangeResults(@SerializedName("plugin") val plugin: PluginDescriptor,
                             @SerializedName("type") val resultType: ResultType,
                             @SerializedName("badPlugin") val badPlugin: VResult.BadPlugin?,
                             @SerializedName("checkedIdeList") val checkedIdeList: List<IdeDescriptor>?,
                             @SerializedName("results") val vResults: VResults?) : Results {

  enum class ResultType {
    NOT_FOUND,
    NO_COMPATIBLE_IDES,
    BAD_PLUGIN,
    CHECKED
  }

}