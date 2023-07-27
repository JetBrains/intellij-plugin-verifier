/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

@file:Suppress("unused")

package com.jetbrains.pluginverifier.response

/*
These classes must be synced with JSON schema expected by JetBrains Marketplace.
Incompatible changes to the classes (e.g. fields renames or removals) are prohibited!
Compatible additions of new fields are allowed.
 */

enum class VerificationResultTypeDto {
  OK,
  WARNINGS,
  PROBLEMS,
  INVALID_PLUGIN,
  NON_DOWNLOADABLE,
  UNCHANGED;
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
  val dynamicPluginStatus: DynamicPluginStatusDto = DynamicPluginStatusDto(false, emptyList())
)

data class AvailableIdeDto(
  val ideVersion: String,
  val releaseVersion: String?,
  val productName: String?
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

data class MissingDependenciesSetDto(
  val dependencyNode: DependenciesGraphDto.DependencyNodeDto,
  val missingDependencies: Set<DependenciesGraphDto.MissingDependencyDto>
)

data class DependenciesGraphDto(
  val start: DependencyNodeDto,
  val vertices: List<DependencyNodeDto>,
  val edges: List<DependencyEdgeDto>,
  val missingDependencies: List<MissingDependenciesSetDto>
) {

  data class DependencyNodeDto(
    val pluginId: String,
    val version: String
  )

  data class DependencyEdgeDto(
    val from: DependencyNodeDto,
    val to: DependencyNodeDto,
    val dependency: DependencyDto
  )

  data class DependencyDto(
    val dependencyId: String,
    val optional: Boolean,
    val module: Boolean
  )

  data class MissingDependencyDto(
    val dependency: DependencyDto,
    val missingReason: String
  )
}

data class DynamicPluginStatusDto(
  val dynamic: Boolean,
  val reasons: List<String>
)
