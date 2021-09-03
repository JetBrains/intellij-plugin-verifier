package org.jetbrains.plugins.verifier.service.service.sarif

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import org.jetbrains.plugins.verifier.service.service.verifier.DependenciesGraphDto
import org.jetbrains.plugins.verifier.service.service.verifier.DynamicPluginStatusDto
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultTypeDto
import java.util.*

/**
 * SARIF format for Plugin Verifier Tool
 * @param version - format version
 * @param runs - tools to run inspections. It always contains one element.
 */
data class PluginVerificationResultSARIF(
  val version: String = "2.1.0",
  val runs: List<Runner>,
)

data class Runner(
  val tool: Tool,
  val results: List<InspectionResult>,
  val automationDetails: AutomationDetails,
  val language: String = "en-US",
  val invocations: List<InvocationStatus>,
  val versionControlProvenance: List<VersionControlProvenance>,
  val properties: PluginVerifierPropertiesBag,
)

data class PluginVerifierPropertiesBag(
  val ideVersion: String,
  val javaVersion: String,
  val resultType: VerificationResultTypeDto,
  val verdict: String,
  val dependenciesGraph: DependenciesGraphDto?,
  val dynamicPluginStatus: DynamicPluginStatusDto?,
)

/**
 * Objects, which contains zero or one object corresponding to the vcs repository in project root.
 * Currently, supported VCS list: Git.
 * @param repositoryUri - the repository checkout URL
 */
data class VersionControlProvenance(
  val repositoryUri: String,
  val properties: VersionControlProvenanceProperties = VersionControlProvenanceProperties(),
)

data class VersionControlProvenanceProperties(
  val vcsType: String = "Git",
  val tags: List<String> = listOf("vcsType")
)

/**
 * @param guid - a unique report ID.
 * @param id   - a user-defined string, should be unique for the report.
 */
data class AutomationDetails(
  val id: String,
  val guid: String = UUID.randomUUID().toString(),
)

/**
 * @param exitCodeDescription - the description of the exit code for non-zero values.
 * @param executionSuccessful - if exitCode is 0 or 255
 * @param exitCode - tool exit code.
 * 0   - indicates successful execution.
 * 1   - indicates any internal error.
 * 255 - indicates successful execution, but the exit code is non-zero due to failThreshold property.
 */
data class InvocationStatus(
  val exitCode: Int,
  val exitCodeDescription: String,
  val executionSuccessful: Boolean,
)

data class Tool(val driver: Driver)

/**
 * @param name - name of the service
 * @param rules - all the errors from service. Subclasses of [CompatibilityProblem].
 */
data class Driver(
  val name: String = "Intellij Plugin Verifier",
  val rules: List<Rule>,
)

/**
 * Based on [CompatibilityProblem]
 * @param id - class name
 * @param shortDescription - [CompatibilityProblem.problemType]
 * @param fullDescription - [CompatibilityProblem.problemType]
 */
data class Rule(
  val id: String,
  val shortDescription: Message,
  val fullDescription: Message,
  val defaultConfiguration: RuleConfiguration,
)

data class RuleConfiguration(
  val level: String,
  val parameters: RuleParameters,
)

data class RuleParameters(
  val ideaSeverity: SeverityIdea,
  val tags: List<String> = listOf("ideaSeverity")
)

enum class SeverityIdea {
  ERROR,
  WARNING
}

/**
 * @param ruleId - [Rule.id]
 * @param kind - always fail
 * @param level - The SARIF severity values could be one of the following strings: error, warning, note
 * @param locations - always one element
 */
data class InspectionResult(
  val ruleId: String,
  val kind: String = "fail",
  val level: String,
  val message: Message,
  val locations: List<Location>,
)

enum class SeverityValue(val id: String) {
  NONE("none"),
  INFO("info"),
  ERROR("error"),
  WARNING("warning")
}

data class Location(
  val physicalLocation: PhysicalLocation,
)

/**
 * @param artifactLocation - location to an artifact.
 * @param region - object, which is a part of the artifact's location containing the text that should be highlighted as a reason of the current result.
 * @param contextRegion - object, which is a part of the artifact location's surrounding region. Typically, two rows above and under region. Used for problems comparisons in baseline.
 */
data class PhysicalLocation(
  val artifactLocation: ArtifactLocation,
  val region: RegionLocation,
  val contextRegion: RegionLocation,
)

data class RegionLocation(
  val startLine: Int,
  val startColumn: Int,
  val charLength: Int,
  val charOffset: Int,
  val snippet: Message,
  val sourceLanguage: String,
)

/**
 * @param uri - the path relative to the project root.
 * @param uriBaseId - always SRCROOT
 */
data class ArtifactLocation(
  val uri: String,
  val uriBaseId: String = "SRCROOT",
)

data class Message(val text: String)
