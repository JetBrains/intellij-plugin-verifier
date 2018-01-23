package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.verification.DependenciesGraphs
import com.jetbrains.plugin.verification.VerificationResults
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning

/**
 * Converts the internal verifier [result] [VerificationResult]
 * to the protocol API version of [result] [VerificationResults.VerificationResult].
 */
fun VerificationResult.prepareVerificationResponse(): VerificationResults.VerificationResult {
  val problems = getCompatibilityProblems()
  val pluginStructureWarnings = getPluginStructureWarnings().map { it.convertPluginStructureWarning() }
  val pluginStructureErrors = (this as? VerificationResult.InvalidPlugin)?.pluginStructureErrors.orEmpty().map { it.convertPluginStructureError() }
  val dependenciesGraph = getDependenciesGraph()?.let { convertDependencyGraph(it) }
  val resultType = convertResultType()
  val compatibilityProblems = problems.map { it.convertCompatibilityProblem() }
  val nonDownloadableReason = (this as? VerificationResult.FailedToDownload)?.reason ?: (this as? VerificationResult.NotFound)?.reason
  return VerificationResults.VerificationResult.newBuilder()
      .setUpdateId((plugin as UpdateInfo).updateId)
      .setIdeVersion(ideVersion.asString())
      .apply { if (dependenciesGraph != null) setDependenciesGraph(dependenciesGraph) }
      .setResultType(resultType)
      .addAllPluginStructureWarnings(pluginStructureWarnings)
      .addAllPluginStructureErrors(pluginStructureErrors)
      .addAllCompatibilityProblems(compatibilityProblems)
      .apply { if (nonDownloadableReason != null) setNonDownloadableReason(nonDownloadableReason) }
      .build()
}

private fun convertDependencyGraph(dependenciesGraph: DependenciesGraph) = DependenciesGraphs.DependenciesGraph.newBuilder()
    .setVerifiedPlugin(convertNode(dependenciesGraph.verifiedPlugin))
    .addAllVertices(dependenciesGraph.vertices.map { convertNode(it) })
    .addAllEdges(dependenciesGraph.edges.map { convertEdge(it) })
    .build()

private fun convertEdge(dependencyEdge: DependencyEdge): DependenciesGraphs.DependenciesGraph.Edge =
    DependenciesGraphs.DependenciesGraph.Edge.newBuilder()
        .setFrom(convertNode(dependencyEdge.from))
        .setTo(convertNode(dependencyEdge.to))
        .setDependency(convertPluginDependency(dependencyEdge.dependency))
        .build()

private fun convertNode(internalNode: DependencyNode): DependenciesGraphs.DependenciesGraph.Node = DependenciesGraphs.DependenciesGraph.Node.newBuilder()
    .setPluginId(internalNode.pluginId)
    .setVersion(internalNode.version)
    .addAllMissingDependencies(internalNode.missingDependencies.map { convertMissingDependency(it) })
    .build()

private fun convertMissingDependency(missingDependency: MissingDependency) = DependenciesGraphs.DependenciesGraph.MissingDependency.newBuilder()
    .setDependency(convertPluginDependency(missingDependency.dependency))
    .setMissingReason(missingDependency.missingReason)
    .build()

private fun convertPluginDependency(dependency: PluginDependency) =
    DependenciesGraphs.DependenciesGraph.Dependency.newBuilder()
        .setDependencyId(dependency.id)
        .setIsModule(dependency.isModule)
        .setIsOptional(dependency.isOptional)
        .build()

private fun VerificationResult.getPluginStructureWarnings() = with(this) {
  when (this) {
    is VerificationResult.OK -> emptySet()
    is VerificationResult.StructureWarnings -> pluginStructureWarnings
    is VerificationResult.MissingDependencies -> pluginStructureWarnings
    is VerificationResult.CompatibilityProblems -> pluginStructureWarnings
    is VerificationResult.InvalidPlugin -> emptySet()
    is VerificationResult.NotFound -> emptySet()
    is VerificationResult.FailedToDownload -> emptySet()
  }
}

private fun VerificationResult.getCompatibilityProblems() = with(this) {
  when (this) {
    is VerificationResult.OK -> emptySet()
    is VerificationResult.StructureWarnings -> emptySet()
    is VerificationResult.MissingDependencies -> compatibilityProblems
    is VerificationResult.CompatibilityProblems -> compatibilityProblems
    is VerificationResult.InvalidPlugin -> emptySet()
    is VerificationResult.NotFound -> emptySet()
    is VerificationResult.FailedToDownload -> emptySet()
  }
}

private fun VerificationResult.getDependenciesGraph() = with(this) {
  when (this) {
    is VerificationResult.OK -> dependenciesGraph
    is VerificationResult.StructureWarnings -> dependenciesGraph
    is VerificationResult.MissingDependencies -> dependenciesGraph
    is VerificationResult.CompatibilityProblems -> dependenciesGraph
    is VerificationResult.InvalidPlugin -> null
    is VerificationResult.NotFound -> null
    is VerificationResult.FailedToDownload -> null
  }
}

private fun CompatibilityProblem.convertCompatibilityProblem() =
    VerificationResults.CompatibilityProblem.newBuilder()
        .setShortDescription(shortDescription)
        .setFullDescription(fullDescription)
        .build()

private fun PluginStructureWarning.convertPluginStructureWarning() =
    VerificationResults.PluginStructureWarning.newBuilder()
        .setMessage(message)
        .build()

private fun PluginStructureError.convertPluginStructureError() =
    VerificationResults.PluginStructureError.newBuilder()
        .setMessage(message)
        .build()

private fun VerificationResult.convertResultType() = when (this) {
  is VerificationResult.OK -> VerificationResults.VerificationResult.ResultType.OK
  is VerificationResult.StructureWarnings -> VerificationResults.VerificationResult.ResultType.STRUCTURE_WARNINGS
  is VerificationResult.CompatibilityProblems -> VerificationResults.VerificationResult.ResultType.COMPATIBILITY_PROBLEMS
  is VerificationResult.MissingDependencies -> VerificationResults.VerificationResult.ResultType.MISSING_DEPENDENCIES
  is VerificationResult.InvalidPlugin -> VerificationResults.VerificationResult.ResultType.INVALID_PLUGIN
  is VerificationResult.NotFound -> VerificationResults.VerificationResult.ResultType.NON_DOWNLOADABLE
  is VerificationResult.FailedToDownload -> VerificationResults.VerificationResult.ResultType.NON_DOWNLOADABLE
}
