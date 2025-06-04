/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.fleet.FleetDescriptorSpec

open class InvalidSupportedProductsListProblem(constraint: String) : InvalidDescriptorProblem(
  descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
  detailedMessage = "Invalid `${FleetDescriptorSpec.Meta.relativeFieldPath(FleetDescriptorSpec.Meta.SUPPORTED_PRODUCTS_FIELD_NAME)}` field value: $constraint."
) {
  override val level
    get() = Level.ERROR
}