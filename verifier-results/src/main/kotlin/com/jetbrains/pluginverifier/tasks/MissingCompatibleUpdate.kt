package com.jetbrains.pluginverifier.tasks

import com.intellij.structure.ide.IdeVersion

/**
 * @author Sergey Patrikeev
 */
data class MissingCompatibleUpdate(val pluginId: String,
                                   val ideVersion: IdeVersion,
                                   val details: String) {
  override fun toString(): String = "For $pluginId there are no updates compatible with $ideVersion in the Plugin Repository${if (details.isNullOrEmpty()) "" else " ($details)"}"
}