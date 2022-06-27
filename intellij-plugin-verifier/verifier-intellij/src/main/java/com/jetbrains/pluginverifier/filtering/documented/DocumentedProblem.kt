/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering.documented

import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.isSubclassOf
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf

/**
 * The documented problems are described on the
 * [Breaking API Changes page](https://plugins.jetbrains.com/docs/intellij/api-changes-list.html).
 * These problems should not be reported in the verification results.
 *
 * @see [PR-1140](https://youtrack.jetbrains.com/issue/PR-1140)
 */
interface DocumentedProblem {
  fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean
}

/**
 * <class name> class removed
 */
data class DocClassRemoved(val className: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext) =
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
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is MethodNotFoundProblem
      && problem.unresolvedMethod.methodName == methodName
      && context.classResolver.isSubclassOrSelf(problem.unresolvedMethod.hostClass.className, hostClass)
}

/**
 * <class name>.<method name> method return type changed from <before> to <after>
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if the return type of method 'foo' of type A was changed, problem 'method B.foo is not found' will not be reported
 */
data class DocMethodReturnTypeChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is MethodNotFoundProblem
      && problem.unresolvedMethod.methodName == methodName
      && context.classResolver.isSubclassOrSelf(problem.unresolvedMethod.hostClass.className, hostClass)
      ||
      problem is MethodNotImplementedProblem
      && problem.abstractMethod.methodName == methodName
      && problem.abstractMethod.hostClass.className == hostClass
}

/**
 * <class name>.<method name> method visibility changed from <before> to <after>
 */
data class DocMethodVisibilityChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is IllegalMethodAccessProblem && problem.inaccessibleMethod.hostClass.className == hostClass && problem.inaccessibleMethod.methodName == methodName
}

/**
 * <class name>.<method name> method marked final
 */
data class DocMethodMarkedFinal(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is OverridingFinalMethodProblem && problem.finalMethod.hostClass.className == hostClass && problem.finalMethod.methodName == methodName
}

/**
 * <class name> class|interface now extends|implements <class name> and inherits its final method <method name>
 */
data class DocFinalMethodInherited(val changedClass: String, val newParent: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is OverridingFinalMethodProblem
      && problem.finalMethod.methodName == methodName
      && problem.finalMethod.hostClass.className == newParent
      && context.classResolver.isSubclassOrSelf(problem.invalidClass.className, changedClass)
      && context.classResolver.isSubclassOf(changedClass, newParent)
}

/**
 * <class name>.<method name> method parameter type changed from <before> to <after>
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if the parameter type of method 'foo' of type A was changed, problem 'method B.foo is not found' will not be reported
 */
data class DocMethodParameterTypeChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is MethodNotFoundProblem
      && problem.unresolvedMethod.methodName == methodName
      && context.classResolver.isSubclassOrSelf(problem.unresolvedMethod.hostClass.className, hostClass)
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
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is FieldNotFoundProblem
      && problem.unresolvedField.fieldName == fieldName
      && context.classResolver.isSubclassOrSelf(problem.unresolvedField.hostClass.className, hostClass)
}

/**
 * <property name> property removed from resource bundle <bundle name>
 */
data class DocPropertyRemoved(val propertyName: String, val bundleName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is MissingPropertyReferenceProblem
      && problem.propertyKey == propertyName
      && problem.bundleBaseName == bundleName
}

/**
 * <class name>.<field name> field type changed from <before> to <after>
 *
 * It supports the following case: given two types A and B such that B derives from A
 * if the field type of a field 'x' of type A was changed, problem 'field B.x is not found' will not be reported
 */
data class DocFieldTypeChanged(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is FieldNotFoundProblem
      && problem.unresolvedField.fieldName == fieldName
      && context.classResolver.isSubclassOrSelf(problem.unresolvedField.hostClass.className, hostClass)
}

/**
 * <class name>.<field name> field visibility changed from <before> to <after>
 */
