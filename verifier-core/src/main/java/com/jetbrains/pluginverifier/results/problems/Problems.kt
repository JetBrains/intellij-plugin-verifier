package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.reference.FieldReference
import com.jetbrains.pluginverifier.reference.MethodReference

data class MultipleDefaultImplementationsProblem(val caller: MethodLocation,
                                                 val methodReference: MethodReference,
                                                 val instruction: Instruction,
                                                 val implementation1: MethodLocation,
                                                 val implementation2: MethodLocation) : Problem("multiple.default.implementations") {

  override val shortDescription = short(methodReference)

  override val fullDescription = full(caller, instruction, methodReference, implementation1, implementation2)

}

data class IllegalClassAccessProblem(val unavailableClass: ClassLocation,
                                     val access: AccessType,
                                     val usage: Location) : Problem("illegal.class.access") {

  override val shortDescription = short(access, unavailableClass)

  override val fullDescription: String
    get() {
      val type = if (unavailableClass.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "interface" else "class"
      return full(access.toString().capitalize(), type, unavailableClass, usage)
    }
}

data class AbstractClassInstantiationProblem(val abstractClass: ClassLocation,
                                             val creator: MethodLocation) : Problem("abstract.class.instantiation") {

  override val shortDescription = short(abstractClass)

  override val fullDescription: String = full(creator, abstractClass)

}

data class ClassNotFoundProblem(val unresolved: ClassReference,
                                val usage: Location) : Problem("class.not.found") {

  override val shortDescription = short(unresolved)

  override val fullDescription: String
    get() {
      val type = when (usage) {
        is ClassLocation -> "Class"
        is MethodLocation -> "Method"
        is FieldLocation -> "Field"
        else -> throw IllegalArgumentException()
      }
      return full(type, usage, unresolved)
    }
}

data class SuperInterfaceBecameClassProblem(val child: ClassLocation,
                                            val clazz: ClassLocation) : Problem("super.interface.became.class") {

  override val shortDescription = short(clazz)

  override val fullDescription: String
    get() {
      val type = if (child.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "Interface" else "Class"
      return full(type, child, clazz)
    }

}

data class InheritFromFinalClassProblem(val child: ClassLocation,
                                        val finalClass: ClassLocation) : Problem("inherit.from.final.class") {

  override val shortDescription = short(finalClass)

  override val fullDescription: String
    get() {
      val type = if (child.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "Interface" else "Class"
      return full(type, child, finalClass)
    }
}

data class SuperClassBecameInterfaceProblem(val child: ClassLocation,
                                            val interfaze: ClassLocation) : Problem("super.class.became.interface") {

  override val shortDescription = short(interfaze)

  override val fullDescription = full(child, interfaze)

}

data class InvokeClassMethodOnInterfaceProblem(val methodReference: MethodReference,
                                               val caller: MethodLocation,
                                               val instruction: Instruction) : Problem("invoke.class.method.on.interface") {

  override val shortDescription = short(methodReference.hostClass)

  override val fullDescription = full(caller, instruction, methodReference, methodReference.hostClass)

}

data class InvokeInterfaceMethodOnClassProblem(val methodReference: MethodReference,
                                               val caller: MethodLocation,
                                               val instruction: Instruction) : Problem("invoke.interface.method.on.class") {

  override val shortDescription = short(methodReference.hostClass)

  override val fullDescription = full(caller, instruction, methodReference, methodReference.hostClass)

}

data class InterfaceInstantiationProblem(val interfaze: ClassLocation,
                                         val creator: MethodLocation) : Problem("interface.instantiation") {

  override val shortDescription = short(interfaze)

  override val fullDescription = full(creator, interfaze)

}

data class ChangeFinalFieldProblem(val field: FieldLocation,
                                   val accessor: MethodLocation,
                                   val instruction: Instruction) : Problem("change.final.field") {

  override val shortDescription = short(field)

  override val fullDescription = full(accessor, instruction, field)

}

data class FieldNotFoundProblem(val field: FieldReference,
                                val accessor: MethodLocation,
                                val instruction: Instruction) : Problem("field.not.found") {

  override val shortDescription = short(field)

  override val fullDescription = full(accessor, instruction, field)
}

data class IllegalFieldAccessProblem(val field: FieldLocation,
                                     val accessor: MethodLocation,
                                     val instruction: Instruction,
                                     val fieldAccess: AccessType) : Problem("illegal.field.access") {

  override val shortDescription = short(fieldAccess, field)

  override val fullDescription = full(accessor, instruction, fieldAccess, field, accessor.hostClass)

}

data class IllegalMethodAccessProblem(val method: MethodLocation,
                                      val caller: MethodLocation,
                                      val instruction: Instruction,
                                      val methodAccess: AccessType) : Problem("illegal.method.access") {

  override val shortDescription = short(methodAccess, method)

  override val fullDescription = full(caller, instruction, methodAccess, method, caller.hostClass)
}

data class InvokeInterfaceOnPrivateMethodProblem(val resolvedMethod: MethodLocation,
                                                 val caller: MethodLocation) : Problem("invoke.interface.on.private.method") {

  override val shortDescription = short(resolvedMethod)

  override val fullDescription = full(caller, resolvedMethod)
}

data class MethodNotFoundProblem(val method: MethodReference,
                                 val caller: MethodLocation,
                                 val instruction: Instruction) : Problem("method.not.found") {

  override val shortDescription = short(method)

  override val fullDescription = full(caller, instruction, method)

}

data class MethodNotImplementedProblem(val method: MethodLocation,
                                       val incompleteClass: ClassLocation) : Problem("method.not.implemented") {

  override val shortDescription = short(method)

  override val fullDescription = full(incompleteClass, method.hostClass, method.methodNameAndParameters())
}

data class AbstractMethodInvocationProblem(val method: MethodLocation,
                                           val caller: MethodLocation,
                                           val instruction: Instruction) : Problem("abstract.method.invocation") {

  override val shortDescription = short(method)

  override val fullDescription = full(caller, instruction, method)

}

data class OverridingFinalMethodProblem(val method: MethodLocation,
                                        val invalidClass: ClassLocation) : Problem("overriding.final.method") {

  override val shortDescription = short(method)

  override val fullDescription = full(invalidClass, method)
}

data class NonStaticAccessOfStaticFieldProblem(val field: FieldLocation,
                                               val accessor: MethodLocation,
                                               val instruction: Instruction) : Problem("non.static.access.to.static.field") {

  override val shortDescription = short(instruction, field)

  override val fullDescription = full(accessor, instruction, field)

}

data class InvokeStaticOnNonStaticMethodProblem(val resolvedMethod: MethodLocation,
                                                val caller: MethodLocation) : Problem("invoke.static.on.non.static.method") {

  override val shortDescription = short(resolvedMethod)

  override val fullDescription = full(caller, resolvedMethod)
}

data class InvokeNonStaticInstructionOnStaticMethodProblem(val resolvedMethod: MethodLocation,
                                                           val caller: MethodLocation,
                                                           val instruction: Instruction) : Problem("invoke.non.static.instruction.on.static.method") {

  override val shortDescription = short(instruction, resolvedMethod)

  override val fullDescription = full(caller, instruction, resolvedMethod)
}

data class StaticAccessOfNonStaticFieldProblem(val field: FieldLocation,
                                               val accessor: MethodLocation,
                                               val instruction: Instruction) : Problem("static.access.to.non.static.field") {

  override val shortDescription = short(instruction, field)

  override val fullDescription = full(accessor, instruction, field)
}

data class InvalidClassFileProblem(val brokenClass: ClassReference,
                                   val usage: Location,
                                   val reason: String) : Problem("invalid.class.file") {
  override val shortDescription = short(brokenClass)

  override val fullDescription = full(brokenClass, usage, reason)

}