package com.jetbrains.pluginverifier.reporting.dependencies

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import org.slf4j.Logger

class LogDependencyGraphReporter(logger: Logger) : LogReporter<DependenciesGraph>(logger), DependencyGraphReporter {
  override fun reportDependenciesGraph(dependenciesGraph: DependenciesGraph) {
    super<LogReporter>.report(dependenciesGraph)
  }

  override fun report(t: DependenciesGraph) {
    reportDependenciesGraph(t)
  }
}