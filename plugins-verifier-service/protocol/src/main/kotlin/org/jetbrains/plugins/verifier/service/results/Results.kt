package org.jetbrains.plugins.verifier.service.results

import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.output.StreamVPrinter
import java.io.PrintStream

/**
 * @author Sergey Patrikeev
 */
data class CheckPluginAgainstSinceUntilBuildsResults(val vResults: VResults) {
  fun printResults(stream: PrintStream) {
    StreamVPrinter(stream).printResults(vResults)
  }
}