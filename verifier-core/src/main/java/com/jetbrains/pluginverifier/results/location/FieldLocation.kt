package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils

data class FieldLocation(val hostClass: ClassLocation,
                         val fieldName: String,
                         val fieldDescriptor: String,
                         val signature: String,
                         val modifiers: Modifiers) : Location {
  override val shortPresentation: String = "$hostClass.${fieldNameAndType(PresentationUtils.cutPackageConverter)}"

  override val fullPresentation: String = "$hostClass.${fieldNameAndType(PresentationUtils.normalConverter)}"

  private fun fieldNameAndType(descriptorConverter: (String) -> String): String {
    if (signature.isNotEmpty()) {
      return "$fieldName : ${PresentationUtils.convertFieldSignature(signature, descriptorConverter)}"
    }
    val type = PresentationUtils.convertJvmDescriptorToNormalPresentation(fieldDescriptor, descriptorConverter)
    return "$fieldName : $type"
  }

  override fun toString(): String = shortPresentation
}