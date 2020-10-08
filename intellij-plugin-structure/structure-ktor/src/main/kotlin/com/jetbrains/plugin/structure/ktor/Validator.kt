/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.ktor.bean.*
import com.jetbrains.plugin.structure.ktor.bean.GradleRepositoryType
import com.jetbrains.plugin.structure.ktor.problems.DocumentationContainsResource
import com.jetbrains.plugin.structure.ktor.problems.GradleRepoIncorrectDescription
import com.jetbrains.plugin.structure.ktor.problems.IncorrectKtorVersionFormat
import com.jetbrains.plugin.structure.ktor.problems.IncorrectKtorVersionRange

internal fun validateKtorPluginBean(descriptor: KtorFeatureDescriptor): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  val vendor = descriptor.vendor
  if (vendor == null || vendor.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(VENDOR))
  }
  if (descriptor.pluginName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(NAME))
  }
  if (descriptor.pluginId.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(ID))
  }
  if (descriptor.pluginVersion.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(VERSION))
  }
  if (descriptor.shortDescription.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(SHORT_DESCRIPTION))
  }
  if (descriptor.documentation == null) {
    problems.add(PropertyNotSpecified(DOCUMENTATION))
  }

  descriptor.documentation?.let { documentation ->
    if (documentation.description.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(DOCUMENTATION_DESCRIPTION))
    }
    if (documentation.usage.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(DOCUMENTATION_USAGE))
    }
    if (documentation.options.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(DOCUMENTATION_OPTIONS))
    }

    if (documentation.description?.contains("\\!\\[.*\\]\\(".toRegex()) == true) {
      problems.add(DocumentationContainsResource("description"))
    }
    if (documentation.usage?.contains("\\!\\[.*\\]\\(".toRegex()) == true) {
      problems.add(DocumentationContainsResource("usage"))
    }
    if (documentation.options?.contains("\\!\\[.*\\]\\(".toRegex()) == true) {
      problems.add(DocumentationContainsResource("options"))
    }
  }

  descriptor.installRecipe?.templates?.forEach { codeTemplate ->
    if (codeTemplate.position == null) {
      problems.add(PropertyNotSpecified(TEMPLATE_POSITION))
    }
    if (codeTemplate.text.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(TEMPLATE_TEXT))
    }
  }

  (descriptor.dependencies.orEmpty()
    + descriptor.testDependencies.orEmpty()).forEach { dependency ->
    if (dependency.group.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(DEPENDENCY_GROUP))
    }
    if (dependency.artifact.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(DEPENDENCY_ARTIFACT))
    }
    if (dependency.version == "") {
      problems.add(PropertyNotSpecified(DEPENDENCY_VERSION))
    }
  }

  descriptor.mavenInstall?.repositories?.forEach { repo ->
    if (repo.id.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(MAVEN_REP_ID))
    }
    if (repo.url.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(MAVEN_REP_URL))
    }
  }

  descriptor.gradleInstall?.repositories?.forEach { repo ->
    if (repo.type == null) {
      problems.add(PropertyNotSpecified(GRADLE_REP_TYPE))
    }
    if (repo.type == GradleRepositoryType.FUNCTION && repo.functionName.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(GRADLE_REP_FUNCTION))
    }
    if (repo.type == GradleRepositoryType.FUNCTION && !repo.url.isNullOrBlank()) {
      problems.add(
        GradleRepoIncorrectDescription(
          expectedField = GRADLE_REP_FUNCTION,
          unexpectedField = GRADLE_REP_URL
        )
      )
    }
    if (repo.type == GradleRepositoryType.URL && repo.url.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(GRADLE_REP_URL))
    }
    if (repo.type == GradleRepositoryType.URL && !repo.functionName.isNullOrBlank()) {
      problems.add(
        GradleRepoIncorrectDescription(
          expectedField = GRADLE_REP_URL,
          unexpectedField = GRADLE_REP_FUNCTION
        )
      )
    }
  }

  if (descriptor.gradleInstall != null && descriptor.mavenInstall == null) {
    problems.add(PropertyNotSpecified(MAVEN_INSTALL))
  }

  if (descriptor.gradleInstall == null && descriptor.mavenInstall != null) {
    problems.add(PropertyNotSpecified(GRADLE_INSTALL))
  }

  descriptor.mavenInstall?.repositories?.forEach { repo ->
    if (repo.id.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(MAVEN_REP_ID))
    }
    if (repo.url.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(MAVEN_REP_URL))
    }
  }

  descriptor.gradleInstall?.plugins?.forEach { plugin ->
    if (plugin.id.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(PLUGIN_ID))
    }
  }

  descriptor.mavenInstall?.plugins?.forEach { plugin ->
    if (plugin.group.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(PLUGIN_GROUP))
    }
    if (plugin.artifact.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(PLUGIN_ARTIFACT))
    }
  }
  val sinceParsed = parseKtorVersion(descriptor.ktorVersion?.since, problems)
  val untilParsed = parseKtorVersion(descriptor.ktorVersion?.until, problems)

  if (sinceParsed != null && untilParsed != null) {
    validateKtorVersioRange(sinceParsed, untilParsed, problems)
  }

  return problems
}


// Ktor version is represented in a format of x.y.z-*
private data class KtorVersionParsed(val x: Int, val y: Int, val z: Int): Comparable<KtorVersionParsed> {
  fun asString(): String = "$x.$y.$z"

  override fun compareTo(other: KtorVersionParsed): Int = when {
    x < other.x -> -1
    x > other.x -> 1
    y < other.y -> -1
    y > other.y -> 1
    z < other.z -> -1
    z > other.z -> 1
    else -> 0
  }
}

private fun parseKtorVersion(version: String?, problems: MutableList<PluginProblem>) : KtorVersionParsed? {
    return if (version.isNullOrBlank()) {
        problems.add(PropertyNotSpecified(KTOR_VERSION))
        null
    } else {
        val parts = version
                .split("-")
                .first()
                .split(".")

        if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) {
            problems.add(IncorrectKtorVersionFormat(version))
        }

        KtorVersionParsed(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }
}

private fun validateKtorVersioRange(
  since: KtorVersionParsed,
  until: KtorVersionParsed,
  problems: MutableList<PluginProblem>
) {
  if (since > until) {
    problems.add(IncorrectKtorVersionRange(since.asString(), until.asString()))
  }
}
