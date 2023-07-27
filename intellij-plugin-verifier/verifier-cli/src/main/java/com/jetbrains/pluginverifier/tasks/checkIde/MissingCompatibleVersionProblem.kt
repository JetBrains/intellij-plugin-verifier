/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

/**
 * Represents a problem of type "No versions of plugin X compatible with IDE Y".
 *
 * Existence of a compatible plugin version may be important for JetBrains plugins
 * when the next IDE EAP is published: all the JetBrains plugins must
 * be published to JetBrains Marketplace to make the EAP useful.
 */
data class MissingCompatibleVersionProblem(
  val pluginId: String,
  val ideVersion: IdeVersion,
  private val details: String?
) {

  override fun toString() = "For plugin '$pluginId' there are no versions compatible with $ideVersion " +
    "in JetBrains Marketplace" + if (details != null) " $details" else ""
}
