package org.jetbrains.plugins.verifier.service.api

import com.google.gson.annotations.SerializedName
import com.intellij.structure.plugin.PluginDependency
import com.intellij.structure.problems.PluginProblem
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.warnings.Warning
import org.jetbrains.plugins.verifier.service.api.ApiVerificationVerdict.VerdictType.*
import org.jetbrains.plugins.verifier.service.service.ServerInstance
import org.jetbrains.plugins.verifier.service.service.verifier.CheckRangeCompatibilityResult

fun prepareVerificationResponse(compatibilityResult: CheckRangeCompatibilityResult): String {
  val apiResultType = convertToApiResultType(compatibilityResult)
  val apiResults = compatibilityResult.verificationResults?.map { convertVerifierResult(it) }
  val invalidPluginProblems = compatibilityResult.invalidPluginProblems?.map { convertInvalidProblem(it) }
  val apiResult = ApiCheckRangeCompatibilityResult(compatibilityResult.plugin.updateInfo!!.updateId, apiResultType, apiResults, invalidPluginProblems, compatibilityResult.nonDownloadableReason)
  return ServerInstance.GSON.toJson(apiResult)
}

private fun convertInvalidProblem(pluginProblem: PluginProblem): ApiInvalidPluginProblem = when (pluginProblem.level) {
  PluginProblem.Level.ERROR -> ApiInvalidPluginProblem(pluginProblem.message, ApiInvalidPluginProblem.Level.ERROR)
  PluginProblem.Level.WARNING -> ApiInvalidPluginProblem(pluginProblem.message, ApiInvalidPluginProblem.Level.WARNING)
}

private data class ApiDependenciesGraph(@SerializedName("start") var start: Node,
                                        @SerializedName("vertices") var vertices: List<Node>,
                                        @SerializedName("edges") var edges: List<Edge>) {


  data class Dependency(@SerializedName("dependencyId") var dependencyId: String,
                        @SerializedName("isOptional") var isOptional: Boolean,
                        @SerializedName("isModule") var isModule: Boolean)

  data class MissingDependency(@SerializedName("dependency") var dependency: Dependency,
                               @SerializedName("missingReason") var missingReason: String)

  data class Node(@SerializedName("pluginId") var pluginId: String,
                  @SerializedName("version") var version: String,
                  @SerializedName("missingDependencies") var missingDependencies: List<MissingDependency>)

  data class Edge(@SerializedName("from") var fromNode: Node,
                  @SerializedName("to") var to: Node,
                  @SerializedName("dependency") var dependency: Dependency)


}

private data class ApiVerificationResult(@SerializedName("ideVersion") val ideVersion: String,
                                         @SerializedName("verdict") val verdict: ApiVerificationVerdict)

private data class ApiVerificationVerdict(@SerializedName("verdictType") val verdictType: VerdictType,
                                          @SerializedName("dependenciesGraph") val dependenciesGraph: ApiDependenciesGraph,
                                          @SerializedName("missingDependencies") val missingDependencies: List<ApiDependenciesGraph.MissingDependency> = emptyList(),
                                          @SerializedName("warnings") val warnings: List<ApiWarning> = emptyList(),
                                          @SerializedName("problems") val problems: List<ApiProblem> = emptyList()) {
  enum class VerdictType {
    OK,
    WARNINGS,
    MISSING_DEPENDENCIES,
    PROBLEMS
  }

}

private data class ApiWarning(@SerializedName("message") val message: String)

private data class ApiProblem(@SerializedName("description") val description: String)

private data class ApiInvalidPluginProblem(@SerializedName("message") val message: String,
                                           @SerializedName("level") val level: Level) {
  enum class Level {
    WARNING,
    ERROR
  }
}

private data class ApiCheckRangeCompatibilityResult(@SerializedName("updateId") val updateId: Int,
                                                    @SerializedName("type") val resultType: ResultType,
                                                    @SerializedName("verificationResults") val verificationResults: List<ApiVerificationResult>?,
                                                    @SerializedName("invalidPluginProblems") val invalidPluginProblems: List<ApiInvalidPluginProblem>?,
                                                    @SerializedName("nonDownloadableReason") val nonDownloadableReason: String?) {
  enum class ResultType {
    NON_DOWNLOADABLE,
    NO_COMPATIBLE_IDES,
    INVALID_PLUGIN,
    VERIFICATION_DONE
  }

}

