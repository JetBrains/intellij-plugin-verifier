package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

/**
 * @author Sergey Patrikeev
 */
data class MissingCompatibleUpdate(val pluginId: String,
                                   val ideVersion: IdeVersion,
                                   val details: String) {
  override fun toString(): String = "For $pluginId there are no updates compatible with $ideVersion in the Plugin Repository${if (details.isEmpty()) "" else " ($details)"}"
}