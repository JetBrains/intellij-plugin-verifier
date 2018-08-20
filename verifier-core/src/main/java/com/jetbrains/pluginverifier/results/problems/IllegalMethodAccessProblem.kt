package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.NO_PARAMETER_NAMES
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.FULL_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import java.util.*

class IllegalMethodAccessProblem(
    val bytecodeMethodReference: MethodReference,
    val inaccessibleMethod: MethodLocation,
    val methodAccessModifier: AccessType,
    val caller: MethodLocation,
    val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Illegal method invocation"

  override val shortDescription
    get() = "Illegal invocation of {0} {1} {2}".formatMessage(methodAccessModifier, inaccessibleMethod.elementType.presentableName, inaccessibleMethod)

  override val fullDescription
    get() = buildString {
      append("{0} {1} contains an *{2}* instruction referencing ".formatMessage(
          caller.elementType.presentableName.capitalize(),
          caller.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, NO_PARAMETER_NAMES),
          instruction
      ))

      val actualMethodPresentation = inaccessibleMethod.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, NO_PARAMETER_NAMES)
      if (bytecodeMethodReference.hostClass.className == inaccessibleMethod.hostClass.className) {
        append("a {0} {1} {2} ".formatMessage(
            methodAccessModifier,
            inaccessibleMethod.elementType.presentableName,
            actualMethodPresentation
        ))
      } else {
        append("{0} which is resolved to a {1} {2} {3} ".formatMessage(
            bytecodeMethodReference.formatMethodReference(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME),
            methodAccessModifier,
            inaccessibleMethod.elementType.presentableName,
            actualMethodPresentation
        ))
      }
      append("inaccessible to a class {0}. ".formatMessage(
          caller.hostClass.formatClassLocation(FULL_NAME, NO_GENERICS)
      ))
      append("This can lead to **IllegalAccessError** exception at runtime.")
    }

  override fun equals(other: Any?) = other is IllegalMethodAccessProblem
      && methodAccessModifier == other.methodAccessModifier
      && inaccessibleMethod == other.inaccessibleMethod
      && caller == other.caller
      && instruction == other.instruction

  override fun hashCode() = Objects.hash(methodAccessModifier, inaccessibleMethod, caller, instruction)
}