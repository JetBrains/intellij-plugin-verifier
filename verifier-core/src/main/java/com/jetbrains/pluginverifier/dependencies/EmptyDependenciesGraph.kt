package com.jetbrains.pluginverifier.dependencies

private val emptyDependencyNode = DependencyNode("", "", emptyList())

/**
 * The [DependenciesGraph] used as a placeholder in the [com.jetbrains.pluginverifier.results.VerificationResult.dependenciesGraph]
 * for the results types that don't contain the graph.
 */
val emptyDependenciesGraph = DependenciesGraph(emptyDependencyNode, emptyList(), emptyList())