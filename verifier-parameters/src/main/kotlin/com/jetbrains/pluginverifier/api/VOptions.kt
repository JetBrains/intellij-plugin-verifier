package com.jetbrains.pluginverifier.api

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.Plugin
import com.intellij.structure.impl.utils.StringUtil
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.MissingReason
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.IFileLock
import java.util.regex.Pattern

interface VProgress {
  fun getProgress(): Double

  fun setProgress(value: Double)

  fun getText(): String

  fun setText(text: String)
}

class DefaultVProgress() : VProgress {
  @Volatile private var progress: Double = 0.0

  @Volatile private var text: String = ""

  override fun getProgress(): Double = progress

  override fun setProgress(value: Double) {
    progress = value
  }

  override fun getText(): String = text

  override fun setText(text: String) {
    this.text = text
  }

}

/**
 * Accumulates parameters of the upcoming verification.
 */
data class VParams(

    /**
     * The JDK against which the plugins will be verified.
     */
    val jdkDescriptor: JdkDescriptor,

    /**
     * The pairs of _(plugin, ide)_ which will be verified.
     */
    val pluginsToCheck: List<Pair<PluginDescriptor, IdeDescriptor>>,

    /**
     * The options for the Verifier (excluded problems, etc).
     */
    val options: VOptions,

    /**
     * The Resolver for external classes. The verification can refer to them.
     */
    val externalClassPath: Resolver = Resolver.getEmptyResolver(),

    /**
     * If set to true the plugins can refer other plugins withing the verification.
     * It's used to check several plugins with dependencies between them.
     */
    val resolveDependenciesWithin: Boolean = false,

    /**
     * If set, this resolver will be used to resolve plugin dependencies.
     * Otherwise a default resolver which searches the plugin in the IDE
     * and in the Plugin Repository will be used.
     */
    val dependencyResolver: DependencyResolver? = null
) {
  override fun toString(): String {
    val todo: Map<IdeDescriptor, List<PluginDescriptor>> = pluginsToCheck.groupBy { it.second }.mapValues { it.value.map { it.first } }
    return "Jdk = $jdkDescriptor; " +
        "(IDE -> [Plugins]) = [${todo.entries.joinToString { "${it.key} -> [${it.value.joinToString()}]" }}]; " +
        "vOptions: $options; "
  }
}

interface DependencyResolver {
  fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): Result

  sealed class Result {
    class Found(val plugin: Plugin) : Result()
    class Created(val plugin: Plugin, val fileLock: IFileLock) : Result()
    class NotFound(val reason: MissingReason) : Result()
    object Skip : Result()
  }
}

/**
 * @author Sergey Patrikeev
 */
data class VOptions(@SerializedName("externalCp") val externalClassPrefixes: Set<String> = emptySet(),
                    /**
                     * Map of _(pluginXmlId, version)_ -> to be ignored _problem pattern_
                     */
                    @SerializedName("ignoredProblems") val problemsToIgnore: Multimap<Pair<String, String>, Pattern> = ImmutableMultimap.of()) {

  fun isIgnoredProblem(plugin: Plugin, problem: Problem): Boolean {
    val xmlId = plugin.pluginId
    val version = plugin.pluginVersion
    for (entry in problemsToIgnore.entries()) {
      val ignoreXmlId = entry.key.first
      val ignoreVersion = entry.key.second
      val ignoredPattern = entry.value

      if (StringUtil.equal(xmlId, ignoreXmlId)) {
        if (StringUtil.isEmpty(ignoreVersion) || StringUtil.equal(version, ignoreVersion)) {
          if (ignoredPattern.matcher(problem.getDescription()).matches()) {
            return true
          }
        }
      }
    }
    return false
  }

  fun isExternalClass(className: String): Boolean {
    for (prefix in externalClassPrefixes) {
      if (prefix.isNotEmpty() && className.startsWith(prefix)) {
        return true
      }
    }
    return false
  }

}