package com.jetbrains.pluginverifier.problems

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.location.ClassLocation
import com.jetbrains.pluginverifier.location.MethodLocation
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.reference.FieldReference
import com.jetbrains.pluginverifier.reference.MethodReference
import com.jetbrains.pluginverifier.reference.SymbolicReference

/**
 * @author Sergey Patrikeev
 */
interface Problem {
  fun getDescription(): String

  fun effect(): String = ""
}

//TODO: add a human-readable effect, e.g. (non-static -> static field) : 	A client program may be interrupted by IllegalAccessError exception when attempt to assign new values to the field.
data class MultipleMethodImplementationsProblem(@SerializedName("method") val method: MethodReference,
                                                @SerializedName("availableMethods") val availableMethods: List<MethodLocation>) : Problem {
  constructor(hostClass: String, methodName: String, methodDescriptor: String, availableMethods: List<MethodLocation>) : this(SymbolicReference.methodOf(hostClass, methodName, methodDescriptor), availableMethods)

  override fun getDescription(): String = "multiple default implementations of method $method"
}

data class IllegalClassAccessProblem(@SerializedName("class") val clazz: ClassReference, @SerializedName("access") val classAccess: AccessType) : Problem {
  constructor(className: String, accessType: AccessType) : this(ClassReference(className), accessType)

  override fun getDescription(): String = "illegal access to $classAccess class $clazz"
}

data class IllegalInterfaceAccessProblem(@SerializedName("interface") val interfaze: ClassReference, @SerializedName("access") val classAccess: AccessType) : Problem {
  constructor(interfaceName: String, accessType: AccessType) : this(ClassReference(interfaceName), accessType)

  override fun getDescription(): String = "illegal access to $classAccess interface $interfaze"
}

data class AbstractClassInstantiationProblem(@SerializedName("class") val clazz: ClassReference) : Problem {
  constructor(className: String) : this(ClassReference(className))

  override fun getDescription(): String = "instantiation of an abstract class $clazz"
}

data class ClassNotFoundProblem(@SerializedName("class") val unknownClass: ClassReference) : Problem {
  constructor(className: String) : this(ClassReference(className))

  override fun getDescription(): String = "accessing to unknown class $unknownClass"
}

data class IncompatibleClassToInterfaceChangeProblem(@SerializedName("class") val clazz: ClassReference) : Problem {
  override fun getDescription(): String = "incompatible change of class $clazz to interface"
}

data class IncompatibleInterfaceToClassChangeProblem(@SerializedName("interface") val interfaze: ClassReference) : Problem {
  override fun getDescription(): String = "incompatible change of interface $interfaze to class"
}

data class InheritFromFinalClassProblem(@SerializedName("finalClass") val finalClass: ClassReference) : Problem {
  constructor(className: String) : this(ClassReference(className))

  override fun getDescription(): String = "cannot inherit from final class $finalClass"
}

data class InterfaceInstantiationProblem(@SerializedName("class") val clazz: ClassReference) : Problem {
  constructor(className: String) : this(ClassReference(className))

  override fun getDescription(): String = "instantiation an interface $clazz"
}

data class ChangeFinalFieldProblem(@SerializedName("field") val field: FieldReference) : Problem {
  constructor(hostClass: String, fieldName: String, fieldDescriptor: String) : this(SymbolicReference.fieldOf(hostClass, fieldName, fieldDescriptor))

  override fun getDescription(): String = "attempt to change a final field $field"
}

data class FieldNotFoundProblem(@SerializedName("field") val field: FieldReference) : Problem {
  constructor(hostClass: String, fieldName: String, fieldDescriptor: String) : this(SymbolicReference.fieldOf(hostClass, fieldName, fieldDescriptor))

  override fun getDescription(): String = "accessing to unknown field $field"
}

data class IllegalFieldAccessProblem(@SerializedName("field") val field: FieldReference, @SerializedName("access") val fieldAccess: AccessType) : Problem {
  constructor(hostClass: String, fieldName: String, fieldDescriptor: String, fieldAccess: AccessType) : this(SymbolicReference.fieldOf(hostClass, fieldName, fieldDescriptor), fieldAccess)

  override fun getDescription(): String = "illegal access of $fieldAccess field $field"
}

