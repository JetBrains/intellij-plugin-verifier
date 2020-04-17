/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class FieldTypeVerifier : FieldVerifier {
  override fun verify(field: Field, context: VerificationContext) {
    val className = field.descriptor.extractClassNameFromDescriptor() ?: return
    context.classResolver.resolveClassChecked(className, field, context)
  }
}