data class DocFieldVisibilityChanged(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is IllegalFieldAccessProblem && problem.inaccessibleField.hostClass.className == hostClass && problem.inaccessibleField.fieldName == fieldName
}

/**
 * <package name> package removed
 */
data class DocPackageRemoved(val packageName: String) : DocumentedProblem {

  private fun String.belongsToPackage() = startsWith("$packageName/")

  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext) =
    when (problem) {
      is PackageNotFoundProblem -> {
        problem.packageName == packageName
          || problem.packageName.belongsToPackage()
          || problem.classNotFoundProblems.all { it.unresolved.className.belongsToPackage() }
      }
      is ClassNotFoundProblem -> problem.unresolved.className.belongsToPackage()
      is MethodNotFoundProblem -> problem.unresolvedMethod.doesMethodDependOnClass { it.belongsToPackage() }
      is FieldNotFoundProblem -> problem.unresolvedField.doesFieldDependOnClass { it.belongsToPackage() }
      else -> false
    }
}

/**
 * <class name>.<method name> abstract method added
 */
data class DocAbstractMethodAdded(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean =
    problem is MethodNotImplementedProblem
      && problem.abstractMethod.methodName == methodName
      && context.classResolver.isSubclassOrSelf(problem.incompleteClass.className, hostClass)
}

/**
 * <class name> class moved to package <package name>
 */
data class DocClassMovedToPackage(val oldClassName: String, val newPackageName: String) : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext) =
    when (problem) {
      is ClassNotFoundProblem -> problem.unresolved.className == oldClassName
      is MethodNotFoundProblem -> problem.unresolvedMethod.doesMethodDependOnClass { it == oldClassName }
      is FieldNotFoundProblem -> problem.unresolvedField.doesFieldDependOnClass { it == oldClassName }
      else -> false
    }
}

/**
 * Inheritors of this class are present in Plugin Verifier only to verify
 * that documented problems page does not contain non-recognizable problems descriptions.
 * Plugin Verifier does not use them to exclude any problems (yet).
 */
abstract class NoOpValidatingDocumentedProblem : DocumentedProblem {
  override fun isDocumenting(problem: CompatibilityProblem, context: VerificationContext): Boolean = false
}

/**
 * <class name>.<method name> method <class name> parameter marked <class name>
 */
data class DocMethodParameterMarkedWithAnnotation(
  val hostClass: String,
  val methodName: String,
  val parameterClassName: String,
  val annotationName: String
) : NoOpValidatingDocumentedProblem()

/**
 * <class name> class type parameter <name> added
 */
data class DocClassTypeParameterAdded(val className: String) : NoOpValidatingDocumentedProblem()

/**
 * <class name> superclass change from <class name> to <class name>
 */
data class DocSuperclassChanged(val className: String, val oldSuperClassName: String, val newSuperClassName: String) : NoOpValidatingDocumentedProblem()

/**
 * <class name> class now interface
 */
data class DocClassNowInterface(val className: String) : NoOpValidatingDocumentedProblem()

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
 * a class that matches the passed predicate.
 */
private fun MethodReference.doesMethodDependOnClass(classPredicate: (String) -> Boolean): Boolean {
  if (classPredicate(hostClass.className)) {
    return true
  }
  val (rawParamTypes, rawReturnType) = JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
  val paramClasses = rawParamTypes.mapNotNull { it.extractClassNameFromDescriptor() }
  val returnType = rawReturnType.extractClassNameFromDescriptor()

  return paramClasses.any(classPredicate) || (returnType != null && classPredicate(returnType))
}

/**
 * Checks if the field's signature of _this_ [FieldReference] contains
 * a class that matches the passed predicate.
 */
private fun FieldReference.doesFieldDependOnClass(classFinder: (String) -> Boolean): Boolean {
  if (classFinder(hostClass.className)) {
    return true
  }
  val fieldType = fieldDescriptor.extractClassNameFromDescriptor()
  return fieldType != null && classFinder(fieldType)
}

