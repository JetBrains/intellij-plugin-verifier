package com.jetbrains.pluginverifier.problems

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.descriptions.DescriptionsBundle
import com.jetbrains.pluginverifier.descriptions.FullDescription
import com.jetbrains.pluginverifier.descriptions.ShortDescription
import com.jetbrains.pluginverifier.location.*
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.reference.FieldReference
import com.jetbrains.pluginverifier.reference.MethodReference
import org.jetbrains.annotations.PropertyKey

/**
 * @author Sergey Patrikeev
 */
sealed class Problem(@PropertyKey(resourceBundle = "long.descriptions") val messageKey: String) {

  fun getShortDescription(): ShortDescription {
    val shortTemplate = DescriptionsBundle.getShortDescription(messageKey)
    return ShortDescription(shortTemplate, shortDescriptionParams())
  }

  fun getFullDescription(): FullDescription {
    val descriptionParams = fullDescriptionParams()
    val fullTemplate = DescriptionsBundle.getFullDescription(messageKey)
    val effect = DescriptionsBundle.getEffect(messageKey)
    return FullDescription(fullTemplate, effect, descriptionParams)
  }

  protected abstract fun fullDescriptionParams(): List<Any>

  protected abstract fun shortDescriptionParams(): List<Any>

}

data class MultipleDefaultImplementationsProblem(@SerializedName("caller") val caller: MethodLocation,
                                                 @SerializedName("methodReference") val methodReference: MethodReference,
                                                 @SerializedName("instruction") val instruction: Instruction,
                                                 @SerializedName("implementation1") val implementation1: MethodLocation,
                                                 @SerializedName("implementation2") val implementation2: MethodLocation) : Problem("multiple.default.implementations") {

  override fun shortDescriptionParams(): List<Any> = listOf(methodReference)

  override fun fullDescriptionParams() = listOf(caller, instruction, methodReference, implementation1, implementation2)

}

