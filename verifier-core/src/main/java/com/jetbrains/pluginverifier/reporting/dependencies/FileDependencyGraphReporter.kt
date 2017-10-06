package com.jetbrains.pluginverifier.reporting.dependencies

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import java.io.File

class FileDependencyGraphReporter(file: File) : FileReporter<DependenciesGraph>(file), DependencyGraphReporter {
  override fun reportDependenciesGraph(dependenciesGraph: DependenciesGraph) {
    super<FileReporter>.report(dependenciesGraph)
  }

  override fun report(t: DependenciesGraph) = reportDependenciesGraph(t)

}