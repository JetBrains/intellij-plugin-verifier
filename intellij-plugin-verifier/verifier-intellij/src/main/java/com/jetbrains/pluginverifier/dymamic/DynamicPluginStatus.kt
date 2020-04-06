package com.jetbrains.pluginverifier.dymamic

/**
 * Whether the IntelliJ plugin can be loaded/unloaded without IDE restart
 * [https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/dynamic_plugins.html]
 */
sealed class DynamicPluginStatus {

  abstract val reasonsNotToLoadUnloadWithoutRestart: Set<String>

  object MaybeDynamic : DynamicPluginStatus() {
    override val reasonsNotToLoadUnloadWithoutRestart
      get() = emptySet<String>()
  }

  data class NotDynamic(override val reasonsNotToLoadUnloadWithoutRestart: Set<String>) : DynamicPluginStatus()
}