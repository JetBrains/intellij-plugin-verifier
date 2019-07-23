package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import java.util.*

/**
 * [CompatibilityProblem] indicating that the plugin [pluginInfo]
 * is listed in `brokenPlugins.txt` and it will
 * not be loaded by IntelliJ IDE with message
 * `The following plugins are incompatible with the current IDE build`.
 */
class PluginIsMarkedIncompatibleProblem(
    private val pluginInfo: PluginInfo,
    private val ideVersion: IdeVersion
) : CompatibilityProblem() {

  override val problemType: String
    get() = "Incompatible plugin mark"

  override val shortDescription: String
    get() = "Plugin is marked as incompatible with $ideVersion"

  override val fullDescription: String
    get() = "Plugin $pluginInfo is marked as incompatible with $ideVersion in the special file 'brokenPlugins.txt' bundled " +
        "to the IDE distribution. This option is used to prevent loading of broken plugins, which may lead to IDE startup errors, " +
        "if the plugins remain locally installed (in config>/plugins directory) and the IDE is updated to newer version where " +
        "this plugin is no more compatible. The new IDE will refuse to load this plugin with a message " +
        "'The following plugins are incompatible with the current IDE build: ${pluginInfo.pluginId}' or similar."

  override fun equals(other: Any?) = other is PluginIsMarkedIncompatibleProblem &&
      pluginInfo == other.pluginInfo &&
      ideVersion == other.ideVersion

  override fun hashCode() = Objects.hash(pluginInfo, ideVersion)
}