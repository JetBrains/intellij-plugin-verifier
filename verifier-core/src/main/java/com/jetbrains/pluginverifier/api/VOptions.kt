package com.jetbrains.pluginverifier.api

import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.Plugin
import com.intellij.structure.impl.utils.StringUtil
import com.jetbrains.pluginverifier.problems.Problem
import java.util.regex.Pattern

/**
 * @author Sergey Patrikeev
 */
data class VOptions(@SerializedName("externalCp") val externalClassPrefixes: Set<String>,
                    @SerializedName("ignoredProblem") val optionalDependenciesIdsToIgnoreIfMissing: Set<String>,
                    /**
                     * Map of _(pluginXmlId, version)_ -> to be ignored _problem pattern_
                     */
                    @SerializedName("ignoredProblems") val problemsToIgnore: Multimap<Pair<String, String>, Pattern>,
                    @SerializedName("failOnCycle") val failOnCyclicDependencies: Boolean) {

  fun isIgnoredProblem(plugin: Plugin, problem: Problem): Boolean {
    val xmlId = plugin.pluginId
    val version = plugin.pluginVersion
    for (entry in problemsToIgnore.entries()) {
      val ignoreXmlId = entry.key.first
      val ignoreVersion = entry.key.second
      val ignoredPattern = entry.value

      if (StringUtil.equal(xmlId, ignoreXmlId)) {
        if (StringUtil.isEmpty(ignoreVersion) || StringUtil.equal(version, ignoreVersion)) {
          if (ignoredPattern.matcher(problem.description.replace('/', '.')).matches()) {
            return true
          }
        }
      }
    }
    return false
  }

  fun isIgnoreDependency(pluginId: String): Boolean {
    return isIgnoreMissingOptionalDependency(pluginId) //TODO: add an option to ignore mandatory plugins too
  }

  private fun isIgnoreMissingOptionalDependency(pluginId: String): Boolean {
    return optionalDependenciesIdsToIgnoreIfMissing.contains(pluginId)
  }

  fun isExternalClass(className: String): Boolean {
    for (prefix in externalClassPrefixes) {
      if (prefix.length > 0 && className.startsWith(prefix)) {
        return true
      }
    }
    return false
  }

}