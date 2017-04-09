package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict

/**
 * @author Sergey Patrikeev
 */
data class CheckRangeResults(@SerializedName("plugin") val plugin: PluginInfo,
                             @SerializedName("type") val resultType: ResultType,
                             @SerializedName("badPlugin") val badPlugin: Verdict.Bad?,
                             @SerializedName("checkedIdeList") val checkedIdeList: List<IdeVersion>?,
                             @SerializedName("results") val result: List<Result>) : ConfigurationResults {

  enum class ResultType {
    NOT_FOUND,
    NO_COMPATIBLE_IDES,
    BAD_PLUGIN,
    CHECKED
  }

}