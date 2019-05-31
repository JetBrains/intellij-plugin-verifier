package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile

class NonExtendableTypeInheritedVerifier : ClassVerifier {
  override fun verify(classFile: ClassFile, context: VerificationContext) {
    if (context !is NonExtendableApiRegistrar) {
      return
    }

    val superTypeNames = listOfNotNull(classFile.superName) + classFile.interfaces
    for (superTypeName in superTypeNames) {
      val superType = context.classResolver.resolveClassChecked(superTypeName, classFile, context) ?: continue
      if (superType.isNonExtendable()) {
        context.registerNonExtendableApiUsage(NonExtendableTypeInherited(superType.location, classFile.location))
      }
    }
  }
}
