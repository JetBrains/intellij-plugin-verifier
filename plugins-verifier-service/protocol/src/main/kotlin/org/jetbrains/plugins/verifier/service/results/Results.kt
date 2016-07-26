package org.jetbrains.plugins.verifier.service.results

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.output.StreamVPrinter
import com.jetbrains.pluginverifier.report.CheckIdeReport
import java.io.PrintStream

/**
 * @author Sergey Patrikeev
 */
data class CheckPluginAgainstSinceUntilBuildsResults(@SerializedName("results") val vResults: VResults) {
  fun printResults(stream: PrintStream) {
    StreamVPrinter(stream).printResults(vResults)
  }
}

data class CheckTrunkApiResults(@SerializedName("majorReport") val majorReport: CheckIdeReport,
                                @SerializedName("currentReport") val currentReport: CheckIdeReport)