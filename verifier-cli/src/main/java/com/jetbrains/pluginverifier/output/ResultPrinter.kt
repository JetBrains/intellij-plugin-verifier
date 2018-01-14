package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.results.Result

interface ResultPrinter {
  fun printResults(results: List<Result>)
}