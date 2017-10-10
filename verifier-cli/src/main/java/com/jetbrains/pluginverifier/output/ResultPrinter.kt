package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.results.Result

/**
 * @author Sergey Patrikeev
 */
interface ResultPrinter {
  fun printResults(results: List<Result>)
}