package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.tasks.TaskParameters


class DeprecatedUsagesParams(
    val pluginsSet: PluginsSet,
    val ideDescriptor: IdeDescriptor,
    val ideVersionForCompatiblePlugins: IdeVersion,
    val verificationDescriptors: List<PluginVerificationDescriptor.IDE>
) : TaskParameters {

  override val presentableText
    get() = """
      |${verificationDescriptors.joinToString()}
    """.trimMargin()

  override fun close() {
    ideDescriptor.closeLogged()
  }

}