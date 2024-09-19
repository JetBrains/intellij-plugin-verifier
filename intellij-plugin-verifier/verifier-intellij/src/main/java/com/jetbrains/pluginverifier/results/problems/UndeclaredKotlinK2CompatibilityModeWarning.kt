package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.intellij.problems.UndeclaredKotlinK2CompatibilityMode
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning

data class UndeclaredKotlinK2CompatibilityModeWarning(private val problem: UndeclaredKotlinK2CompatibilityMode) : CompatibilityWarning() {
  override val problemType = "Plugin descriptor problem"
  override val shortDescription = "Plugin does not declare Kotlin plugin mode in the <org.jetbrains.kotlin.supportsKotlinPluginMode> extension. "
  override val fullDescription = "Plugin depends on the Kotlin plugin (org.jetbrains.kotlin) but does not declare " +
    "a compatibility mode in the <org.jetbrains.kotlin.supportsKotlinPluginMode> extension. " +
    "This feature is available for IntelliJ IDEA 2024.2.1 or later. " +
    "See https://kotlin.github.io/analysis-api/migrating-from-k1.html#declaring-compatibility-with-the-k2-kotlin-mode"
}