private fun convertDependencyGraph(dependenciesGraph: DependenciesGraph): ApiDependenciesGraph = ApiDependenciesGraph(
    convertNode(dependenciesGraph.start),
    dependenciesGraph.vertices.map { convertNode(it) },
    dependenciesGraph.edges.map { ApiDependenciesGraph.Edge(convertNode(it.from), convertNode(it.to), convertPluginDependency(it.dependency)) }
)

private fun convertNode(internalNode: DependencyNode): ApiDependenciesGraph.Node = ApiDependenciesGraph.Node(
    internalNode.id,
    internalNode.version,
    internalNode.missingDependencies.map { convertMissingDependency(it) }
)

private fun convertMissingDependency(missingDependency: MissingDependency) = ApiDependenciesGraph.MissingDependency(
    convertPluginDependency(missingDependency.dependency), missingDependency.missingReason
)

private fun convertPluginDependency(dependency: PluginDependency) = ApiDependenciesGraph.Dependency(dependency.id, dependency.isOptional, dependency.isModule)

private fun convertVerifierResult(result: Result): ApiVerificationResult =
    ApiVerificationResult(result.ideVersion.asString(), convertVerdict(result.verdict))

private fun convertVerdict(verdict: Verdict): ApiVerificationVerdict {
  val dependenciesGraph = when (verdict) {
    is Verdict.OK -> verdict.dependenciesGraph
    is Verdict.Warnings -> verdict.dependenciesGraph
    is Verdict.MissingDependencies -> verdict.dependenciesGraph
    is Verdict.Problems -> verdict.dependenciesGraph
    is Verdict.Bad, is Verdict.NotFound -> throw RuntimeException()
  }
  val convertedGraph = convertDependencyGraph(dependenciesGraph)
  return when (verdict) {
    is Verdict.OK -> ApiVerificationVerdict(OK, convertedGraph)
    is Verdict.Warnings -> ApiVerificationVerdict(WARNINGS, convertedGraph, warnings = convertWarnings(verdict.warnings))
    is Verdict.MissingDependencies -> ApiVerificationVerdict(MISSING_DEPENDENCIES, convertedGraph, convertMissingDependencies(verdict), convertWarnings(verdict.warnings), convertProblems(verdict.problems))
    is Verdict.Problems -> ApiVerificationVerdict(PROBLEMS, convertedGraph, emptyList(), convertWarnings(verdict.warnings), convertProblems(verdict.problems))
    is Verdict.Bad, is Verdict.NotFound -> throw RuntimeException()
  }
}

private fun convertMissingDependencies(verdict: Verdict.MissingDependencies) = verdict.missingDependencies.map { convertMissingDependency(it) }

private fun convertProblems(problems: Set<Problem>): List<ApiProblem> = problems.map { ApiProblem(it.getFullDescription().toString()) }

private fun convertWarnings(warnings: Set<Warning>): List<ApiWarning> = warnings.map { ApiWarning(it.message) }

private fun convertToApiResultType(compatibilityResult: CheckRangeCompatibilityResult): ApiCheckRangeCompatibilityResult.ResultType = when (compatibilityResult.resultType) {
  CheckRangeCompatibilityResult.ResultType.NON_DOWNLOADABLE -> ApiCheckRangeCompatibilityResult.ResultType.NON_DOWNLOADABLE
  CheckRangeCompatibilityResult.ResultType.NO_COMPATIBLE_IDES -> ApiCheckRangeCompatibilityResult.ResultType.NO_COMPATIBLE_IDES
  CheckRangeCompatibilityResult.ResultType.INVALID_PLUGIN -> ApiCheckRangeCompatibilityResult.ResultType.INVALID_PLUGIN
  CheckRangeCompatibilityResult.ResultType.VERIFICATION_DONE -> ApiCheckRangeCompatibilityResult.ResultType.VERIFICATION_DONE
}
