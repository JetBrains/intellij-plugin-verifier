package org.jetbrains.plugins.verifier.service.results

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.configurations.Results
import com.jetbrains.pluginverifier.output.StreamVPrinter
import com.jetbrains.pluginverifier.report.CheckIdeReport
import java.io.PrintStream

/**
 * @author Sergey Patrikeev
 */
data class CheckPluginAgainstSinceUntilBuildsResults(@SerializedName("results") val vResults: VResults) : Results {
  fun printResults(stream: PrintStream) {
    StreamVPrinter(stream).printResults(vResults)
  }
}

data class CheckTrunkApiResults(@SerializedName("majorReport") val majorReport: CheckIdeReport,
                                @SerializedName("majorPlugins") val majorPlugins: BundledPlugins,
                                @SerializedName("currentReport") val currentReport: CheckIdeReport,
                                @SerializedName("currentPlugins") val currentPlugins: BundledPlugins) : Results {
}

data class BundledPlugins(@SerializedName("pluginIds") val pluginIds: List<String>,
                          @SerializedName("moduleIds") val moduleIds: List<String>)