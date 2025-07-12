/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.ElementAvailableOnlySinceNewerVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

class ProjectAndApplicationListenerAvailabilityVerifier {
  private val listenersAvailableSinceBuild = IdeVersion.createIdeVersion("193")

  fun verify(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    val sinceBuild = plugin.sinceBuild
    if (sinceBuild == null || sinceBuild >= listenersAvailableSinceBuild) return
    if (plugin.appContainerDescriptor.listeners.isNotEmpty()) {
      problemRegistrar.registerUnavailableElement(plugin, sinceBuild, "applicationListeners")
    }
    if (plugin.projectContainerDescriptor.listeners.isNotEmpty()) {
      problemRegistrar.registerUnavailableElement(plugin, sinceBuild, "projectListeners")
    }
  }

  private fun ProblemRegistrar.registerUnavailableElement(plugin: IdePlugin, sinceBuild: IdeVersion, elementName: String) {
    registerProblem(
      ElementAvailableOnlySinceNewerVersion(
        elementName,
        listenersAvailableSinceBuild,
        sinceBuild,
        plugin.untilBuild
      )
    )
  }
}