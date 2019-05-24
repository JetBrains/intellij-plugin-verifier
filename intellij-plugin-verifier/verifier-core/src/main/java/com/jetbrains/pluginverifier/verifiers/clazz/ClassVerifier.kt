package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile

interface ClassVerifier {
  fun verify(classFile: ClassFile, context: VerificationContext)
}
