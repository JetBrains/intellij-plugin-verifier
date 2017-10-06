package com.jetbrains.pluginverifier.reporting.dependencies

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.reporting.Reporter

/**
 * @author Sergey Patrikeev
 */
interface DependencyGraphReporter : Reporter<DependenciesGraph> {

  override fun report(t: DependenciesGraph) = reportDependenciesGraph(t)

  fun reportDependenciesGraph(dependenciesGraph: DependenciesGraph)
}