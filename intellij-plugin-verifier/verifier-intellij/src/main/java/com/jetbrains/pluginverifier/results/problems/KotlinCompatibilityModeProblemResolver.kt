package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.intellij.problems.UndeclaredKotlinK2CompatibilityMode
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationDescriptor.IDE
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext

/**
 Remaps [UndeclaredKotlinK2CompatibilityMode] structure [com.jetbrains.plugin.structure.base.problems.PluginProblem]
 to a Plugin Verifier [CompatibilityProblem] if the IDE version is at least 2024.2.1.

 The `structure` library reports the [UndeclaredKotlinK2CompatibilityMode] as a warning.
 However, depending on the Platform version, this warning might be either ignored or resolved to a Plugin Verifier
 [CompatibilityProblem].
 */
class KotlinCompatibilityModeProblemResolver : CompatibilityProblemResolver {
  private val sinceIdeVersion = IdeVersion.createIdeVersion("242.21829.142")

  override fun resolveCompatibilityProblems(context: PluginVerificationContext): List<CompatibilityProblem> {
    if (!context.isSinceSupportedIdeVersion()) return emptyList()

    return context.pluginStructureWarnings
      .map { it.problem }
      .filterIsInstance<UndeclaredKotlinK2CompatibilityMode>()
      .map { UndeclaredKotlinK2CompatibilityModeProblem(it) }
  }

  private fun PluginVerificationContext.isSinceSupportedIdeVersion(): Boolean {
    if (verificationDescriptor !is IDE) return false
    return verificationDescriptor.ideVersion >= sinceIdeVersion
  }
}