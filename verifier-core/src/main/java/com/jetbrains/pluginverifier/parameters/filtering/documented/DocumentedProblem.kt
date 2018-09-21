package com.jetbrains.pluginverifier.parameters.filtering.documented

import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf

/**
 * The documented problems are described on the
 * [Breaking API Changes page](http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 * These problems should not be reported in the verification results.
 *
 * @see [PR-1140](https://youtrack.jetbrains.com/issue/PR-1140)
 * @author Sergey Patrikeev
 */
interface DocumentedProblem {
  fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean
}

/**
 * <class name> class removed
 */
data class DocClassRemoved(val className: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext) =
      when (problem) {
        is ClassNotFoundProblem -> problem.unresolved.className == className
        is MethodNotFoundProblem -> problem.unresolvedMethod.doesMethodDependOnClass(className)
        is FieldNotFoundProblem -> problem.unresolvedField.doesFieldDependOnClass(className)
        else -> false
      }
}

/**
 * <class name>.<method name> method removed
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if a method 'foo' of type A was removed, problem 'method B.foo is not found' will not be reported
 */
data class DocMethodRemoved(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is MethodNotFoundProblem
          && problem.unresolvedMethod.methodName == methodName
          && verificationContext.isSubclassOrSelf(problem.unresolvedMethod.hostClass.className, hostClass)
}

/**
 * <class name>.<method name> method return type changed from <before> to <after>
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if the return type of method 'foo' of type A was changed, problem 'method B.foo is not found' will not be reported
 */
data class DocMethodReturnTypeChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is MethodNotFoundProblem
          && problem.unresolvedMethod.methodName == methodName
          && verificationContext.isSubclassOrSelf(problem.unresolvedMethod.hostClass.className, hostClass)
          ||
          problem is MethodNotImplementedProblem
          && problem.abstractMethod.methodName == methodName
          && problem.abstractMethod.hostClass.className == hostClass
}

/**
 * <class name>.<method name> method visibility changed from <before> to <after>
 */
data class DocMethodVisibilityChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is IllegalMethodAccessProblem && problem.inaccessibleMethod.hostClass.className == hostClass && problem.inaccessibleMethod.methodName == methodName
}

/**
 * <class name>.<method name> method parameter type changed from <before> to <after>
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if the parameter type of method 'foo' of type A was changed, problem 'method B.foo is not found' will not be reported
 */
data class DocMethodParameterTypeChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is MethodNotFoundProblem
          && problem.unresolvedMethod.methodName == methodName
          && verificationContext.isSubclassOrSelf(problem.unresolvedMethod.hostClass.className, hostClass)
          ||
          problem is MethodNotImplementedProblem
          && problem.abstractMethod.methodName == methodName
          && problem.abstractMethod.hostClass.className == hostClass
}

/**
 * <class name>.<field name> field removed
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if a field 'x' of type A was removed, problem 'field B.x is not found' will not be reported
 */
data class DocFieldRemoved(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is FieldNotFoundProblem
          && problem.unresolvedField.fieldName == fieldName
          && verificationContext.isSubclassOrSelf(problem.unresolvedField.hostClass.className, hostClass)
}

/**
 * <class name>.<field name> field type changed from <before> to <after>
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if the field type of a field 'x' of type A was changed, problem 'field B.x is not found' will not be reported
 */
data class DocFieldTypeChanged(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is FieldNotFoundProblem
          && problem.unresolvedField.fieldName == fieldName
          && verificationContext.isSubclassOrSelf(problem.unresolvedField.hostClass.className, hostClass)
}

/**
 * <class name>.<field name> field visibility changed from <before> to <after>
 */
data class DocFieldVisibilityChanged(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is IllegalFieldAccessProblem && problem.inaccessibleField.hostClass.className == hostClass && problem.inaccessibleField.fieldName == fieldName
}

/**
 * <package name> package removed
 */
data class DocPackageRemoved(val packageName: String) : DocumentedProblem {

  private fun String?.isClassInPackage() =
      this?.startsWith(packageName + "/") ?: false

  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext) =
      when (problem) {
        is ClassNotFoundProblem -> problem.unresolved.className.isClassInPackage()
        is MethodNotFoundProblem -> problem.unresolvedMethod.doesMethodDependOnClass { it.isClassInPackage() }
        is FieldNotFoundProblem -> problem.unresolvedField.doesFieldDependOnClass { it.isClassInPackage() }
        else -> false
      }
}

/**
 * <class name>.<method name> abstract method added
 */
data class DocAbstractMethodAdded(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext): Boolean =
      problem is MethodNotImplementedProblem
          && problem.abstractMethod.methodName == methodName
          && verificationContext.isSubclassOrSelf(problem.incompleteClass.className, hostClass)
}

/**
 * <class name> class moved to package <package name>
 */
data class DocClassMovedToPackage(val oldClassName: String, val newPackageName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, verificationContext: VerificationContext) =
      when (problem) {
        is ClassNotFoundProblem -> problem.unresolved.className == oldClassName
        is MethodNotFoundProblem -> problem.unresolvedMethod.doesMethodDependOnClass { it == oldClassName }
        is FieldNotFoundProblem -> problem.unresolvedField.doesFieldDependOnClass { it == oldClassName }
        else -> false
      }
}

/**
 * Checks if the method's signature of _this_ [MethodReference] contains
 * the class [className].
 */
private fun MethodReference.doesMethodDependOnClass(className: String) =
    doesMethodDependOnClass { it == className }

/**
 * Checks if the field's signature of _this_ [FieldReference] contains
 * the class [className].
 */
private fun FieldReference.doesFieldDependOnClass(className: String) =
    doesFieldDependOnClass { it == className }

/**
 * Checks if the method's signature of _this_ [MethodReference] contains
 * a class that matches the passed [predicate] [classFinder].
 */
private fun MethodReference.doesMethodDependOnClass(classFinder: (String) -> Boolean): Boolean {
  if (classFinder(hostClass.className)) {
    return true
  }
  val (rawParamTypes, rawReturnType) = JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
  val paramClasses = rawParamTypes.mapNotNull { it.extractClassNameFromDescr() }
  val returnType = rawReturnType.extractClassNameFromDescr()

  return paramClasses.any(classFinder) || (returnType != null && classFinder(returnType))
}

/**
 * Checks if the field's signature of _this_ [FieldReference] contains
 * a class that matches the passed [predicate] [classFinder].
 */
private fun FieldReference.doesFieldDependOnClass(classFinder: (String) -> Boolean): Boolean {
  if (classFinder(hostClass.className)) {
    return true
  }
  val fieldType = fieldDescriptor.extractClassNameFromDescr()
  return fieldType != null && classFinder(fieldType)
}
