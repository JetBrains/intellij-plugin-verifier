package org.jetbrains.plugins.verifier.service.results

import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.output.StreamVPrinter

/**
 * @author Sergey Patrikeev
 */
data class CheckPluginAgainstSinceUntilBuildsResults(val vResults: VResults) {
  fun processResults() {
    StreamVPrinter(System.out).printResults(vResults)
  }
}