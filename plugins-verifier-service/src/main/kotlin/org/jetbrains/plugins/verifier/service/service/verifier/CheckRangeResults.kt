package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.gson.annotations.SerializedName
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Result

/**
 * @author Sergey Patrikeev
 */
data class CheckRangeResults(@SerializedName("plugin") val plugin: PluginInfo,
                             @SerializedName("type") val resultType: ResultType,
                             @SerializedName("checkedIdeList") val checkedIdeList: List<IdeVersion>,
                             @SerializedName("results") val result: List<Result>,
                             @SerializedName("protocolVersion") val protocolVersion: Int) {
  enum class ResultType {
    NOT_FOUND,
    NO_COMPATIBLE_IDES,
    BAD_PLUGIN,
    CHECKED
  }

}