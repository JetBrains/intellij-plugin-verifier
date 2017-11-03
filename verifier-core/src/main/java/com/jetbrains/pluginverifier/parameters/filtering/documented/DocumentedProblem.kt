package com.jetbrains.pluginverifier.parameters.filtering.documented

import com.jetbrains.pluginverifier.results.problems.*

/**
 * The documented problems are described on the
 * [Breaking API Changes page](http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 * These problems should not be reported in the verification results.
 *
 * @see [PR-1140](https://youtrack.jetbrains.com/issue/PR-1140)
 * @author Sergey Patrikeev
 */
interface DocumentedProblem {
  fun isDocumenting(problem: Problem): Boolean
}

/**
 * <class name> class removed
 */
data class DocClassRemoved(val className: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is ClassNotFoundProblem && problem.unresolved.className == className
}

/**
 * <class name>.<method name> method removed
 */
data class DocMethodRemoved(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is MethodNotFoundProblem && problem.method.hostClass.className == hostClass && problem.method.methodName == methodName
}

/**
 * <class name>.<method name> method return type changed from <before> to <after>
 */
data class DocMethodReturnTypeChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is MethodNotFoundProblem && problem.method.hostClass.className == hostClass && problem.method.methodName == methodName
}

/**
 * <class name>.<method name> method visibility changed from <before> to <after>
 */
data class DocMethodVisibilityChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is IllegalMethodAccessProblem && problem.method.hostClass.className == hostClass && problem.method.methodName == methodName
}

/**
 * <class name>.<method name> method parameter type changed from <before> to <after>
 */
data class DocMethodParameterTypeChanged(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is MethodNotFoundProblem && problem.method.hostClass.className == hostClass && problem.method.methodName == methodName
}

/**
 * <class name>.<field name> field removed
 */
data class DocFieldRemoved(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is FieldNotFoundProblem && problem.field.hostClass.className == hostClass && problem.field.fieldName == fieldName
}

/**
 * <class name>.<field name> field type changed from <before> to <after>
 */
data class DocFieldTypeChanged(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is FieldNotFoundProblem && problem.field.hostClass.className == hostClass && problem.field.fieldName == fieldName
}

/**
 * <class name>.<field name> field visibility changed from <before> to <after>
 */
data class DocFieldVisibilityChanged(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is IllegalFieldAccessProblem && problem.field.hostClass.className == hostClass && problem.field.fieldName == fieldName
}

/**
 * <package name> package removed
 */
data class DocPackageRemoved(val packageName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is ClassNotFoundProblem && problem.unresolved.className.startsWith(packageName + "/")
}

/**
 * <class name>.<method name> abstract method added
 */
data class DocAbstractMethodAdded(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is MethodNotImplementedProblem && problem.abstractMethod.hostClass.className == hostClass && problem.abstractMethod.methodName == methodName
}

/**
 * <class name> class moved to package <package name>
 */
data class DocClassMovedToPackage(val oldClassName: String, val newPackageName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is ClassNotFoundProblem && problem.unresolved.className == oldClassName
}