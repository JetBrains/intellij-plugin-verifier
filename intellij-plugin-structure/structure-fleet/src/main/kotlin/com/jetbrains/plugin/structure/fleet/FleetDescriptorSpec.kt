/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.utils.CompatibilityUtils

object FleetDescriptorSpec {
  const val DESCRIPTOR_FILE_NAME = "extension.json"
  const val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

  const val ID_FIELD_NAME = "id"
  const val VERSION_FIELD_NAME = "version"

  object CompatibleShipVersion {
    const val NAME = "compatibleShipVersionRange"

    const val FROM_FIELD_NAME = "from"
    const val TO_FIELD_NAME = "to"

    object LegacyVersioningSpec {
      const val VERSION_PATCH_LENGTH = 14
      const val VERSION_MINOR_LENGTH = 13

      const val MAJOR_PART_MAX_VALUE = 7449 // 1110100011001
      const val MINOR_PART_MAX_VALUE = 1.shl(VERSION_MINOR_LENGTH) - 1 // 8191
      const val PATCH_PART_MAX_VALUE = 1.shl(VERSION_PATCH_LENGTH) - 1 // 16383
    }

    object UnifiedVersioningSpec {
      const val MAJOR_PART_MAX_VALUE = CompatibilityUtils.MAX_BRANCH_VALUE
      const val MINOR_PART_MAX_VALUE = CompatibilityUtils.MAX_BUILD_VALUE
      const val PATCH_PART_MAX_VALUE = CompatibilityUtils.MAX_COMPONENT_VALUE
    }

    fun getVersionConstraints(isLegacy: Boolean): Triple<Int, Int, Int> {
      return if (isLegacy) {
        Triple(
          LegacyVersioningSpec.MAJOR_PART_MAX_VALUE,
          LegacyVersioningSpec.MINOR_PART_MAX_VALUE,
          LegacyVersioningSpec.PATCH_PART_MAX_VALUE
        )
      } else {
        Triple(
          UnifiedVersioningSpec.MAJOR_PART_MAX_VALUE,
          UnifiedVersioningSpec.MINOR_PART_MAX_VALUE,
          UnifiedVersioningSpec.PATCH_PART_MAX_VALUE
        )
      }
    }

    fun relativeFieldPath(field: String) = FleetDescriptorSpec.relativeFieldPath(NAME) + "." + field
  }

  object Meta {
    const val NAME = "meta"

    const val NAME_FIELD_NAME = "readableName"
    const val DESCRIPTION_FIELD_NAME = "description"
    const val VENDOR_FIELD_NAME = "vendor"
    const val FRONTEND_ONLY_FIELD_NAME = "frontend-only"
    const val HUMAN_VISIBLE_FIELD_NAME = "visible"
    const val SUPPORTED_PRODUCTS_FIELD_NAME = "supportedProducts"

    fun relativeFieldPath(field: String) = FleetDescriptorSpec.relativeFieldPath(CompatibleShipVersion.NAME) + "." + field
  }

  fun relativeFieldPath(field: String) = field
}