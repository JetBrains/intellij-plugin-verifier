/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class NonExtendableTypeInheritedProcessor(private val nonExtendableApiRegistrar: NonExtendableApiRegistrar) : ClassVerifier {
  override fun verify(classFile: ClassFile, context: VerificationContext) {
    val superTypeNames = listOfNotNull(classFile.superName) + classFile.interfaces
    for (superTypeName in superTypeNames) {
      val superType = context.classResolver.resolveClassChecked(superTypeName, classFile, context) ?: continue
      if (superType.isNonExtendable()) {
        nonExtendableApiRegistrar.registerNonExtendableApiUsage(NonExtendableTypeInherited(superType.location, classFile.location))
      }
    }
  }
}
