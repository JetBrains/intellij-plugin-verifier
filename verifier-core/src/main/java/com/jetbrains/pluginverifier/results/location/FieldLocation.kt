package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation

data class FieldLocation(val hostClass: ClassLocation,
                         val fieldName: String,
                         val fieldDescriptor: String,
                         val signature: String,
                         val modifiers: Modifiers) : Location {

  override fun toString(): String = formatFieldLocation(HostClassOption.FULL_HOST_WITH_SIGNATURE, FieldTypeOption.SIMPLE_HOST_NAME)
}