package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption.FULL_TYPE
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.NO_PARAMETER_NAMES
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import com.jetbrains.pluginverifier.results.presentation.formatFieldReference
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference
import java.util.*

class IllegalFieldAccessProblem(
    val fieldBytecodeReference: FieldReference,
    val inaccessibleField: FieldLocation,
    val accessor: MethodLocation,
    val instruction: Instruction,
    val fieldAccess: AccessType
) : CompatibilityProblem() {

  override val problemType
    get() = "Illegal field access"

  override val shortDescription
    get() = "Illegal access to a {0} field {1}".formatMessage(fieldAccess, inaccessibleField)

  override val fullDescription
    get() = buildString {
      append("Method {0} contains a *{1}* instruction referencing ".formatMessage(
          accessor.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, NO_PARAMETER_NAMES),
          instruction
      ))

      val actualFieldPresentation = inaccessibleField.formatFieldLocation(FULL_HOST_NAME, FULL_TYPE)
      if (fieldBytecodeReference.hostClass.className == inaccessibleField.hostClass.className) {
        append("a {0} field {1} ".formatMessage(
            fieldAccess,
            actualFieldPresentation
        ))
      } else {
        append("{0} which is resolved to a {1} field {2} ".formatMessage(
            fieldBytecodeReference.formatFieldReference(FULL_HOST_NAME, FULL_TYPE),
            fieldAccess,
            actualFieldPresentation
        ))
      }
      append("inaccessible to a class {0}. ".formatMessage(
          accessor.hostClass.formatClassLocation(FULL_NAME, NO_GENERICS)
      ))
      append("This can lead to **IllegalAccessError** exception at runtime.")
    }

  override fun equals(other: Any?) =
      other is IllegalFieldAccessProblem
          && fieldAccess == other.fieldAccess
          && inaccessibleField == other.inaccessibleField
          && accessor == other.accessor
          && instruction == other.instruction

  override fun hashCode() = Objects.hash(fieldAccess, inaccessibleField, accessor, instruction)

}