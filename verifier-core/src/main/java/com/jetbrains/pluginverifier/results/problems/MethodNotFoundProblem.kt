package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.HierarchicalProblemsDescription
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.FULL_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodReference
import com.jetbrains.pluginverifier.results.presentation.methodOrConstructorWord
import com.jetbrains.pluginverifier.results.reference.MethodReference
import java.util.*

class MethodNotFoundProblem(
    val unresolvedMethod: MethodReference,
    val caller: MethodLocation,
    val instruction: Instruction,
    val methodOwnerHierarchy: ClassHierarchy
) : CompatibilityProblem() {

  override val problemType
    get() = "Method not found"

  override val shortDescription
    get() = "Invocation of unresolved {0} {1}".formatMessage(
        unresolvedMethod.methodOrConstructorWord,
        unresolvedMethod.formatMethodReference(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME)
    )

  private val descriptionMainPart
    get() = buildString {
      append("${caller.elementType.presentableName.capitalize()} {0} contains an *{1}* instruction referencing an unresolved {2} {3}. ".formatMessage(
          caller.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE),
          instruction,
          unresolvedMethod.methodOrConstructorWord,
          unresolvedMethod.formatMethodReference(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME)
      ))
      append("This can lead to **NoSuchMethodError** exception at runtime.")
    }

  override val fullDescription
    get() = buildString {
      append(descriptionMainPart)
      if (instruction != Instruction.INVOKE_SPECIAL) {
        append(HierarchicalProblemsDescription.presentableElementMightHaveBeenDeclaredInIdeSuperTypes("method", methodOwnerHierarchy, true, true))
      }
    }

  override fun equals(other: Any?) = other is MethodNotFoundProblem
      && unresolvedMethod == other.unresolvedMethod
      && caller == other.caller
      && instruction == other.instruction

  override fun hashCode() = Objects.hash(unresolvedMethod, caller, instruction)

}