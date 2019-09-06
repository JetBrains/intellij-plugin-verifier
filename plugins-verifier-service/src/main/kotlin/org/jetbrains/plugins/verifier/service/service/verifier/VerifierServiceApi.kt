package org.jetbrains.plugins.verifier.service.service.verifier

// This file must be synced with VerificationDto in the Marketplace repository.

enum class VerificationResultTypeDto {
  OK,
  STRUCTURE_WARNINGS,
  COMPATIBILITY_PROBLEMS,
  MISSING_DEPENDENCIES,
  INVALID_PLUGIN,
  NON_DOWNLOADABLE;
}

data class FullVerificationResultDto(
  val updateId: Int,
  val ideVersion: AvailableIdeDto,
  val javaVersion: String,
  val resultType: VerificationResultTypeDto,
  val verificationVerdict: String,
  val dependenciesGraph: DependenciesGraphDto?,
  val pluginStructureWarnings: List<PluginStructureWarningDto> = emptyList(),
  val pluginStructureErrors: List<PluginStructureErrorDto> = emptyList(),
  val compatibilityWarnings: List<CompatibilityWarningDto> = emptyList(),
  val compatibilityProblems: List<CompatibilityProblemDto> = emptyList(),
  val deprecatedApiUsages: List<DeprecatedApiUsageDto> = emptyList(),
  val experimentalApiUsages: List<ExperimentalApiUsageDto> = emptyList(),
  val internalApiUsages: List<InternalApiUsageDto> = emptyList(),
  val overrideOnlyApiUsages: List<OverrideOnlyApiUsageDto> = emptyList(),
  val nonExtendableApiUsages: List<NonExtendableApiUsageDto> = emptyList(),
  val nonDownloadableReason: String? = null
)

data class AvailableIdeDto(
  val ideVersion: String,
  val releaseVersion: String?
)

data class CompatibilityProblemDto(
  val shortDescription: String,
  val fullDescription: String,
  val problemType: String
)

enum class ApiElementTypeDto {
  CLASS,
  INTERFACE,
  ANNOTATION,
  ENUM,
  METHOD,
  CONSTRUCTOR,
  FIELD
}

data class PluginStructureWarningDto(
  val message: String
)

data class CompatibilityWarningDto(
  val message: String
)

data class PluginStructureErrorDto(
  val message: String
)

data class DeprecatedApiUsageDto(
  val apiElement: String,
  val usageLocation: String,
  val apiElementType: ApiElementTypeDto,
  val shortDescription: String,
  val fullDescription: String,
  val deprecationInfo: DeprecationInfoDto
)

data class DeprecationInfoDto(
  val forRemoval: Boolean,
  val untilVersion: String?
)

data class ExperimentalApiUsageDto(
  val apiElement: String,
  val usageLocation: String,
  val apiElementType: ApiElementTypeDto,
  val shortDescription: String,
  val fullDescription: String

)

data class InternalApiUsageDto(
  val apiElement: String,
  val usageLocation: String,
  val apiElementType: ApiElementTypeDto,
  val shortDescription: String,
  val fullDescription: String
)

data class OverrideOnlyApiUsageDto(
  val apiElement: String,
  val usageLocation: String,
  val apiElementType: ApiElementTypeDto,
  val shortDescription: String,
  val fullDescription: String
)

data class NonExtendableApiUsageDto(
  val apiElement: String,
  val usageLocation: String,
  val apiElementType: ApiElementTypeDto,
  val shortDescription: String,
  val fullDescription: String
)

class DependenciesGraphDto(
  val start: DependencyNodeDto,
  val vertices: List<DependencyNodeDto>,
  val edges: List<DependencyEdgeDto>
) {

  data class DependencyNodeDto(
    val pluginId: String,
    val version: String,
    val missingDependencies: List<MissingDependencyDto>
  )

  data class DependencyEdgeDto(
    val from: DependencyNodeDto,
    val to: DependencyNodeDto,
    val dependency: DependencyDto
  )

  data class DependencyDto(
    val dependencyId: String,
    val isOptional: Boolean,
    val isModule: Boolean
  )

  data class MissingDependencyDto(
    val dependency: DependencyDto,
    val missingReason: String
  )
}