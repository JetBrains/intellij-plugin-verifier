/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId
import com.jetbrains.pluginverifier.PluginVerifier

/**
 * List of plugins that take significant time to verify.
 * Observed in internal JetBrains builds that verify fresh IDE versions.
 */
@Suppress("SpellCheckingInspection")
private val PluginsWithLongVerificationTime: Set<PluginId> = setOf(
  "ai.zencoder.plugin",
  "cn.cangnova.cangjie",
  "com.alibabacloud.intellij.toolkit-intellij",
  "com.codeium.intellij",
  "com.codeium.rd",
  "com.cursiveclojure.cursive",
  "com.explyt.test",
  "com.google.gct.core",
  "com.google.idea.bazel.ijwb",
  "com.google.tools.ij.aiplugin",
  "com.illuminatedcloud2.intellij",
  "com.intellij.idea.plugin.sap.commerce",
  "com.jetbrains.edu",
  "com.jetbrains.kmm",
  "com.jetbrains.php",
  "com.jetbrains.rust",
  "CUBA",
  "icu.windea.pls",
  "intellij.bigdatatools.gcloud",
  "io.jmix.studio",
  "io.kotzilla.koin",
  "io.openbpm.studio",
  "io.wiz",
  "maxmoro.lg",
  "org.hzero.copilot",
  "org.intellij.scala",
  "org.jetbrains.android",
  "org.jetbrains.bazel",
  "org.jetbrains.intellij.scripting-javascript",
  "org.jetbrains.intellij.scripting-python",
  "org.jetbrains.junie",
  "org.jetbrains.plugins.clion.radler",
  "org.jetbrains.plugins.go",
  "org.jetbrains.plugins.ruby",
  "org.jetbrains.toolbox-enterprise-client",
  "PythonCore",
)

private val PluginIdComparator = Comparator<PluginId> { a, b ->
  val slowA = a in PluginsWithLongVerificationTime
  val slowB = b in PluginsWithLongVerificationTime

  if (slowA == slowB) a.compareTo(b)
  else if (slowA) -1
  else 1
}
private val PluginVerifierComparator: Comparator<in PluginVerifier> = compareBy(PluginIdComparator) { it.verificationDescriptor.checkedPlugin.pluginId }

/**
 * Sort verification tasks to increase the chances that two verifications of the same plugin
 * would be executed shortly, and therefore caches, such as plugin details cache, would be warmed up.
 * Also move plugins which take significant time to verify to the front of the queue.
 */
internal fun List<PluginVerifier>.sortWithBigPluginsInFront(): List<PluginVerifier> = sortedWith(PluginVerifierComparator)