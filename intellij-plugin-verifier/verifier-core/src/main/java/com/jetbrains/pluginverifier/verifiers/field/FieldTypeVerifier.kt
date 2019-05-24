package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Field

class FieldTypeVerifier : FieldVerifier {
  override fun verify(field: Field, context: VerificationContext) {
    val className = field.descriptor.extractClassNameFromDescriptor() ?: return
    context.classResolver.resolveClassChecked(className, field, context)
  }
}
