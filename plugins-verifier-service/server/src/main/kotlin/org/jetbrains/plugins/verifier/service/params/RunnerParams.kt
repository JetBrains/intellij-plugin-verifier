package org.jetbrains.plugins.verifier.service.params

import com.google.gson.annotations.SerializedName
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.tasks.PluginIdAndVersion

enum class JdkVersion {
  JAVA_6_ORACLE,
  JAVA_7_ORACLE,
  JAVA_8_ORACLE
}

data class CheckIdeRunnerParams(@SerializedName("jdkVersion") val jdkVersion: JdkVersion,
                                @SerializedName("checkAllBuilds") val checkAllBuilds: List<String>,
                                @SerializedName("checkLastBuilds") val checkLastBuilds: List<String>,
                                @SerializedName("excludedPlugins") val excludedPlugins: List<PluginIdAndVersion>,
                                @SerializedName("pluginIdsToCheckExistingBuilds") val pluginIdsToCheckExistingBuilds: List<String>,
                                @SerializedName("actualIdeVersion") val actualIdeVersion: IdeVersion? = null)

data class CheckRangeRunnerParams(@SerializedName("jdkVersion") val jdkVersion: JdkVersion)

data class CheckPluginRunnerParams(@SerializedName("jdkVersion") val jdkVersion: JdkVersion)