package org.jetbrains.plugins.verifier.service.results

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.configurations.Results
import com.jetbrains.pluginverifier.output.StreamVPrinter
import java.io.PrintStream

/**
 * @author Sergey Patrikeev
 */
data class CheckPluginAgainstSinceUntilBuildsResults(@SerializedName("results") val vResults: VResults) : Results {
  fun printResults(stream: PrintStream) {
    StreamVPrinter(stream).printResults(vResults)
  }
}