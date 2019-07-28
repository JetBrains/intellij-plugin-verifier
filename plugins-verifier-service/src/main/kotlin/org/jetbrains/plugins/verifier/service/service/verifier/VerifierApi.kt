package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.verification.DependenciesGraphs
import com.jetbrains.plugin.verification.VerificationResults
import com.jetbrains.pluginverifier.*
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.location.*
import com.jetbrains.pluginverifier.results.presentation.*
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.deprecated.DeprecationInfo
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.PluginStructureError

/**
 * Converts the internal verifier results to the protocol API results.
 */
fun PluginVerificationResult.prepareVerificationResponse(updateInfo: UpdateInfo): VerificationResults.VerificationResult {
  val problems = getCompatibilityProblems()
  val warnings = getCompatibilityWarnings().map { it.convertCompatibilityWarning() }
  val pluginStructureErrors = (this as? PluginVerificationResult.InvalidPlugin)?.pluginStructureErrors.orEmpty().map { it.convertPluginStructureError() }
  val deprecatedUsages = getDeprecatedUsages().map { it.convertDeprecatedApiUsage() }
  val experimentalApiUsages = getExperimentalApiUsages().map { it.convertExperimentalApiUsage() }
  val dependenciesGraph = getDependenciesGraph()?.let { convertDependencyGraph(it) }
  val resultType = convertResultType()
  val compatibilityProblems = problems.map { it.convertCompatibilityProblem() }
  val nonDownloadableReason = (this as? PluginVerificationResult.FailedToDownload)?.failedToDownloadReason
      ?: (this as? PluginVerificationResult.NotFound)?.notFoundReason
  return VerificationResults.VerificationResult.newBuilder()
      .setUpdateId(updateInfo.updateId)
      .setIdeVersion((verificationTarget as PluginVerificationTarget.IDE).ideVersion.asString())
      .apply { if (dependenciesGraph != null) setDependenciesGraph(dependenciesGraph) }
      .setResultType(resultType)
      .addAllPluginStructureWarnings(warnings)
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

private fun PluginVerificationResult.getCompatibilityWarnings(): Set<CompatibilityWarning> =
    (this as? PluginVerificationResult.Verified)?.compatibilityWarnings ?: emptySet()

private fun PluginVerificationResult.getDeprecatedUsages(): Set<DeprecatedApiUsage> =
    (this as? PluginVerificationResult.Verified)?.deprecatedUsages ?: emptySet()

private fun PluginVerificationResult.getExperimentalApiUsages(): Set<ExperimentalApiUsage> =
    (this as? PluginVerificationResult.Verified)?.experimentalApiUsages ?: emptySet()

private fun PluginVerificationResult.getCompatibilityProblems(): Set<CompatibilityProblem> =
    (this as? PluginVerificationResult.Verified)?.compatibilityProblems ?: emptySet()

private fun PluginVerificationResult.getDependenciesGraph() =
    (this as? PluginVerificationResult.Verified)?.dependenciesGraph

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

private fun CompatibilityWarning.convertCompatibilityWarning() =
    VerificationResults.PluginStructureWarning.newBuilder()
        .setMessage(message)
        .build()

private fun PluginStructureError.convertPluginStructureError() =
    VerificationResults.PluginStructureError.newBuilder()
        .setMessage(message)
        .build()

private fun PluginVerificationResult.convertResultType() = when (this) {
  is PluginVerificationResult.Verified -> when {
    hasCompatibilityWarnings -> VerificationResults.VerificationResult.ResultType.STRUCTURE_WARNINGS
    hasCompatibilityProblems -> VerificationResults.VerificationResult.ResultType.COMPATIBILITY_PROBLEMS
    hasDirectMissingDependencies -> VerificationResults.VerificationResult.ResultType.MISSING_DEPENDENCIES
    else -> VerificationResults.VerificationResult.ResultType.OK
  }
  is PluginVerificationResult.InvalidPlugin -> VerificationResults.VerificationResult.ResultType.INVALID_PLUGIN
  is PluginVerificationResult.NotFound -> VerificationResults.VerificationResult.ResultType.NON_DOWNLOADABLE
  is PluginVerificationResult.FailedToDownload -> VerificationResults.VerificationResult.ResultType.NON_DOWNLOADABLE
}
