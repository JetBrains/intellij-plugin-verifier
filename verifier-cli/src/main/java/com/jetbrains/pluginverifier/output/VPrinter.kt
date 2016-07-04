package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.api.VResults

/**
 * @author Sergey Patrikeev
 */
interface VPrinter {
  fun printResults(results: VResults)
}