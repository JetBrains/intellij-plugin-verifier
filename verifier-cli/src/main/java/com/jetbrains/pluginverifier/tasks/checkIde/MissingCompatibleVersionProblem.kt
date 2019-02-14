package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.appendWithSpaceIfNotBlank

/**
 * Represents a problem of type "No versions of plugin X compatible with IDE Y".
 *
 * Existence of a compatible plugin version may be important for JetBrains plugins
 * when the next IDE EAP is published: all the JetBrains plugins must
 * be published to the Plugin Repository to make the EAP useful.
 */
data class MissingCompatibleVersionProblem(
    val pluginId: String,
    val ideVersion: IdeVersion,
    private val details: String
) {

  override fun toString() = "For plugin '$pluginId' there are no versions compatible with $ideVersion " +
      "in the Plugin Repository" + details.appendWithSpaceIfNotBlank()
}