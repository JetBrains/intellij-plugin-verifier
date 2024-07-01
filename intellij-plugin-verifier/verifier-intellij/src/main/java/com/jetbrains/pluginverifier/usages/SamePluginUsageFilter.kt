/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.pluginverifier.usages

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

/**
 * API Usage filter that allows class usages, method invocations and field references
 * within the same JAR, directory or similar common origins.
 *
 * This filter will ignore API usages that occur within the same plugin.
 */
class SamePluginUsageFilter : ApiUsageFilter {

  override fun allow(
    classReference: ClassReference,
    invocationTarget: ClassFile,
    caller: ClassFileMember,
    usageType: ClassUsageType,
    context: VerificationContext
  ): Boolean {
    val usageHost = caller.location.containingClass
    val apiHost = invocationTarget.location.containingClass
    return allow(usageHost, apiHost)
  }

  override fun allow(
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean {
    val usageHost = callerMethod.location.containingClass
    val apiHost = invokedMethod.location.containingClass
    return allow(usageHost, apiHost)
  }

  override fun allow(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean {
    val apiHost = callerMethod.location.containingClass
    val usageHost = resolvedField.location.containingClass
    return allow(usageHost, apiHost)
  }

  private fun allow(usageLocation: ClassLocation, apiLocation: ClassLocation): Boolean {
    val callSourceOrigin = usageLocation.classFileOrigin
    val callTargetOrigin = apiLocation.classFileOrigin
    if (isInvocationWithinPlatform(callSourceOrigin, callTargetOrigin)) {
      return true
    }
    return callTargetOrigin
      .findOriginOfType<PluginFileOrigin>()
      .takeIf { callSourceOrigin == it } != null
  }

  private fun isInvocationWithinPlatform(usageOrigin: FileOrigin, apiHostOrigin: FileOrigin): Boolean {
    if (!apiHostOrigin.isOriginOfType<IdeFileOrigin>()) return false
    if (usageOrigin == apiHostOrigin) return true
    if (usageOrigin.isOriginOfType<IdeFileOrigin>()) return true
    return false
  }
}