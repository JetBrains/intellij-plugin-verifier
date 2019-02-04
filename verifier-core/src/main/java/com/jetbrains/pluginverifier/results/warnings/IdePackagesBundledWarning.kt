package com.jetbrains.pluginverifier.results.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

/**
 * Verified plugin bundles packages that belong to IDE.
 * It is considered wrong plugin configuration.
 * Plugin author should reuse the IDE classes.
 */
data class IdePackagesBundledWarning(val idePackages: List<String>) : PluginProblem() {
  override val level = Level.WARNING

  override val message
    get() = buildString {
      append("The plugin distribution contains IDE packages: ")
      if (idePackages.size < 5) {
        append(idePackages.joinToString())
      } else {
        append(idePackages.take(3).joinToString())
        append(" and ${idePackages.size - 3} other")
      }
      append(". ")
      append("Bundling IDE classes is considered bad practice and may lead to sophisticated compatibility problems. ")
      append("Consider excluding IDE classes from the plugin distribution and reusing the IDE's classes. ")
      append("If your plugin depends on classes of an IDE bundled plugin, explicitly specify dependency on that plugin instead of bundling it. ")
    }
}