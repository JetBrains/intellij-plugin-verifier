package com.jetbrains.pluginverifier.usages.util

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

fun VerificationContext.isFromVerifiedPlugin(fileMember: ClassFileMember): Boolean {
  val pluginFileOrigin = fileMember.containingClassFile.classFileOrigin.findOriginOfType<PluginFileOrigin>()
  return this is PluginVerificationContext && idePlugin == pluginFileOrigin?.idePlugin
}