data class IllegalMethodAccessProblem(@SerializedName("method") val method: MethodReference, @SerializedName("access") val methodAccess: AccessType) : Problem {
  constructor(hostClass: String, methodName: String, methodDescriptor: String, methodAccess: AccessType) : this(SymbolicReference.methodOf(hostClass, methodName, methodDescriptor), methodAccess)

  override fun getDescription(): String = "illegal invocation of $methodAccess method $method"
}

data class InvokeInterfaceOnPrivateMethodProblem(@SerializedName("method") val method: MethodReference) : Problem {
  override fun getDescription(): String = "attempt to perform 'invokeinterface' on private method $method"
}

data class MethodNotFoundProblem(@SerializedName("method") val method: MethodReference) : Problem {
  constructor(hostClass: String, methodName: String, methodDescriptor: String) : this(SymbolicReference.methodOf(hostClass, methodName, methodDescriptor))

  override fun getDescription(): String = "invoking unknown method $method"
}

data class MethodNotImplementedProblem(@SerializedName("method") val method: MethodLocation,
                                       @SerializedName("incompleteClass") val incompleteClass: ClassLocation) : Problem {
  override fun getDescription(): String = "method isn't implemented $method"

  override fun effect() = "Non-abstract class $incompleteClass inherits ${method.hostClass} but doesn't implement the abstract method ${method.methodNameAndParameters()}. This can lead to **AbstractMethodError** exception at runtime."
}

data class AbstractMethodInvocationProblem(@SerializedName("method") val method: MethodReference) : Problem {
  constructor(hostClass: String, methodName: String, methodDescriptor: String) : this(SymbolicReference.methodOf(hostClass, methodName, methodDescriptor))

  override fun getDescription(): String = "attempt to invoke an abstract method $method"
}

data class OverridingFinalMethodProblem(@SerializedName("method") val method: MethodLocation,
                                        @SerializedName("invalidClass") val invalidClass: ClassLocation) : Problem {
  override fun getDescription(): String = "overriding final method $method"

  override fun effect() = "Class $invalidClass overrides the final method $method. This can lead to **VerifyError** exception at runtime."
}

data class InstanceAccessOfStaticFieldProblem(@SerializedName("field") val field: FieldReference) : Problem {
  constructor(hostClass: String, fieldName: String, fieldDescriptor: String) : this(SymbolicReference.fieldOf(hostClass, fieldName, fieldDescriptor))

  override fun getDescription(): String = "attempt to perform instance access on a static field $field"
}

data class InvokeInterfaceOnStaticMethodProblem(@SerializedName("method") val method: MethodReference) : Problem {
  override fun getDescription(): String = "attempt to perform 'invokeinterface' on static method $method"
}

data class InvokeSpecialOnStaticMethodProblem(@SerializedName("method") val method: MethodReference) : Problem {
  override fun getDescription(): String = "attempt to perform 'invokespecial' on static method $method"
}

data class InvokeStaticOnInstanceMethodProblem(@SerializedName("method") val method: MethodReference) : Problem {
  constructor(hostClass: String, methodName: String, methodDescriptor: String) : this(SymbolicReference.methodOf(hostClass, methodName, methodDescriptor))

  override fun getDescription(): String = "attempt to perform 'invokestatic' on an instance method $method"
}

data class InvokeVirtualOnStaticMethodProblem(@SerializedName("method") val method: MethodReference) : Problem {
  constructor(hostClass: String, methodName: String, methodDescriptor: String) : this(SymbolicReference.methodOf(hostClass, methodName, methodDescriptor))

  override fun getDescription(): String = "attempt to perform 'invokevirtual' on static method $method"
}

data class StaticAccessOfInstanceFieldProblem(@SerializedName("field") val field: FieldReference) : Problem {
  constructor(hostClass: String, fieldName: String, fieldDescriptor: String) : this(SymbolicReference.fieldOf(hostClass, fieldName, fieldDescriptor))

  override fun getDescription(): String = "attempt to perform static access on an instance field $field"
}

enum class AccessType constructor(private val type: String) {
  PUBLIC("public"),
  PROTECTED("protected"),
  PACKAGE_PRIVATE("package-private"),
  PRIVATE("private");

  override fun toString(): String = type
}