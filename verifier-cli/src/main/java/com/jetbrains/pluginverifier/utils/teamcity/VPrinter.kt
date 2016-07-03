package com.jetbrains.pluginverifier.utils.teamcity

import com.jetbrains.pluginverifier.api.VResults

/**
 * @author Sergey Patrikeev
 */
interface VPrinter {
  fun printResults(results: VResults)
}