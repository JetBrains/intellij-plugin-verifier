package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.verification.DependenciesGraphs
import com.jetbrains.plugin.verification.VerificationResults
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.deprecated.DeprecationInfo
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.location.*
import com.jetbrains.pluginverifier.results.presentation.*
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning

/**
 * Converts the internal verifier [result] [VerificationResult]
 * to the protocol API version of [result] [VerificationResults.VerificationResult].
 */
fun VerificationResult.prepareVerificationResponse(updateInfo: UpdateInfo): VerificationResults.VerificationResult {
  val problems = getCompatibilityProblems()
  val pluginStructureWarnings = getPluginStructureWarnings().map { it.convertPluginStructureWarning() }
  val pluginStructureErrors = (this as? VerificationResult.InvalidPlugin)?.pluginStructureErrors.orEmpty().map { it.convertPluginStructureError() }
  val deprecatedUsages = getDeprecatedUsages().map { it.convertDeprecatedApiUsage() }
  val experimentalApiUsages = getExperimentalApiUsages().map { it.convertExperimentalApiUsage() }
  val dependenciesGraph = getDependenciesGraph()?.let { convertDependencyGraph(it) }
  val resultType = convertResultType()
  val compatibilityProblems = problems.map { it.convertCompatibilityProblem() }
  val nonDownloadableReason = (this as? VerificationResult.FailedToDownload)?.failedToDownloadReason
      ?: (this as? VerificationResult.NotFound)?.notFoundReason
  return VerificationResults.VerificationResult.newBuilder()
      .setUpdateId(updateInfo.updateId)
      .setIdeVersion((verificationTarget as VerificationTarget.Ide).ideVersion.asString())
      .apply { if (dependenciesGraph != null) setDependenciesGraph(dependenciesGraph) }
      .setResultType(resultType)
      .addAllPluginStructureWarnings(pluginStructureWarnings)
      .addAllPluginStructureErrors(pluginStructureErrors)
      .addAllDeprecatedUsages(deprecatedUsages)
      .addAllExperimentalApiUsages(experimentalApiUsages)
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

private fun VerificationResult.getDeprecatedUsages() = with(this) {
  when (this) {
    is VerificationResult.OK -> deprecatedUsages
    is VerificationResult.StructureWarnings -> deprecatedUsages
    is VerificationResult.MissingDependencies -> deprecatedUsages
    is VerificationResult.CompatibilityProblems -> deprecatedUsages
    is VerificationResult.InvalidPlugin -> emptySet()
    is VerificationResult.NotFound -> emptySet()
    is VerificationResult.FailedToDownload -> emptySet()
  }
}

private fun VerificationResult.getExperimentalApiUsages() = with(this) {
  when (this) {
    is VerificationResult.OK -> experimentalApiUsages
    is VerificationResult.StructureWarnings -> experimentalApiUsages
    is VerificationResult.MissingDependencies -> experimentalApiUsages
    is VerificationResult.CompatibilityProblems -> experimentalApiUsages
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
        .setProblemType(problemType)
        .build()

private fun DeprecatedApiUsage.convertDeprecatedApiUsage() =
    VerificationResults.DeprecatedApiUsage.newBuilder()
        .setShortDescription(shortDescription)
        .setFullDescription(fullDescription)
        .setDeprecatedElement(apiElement.fullyQualifiedLocation())
        .setUsageLocation(usageLocation.presentableUsageLocation())
        .setElementType(apiElement.elementType.convertElementType())
        .setDeprecationInfo(deprecationInfo.convertDeprecationInfo())
        .build()

private fun DeprecationInfo.convertDeprecationInfo() =
    VerificationResults.DeprecatedApiUsage.DeprecationInfo.newBuilder()
        .setForRemoval(forRemoval)
        .setUntilVersion(untilVersion ?: "")
        .build()

private fun ExperimentalApiUsage.convertExperimentalApiUsage() =
    VerificationResults.ExperimentalApiUsage.newBuilder()
        .setShortDescription(shortDescription)
        .setFullDescription(fullDescription)
        .setApiElement(apiElement.fullyQualifiedLocation())
        .setUsageLocation(usageLocation.presentableUsageLocation())
        .setApiElementType(apiElement.elementType.convertElementType())
        .build()

private fun ElementType.convertElementType() = when (this) {
  ElementType.CLASS -> VerificationResults.ElementType.CLASS
  ElementType.INTERFACE -> VerificationResults.ElementType.INTERFACE
  ElementType.ENUM -> VerificationResults.ElementType.ENUM
  ElementType.ANNOTATION -> VerificationResults.ElementType.ANNOTATION
  ElementType.METHOD -> VerificationResults.ElementType.METHOD
  ElementType.FIELD -> VerificationResults.ElementType.FIELD
  ElementType.CONSTRUCTOR -> VerificationResults.ElementType.CONSTRUCTOR
}

private fun Location.fullyQualifiedLocation(): String = when (this) {
  is ClassLocation -> formatClassLocation(
      ClassOption.FULL_NAME,
      ClassGenericsSignatureOption.NO_GENERICS
  )
  is MethodLocation -> formatMethodLocation(
      HostClassOption.FULL_HOST_NAME,
      MethodParameterTypeOption.FULL_PARAM_CLASS_NAME,
      MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME,
      MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
  )
  is FieldLocation -> formatFieldLocation(
      HostClassOption.FULL_HOST_NAME,
      FieldTypeOption.FULL_TYPE
  )
}

private fun Location.presentableUsageLocation(): String = when (this) {
  is ClassLocation -> formatClassLocation(
      ClassOption.FULL_NAME,
      ClassGenericsSignatureOption.NO_GENERICS
  )
  is MethodLocation -> formatMethodLocation(
      HostClassOption.FULL_HOST_NAME,
      MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME,
      MethodReturnTypeOption.NO_RETURN_TYPE,
      MethodParameterNameOption.NO_PARAMETER_NAMES
  )
  is FieldLocation -> formatFieldLocation(
      HostClassOption.FULL_HOST_NAME,
      FieldTypeOption.NO_TYPE
  )
}

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
