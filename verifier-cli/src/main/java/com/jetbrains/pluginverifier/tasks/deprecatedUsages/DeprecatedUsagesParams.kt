package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeParams


data class DeprecatedUsagesParams(val checkIdeParams: CheckIdeParams) : TaskParameters {
  override fun presentableText(): String = with(checkIdeParams) {
    """Deprecated usages detection parameters:
IDE to be checked: $ideDescriptor
JDK: $jdkDescriptor
Plugins to be checked: [${pluginsToCheck.joinToString()}]
Excluded plugins: [${excludedPlugins.joinToString()}]
"""
  }


  override fun close() {
    checkIdeParams.close()
  }

  override fun toString(): String = presentableText()
}