data class IllegalClassAccessProblem(@SerializedName("unavailableClass") val unavailableClass: ClassLocation,
                                     @SerializedName("access") val access: AccessType,
                                     @SerializedName("usage") val usage: Location) : Problem("illegal.class.access") {

  override fun shortDescriptionParams(): List<Any> = listOf(access, unavailableClass)

  override fun fullDescriptionParams(): List<Any> {
    val type = if (unavailableClass.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "interface" else "class"
    return listOf(access.toString().capitalize(), type, unavailableClass, usage)
  }
}

data class AbstractClassInstantiationProblem(@SerializedName("abstractClass") val abstractClass: ClassLocation,
                                             @SerializedName("creator") val creator: MethodLocation) : Problem("abstract.class.instantiation") {


  override fun shortDescriptionParams(): List<Any> = listOf(abstractClass)

  override fun fullDescriptionParams() = listOf(creator, abstractClass)

}

data class ClassNotFoundProblem(@SerializedName("class") val unresolved: ClassReference,
                                @SerializedName("usage") val usage: Location) : Problem("class.not.found") {

  override fun shortDescriptionParams(): List<Any> = listOf(unresolved)

  override fun fullDescriptionParams(): List<Any> {
    val type: String = when (usage) {
      is ClassLocation -> "Class"
      is MethodLocation -> "Method"
      is FieldLocation -> "Field"
      else -> throw IllegalArgumentException()
    }
    return listOf(type, usage, unresolved)
  }
}

data class SuperInterfaceBecameClassProblem(@SerializedName("child") val child: ClassLocation,
                                            @SerializedName("class") val clazz: ClassLocation) : Problem("super.interface.became.class") {

  override fun shortDescriptionParams(): List<Any> = listOf(clazz)

  override fun fullDescriptionParams(): List<Any> {
    val type = if (child.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "Interface" else "Class"
    return listOf(type, child, clazz)
  }

}

data class InheritFromFinalClassProblem(@SerializedName("child") val child: ClassLocation,
                                        @SerializedName("finalClass") val finalClass: ClassLocation) : Problem("inherit.from.final.class") {

  override fun shortDescriptionParams(): List<Any> = listOf(finalClass)

  override fun fullDescriptionParams(): List<Any> {
    val type = if (child.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "Interface" else "Class"
    return listOf(type, child, finalClass)
  }
}

data class SuperClassBecameInterfaceProblem(@SerializedName("child") val child: ClassLocation,
                                            @SerializedName("interface") val interfaze: ClassLocation) : Problem("super.class.became.interface") {

  override fun shortDescriptionParams(): List<Any> = listOf(interfaze)

  override fun fullDescriptionParams() = listOf(child, interfaze)

}

data class InvokeClassMethodOnInterfaceProblem(@SerializedName("methodReference") val methodReference: MethodReference,
                                               @SerializedName("caller") val caller: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem("invoke.class.method.on.interface") {

  override fun shortDescriptionParams(): List<Any> = listOf(methodReference.hostClass)

  override fun fullDescriptionParams() = listOf(caller, instruction, methodReference, methodReference.hostClass)

}

data class InvokeInterfaceMethodOnClassProblem(@SerializedName("methodReference") val methodReference: MethodReference,
                                               @SerializedName("caller") val caller: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem("invoke.interface.method.on.class") {

  override fun shortDescriptionParams(): List<Any> = listOf(methodReference.hostClass)

  override fun fullDescriptionParams() = listOf(caller, instruction, methodReference, methodReference.hostClass)

}

data class InterfaceInstantiationProblem(@SerializedName("interface") val interfaze: ClassLocation,
                                         @SerializedName("creator") val creator: MethodLocation) : Problem("interface.instantiation") {

  override fun shortDescriptionParams(): List<Any> = listOf(interfaze)

  override fun fullDescriptionParams() = listOf(creator, interfaze)

}

data class ChangeFinalFieldProblem(@SerializedName("field") val field: FieldLocation,
                                   @SerializedName("accessor") val accessor: MethodLocation,
                                   @SerializedName("instruction") val instruction: Instruction) : Problem("change.final.field") {

  override fun shortDescriptionParams(): List<Any> = listOf(field)

  override fun fullDescriptionParams() = listOf(accessor, instruction, field)

}

data class FieldNotFoundProblem(@SerializedName("field") val field: FieldReference,
                                @SerializedName("accessor") val accessor: MethodLocation,
                                @SerializedName("instruction") val instruction: Instruction) : Problem("field.not.found") {

  override fun shortDescriptionParams(): List<Any> = listOf(field)

  override fun fullDescriptionParams() = listOf(accessor, instruction, field)
}

data class IllegalFieldAccessProblem(@SerializedName("field") val field: FieldLocation,
                                     @SerializedName("accessor") val accessor: MethodLocation,
                                     @SerializedName("instruction") val instruction: Instruction,
                                     @SerializedName("access") val fieldAccess: AccessType) : Problem("illegal.field.access") {

  override fun shortDescriptionParams(): List<Any> = listOf(fieldAccess, field)

  override fun fullDescriptionParams() = listOf(accessor, instruction, fieldAccess, field, accessor.hostClass)

}

data class IllegalMethodAccessProblem(@SerializedName("method") val method: MethodLocation,
                                      @SerializedName("caller") val caller: MethodLocation,
                                      @SerializedName("instruction") val instruction: Instruction,
                                      @SerializedName("access") val methodAccess: AccessType) : Problem("illegal.method.access") {

  override fun shortDescriptionParams(): List<Any> = listOf(methodAccess, method)

  override fun fullDescriptionParams() = listOf(caller, instruction, methodAccess, method, caller.hostClass)
}

data class InvokeInterfaceOnPrivateMethodProblem(@SerializedName("resolvedMethod") val resolvedMethod: MethodLocation,
                                                 @SerializedName("caller") val caller: MethodLocation) : Problem("invoke.interface.on.private.method") {

  override fun shortDescriptionParams(): List<Any> = listOf(resolvedMethod)

  override fun fullDescriptionParams() = listOf(caller, resolvedMethod)
}

data class MethodNotFoundProblem(@SerializedName("method") val method: MethodReference,
                                 @SerializedName("caller") val caller: MethodLocation,
                                 @SerializedName("instruction") val instruction: Instruction) : Problem("method.not.found") {

  override fun shortDescriptionParams(): List<Any> = listOf(method)

  override fun fullDescriptionParams() = listOf(caller, instruction, method)

}

data class MethodNotImplementedProblem(@SerializedName("method") val method: MethodLocation,
                                       @SerializedName("incompleteClass") val incompleteClass: ClassLocation) : Problem("method.not.implemented") {

  override fun shortDescriptionParams(): List<Any> = listOf(method)

  override fun fullDescriptionParams() = listOf(incompleteClass, method.hostClass, method.methodNameAndParameters())
}

data class AbstractMethodInvocationProblem(@SerializedName("method") val method: MethodLocation,
                                           @SerializedName("caller") val caller: MethodLocation,
                                           @SerializedName("instruction") val instruction: Instruction) : Problem("abstract.method.invocation") {

  override fun shortDescriptionParams(): List<Any> = listOf(method)

  override fun fullDescriptionParams() = listOf(caller, instruction, method)

}

data class OverridingFinalMethodProblem(@SerializedName("method") val method: MethodLocation,
                                        @SerializedName("invalidClass") val invalidClass: ClassLocation) : Problem("overriding.final.method") {

  override fun shortDescriptionParams(): List<Any> = listOf(method)

  override fun fullDescriptionParams() = listOf(invalidClass, method)
}

data class NonStaticAccessOfStaticFieldProblem(@SerializedName("field") val field: FieldLocation,
                                               @SerializedName("accessor") val accessor: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem("non.static.access.of.static.field") {

  override fun shortDescriptionParams(): List<Any> = listOf(instruction, field)

  override fun fullDescriptionParams() = listOf(accessor, instruction, field)

}

data class InvokeStaticOnNonStaticMethodProblem(@SerializedName("resolvedMethod") val resolvedMethod: MethodLocation,
                                                @SerializedName("caller") val caller: MethodLocation) : Problem("invoke.static.on.non.static.method") {

  override fun shortDescriptionParams(): List<Any> = listOf(resolvedMethod)

  override fun fullDescriptionParams() = listOf(caller, resolvedMethod)
}

data class InvokeNonStaticInstructionOnStaticMethodProblem(@SerializedName("resolvedMethod") val resolvedMethod: MethodLocation,
                                                           @SerializedName("caller") val caller: MethodLocation,
                                                           @SerializedName("instruction") val instruction: Instruction) : Problem("invoke.non.static.instruction.on.static.method") {

  override fun shortDescriptionParams(): List<Any> = listOf(instruction, resolvedMethod)

  override fun fullDescriptionParams() = listOf(caller, instruction, resolvedMethod)
}

data class StaticAccessOfNonStaticFieldProblem(@SerializedName("field") val field: FieldLocation,
                                               @SerializedName("accessor") val accessor: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem("static.access.of.non.static.field") {

  override fun shortDescriptionParams(): List<Any> = listOf(instruction, field)

  override fun fullDescriptionParams() = listOf(accessor, instruction, field)
}

data class InvalidClassFileProblem(@SerializedName("brokenClass") val brokenClass: ClassReference,
                                   @SerializedName("usage") val usage: Location,
                                   @SerializedName("reason") val reason: String) : Problem("invalid.class.file") {
  override fun shortDescriptionParams(): List<Any> = listOf(brokenClass)

  override fun fullDescriptionParams(): List<Any> = listOf(brokenClass, usage, reason)

}