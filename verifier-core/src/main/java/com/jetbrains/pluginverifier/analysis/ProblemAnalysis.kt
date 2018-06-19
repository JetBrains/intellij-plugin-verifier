package com.jetbrains.pluginverifier.analysis

import com.jetbrains.pluginverifier.results.problems.*

/**
 * Returns name of the class that induces the problem.
 *
 * It can be used to determine if this [CompatibilityProblem]
 * must be reported based on the class's package.
 */
fun CompatibilityProblem.getClassInducingProblem(): String? {
  return when (this) {
    is AbstractClassInstantiationProblem -> abstractClass.className
    is AbstractMethodInvocationProblem -> method.hostClass.className
    is ChangeFinalFieldProblem -> field.hostClass.className
    is ClassNotFoundProblem -> unresolved.className
    is FieldNotFoundProblem -> unresolvedField.hostClass.className
    is IllegalClassAccessProblem -> unavailableClass.className
    is IllegalFieldAccessProblem -> inaccessibleField.hostClass.className
    is IllegalMethodAccessProblem -> inaccessibleMethod.hostClass.className
    is InheritFromFinalClassProblem -> finalClass.className
    is InterfaceInstantiationProblem -> interfaze.className
    is InvalidClassFileProblem -> invalidClass.className
    is FailedToReadClassFileProblem -> failedClass.className
    is InvokeClassMethodOnInterfaceProblem -> changedClass.className
    is InvokeInterfaceMethodOnClassProblem -> changedInterface.className
    is InvokeInterfaceOnPrivateMethodProblem -> resolvedMethod.hostClass.className
    is InvokeInstanceInstructionOnStaticMethodProblem -> resolvedMethod.hostClass.className
    is InvokeStaticOnInstanceMethodProblem -> resolvedMethod.hostClass.className
    is MethodNotFoundProblem -> unresolvedMethod.hostClass.className
    is MethodNotImplementedProblem -> abstractMethod.hostClass.className
    is MultipleDefaultImplementationsProblem -> methodReference.hostClass.className
    is InstanceAccessOfStaticFieldProblem -> field.hostClass.className
    is OverridingFinalMethodProblem -> finalMethod.hostClass.className
    is StaticAccessOfInstanceFieldProblem -> field.hostClass.className
    is SuperClassBecameInterfaceProblem -> interfaze.className
    is SuperInterfaceBecameClassProblem -> clazz.className
    else -> null
  }
}