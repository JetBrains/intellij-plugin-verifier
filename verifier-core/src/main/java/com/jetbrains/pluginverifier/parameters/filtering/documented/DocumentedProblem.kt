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

data class DocClassRemoved(val className: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is ClassNotFoundProblem && problem.unresolved.className == className
}

data class DocMethodRemoved(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is MethodNotFoundProblem && problem.method.hostClass.className == hostClass && problem.method.methodName == methodName
}

data class DocFieldRemoved(val hostClass: String, val fieldName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is FieldNotFoundProblem && problem.field.hostClass.className == hostClass && problem.field.fieldName == fieldName
}

data class DocPackageRemoved(val packageName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is ClassNotFoundProblem && problem.unresolved.className.startsWith(packageName + "/")
}

data class DocAbstractMethodAdded(val hostClass: String, val methodName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is MethodNotImplementedProblem && problem.abstractMethod.hostClass.className == hostClass && problem.abstractMethod.methodName == methodName
}

data class DocClassMovedToPackage(val oldClassName: String, val newPackageName: String) : DocumentedProblem {
  override fun isDocumenting(problem: Problem): Boolean =
      problem is ClassNotFoundProblem && problem.unresolved.className == oldClassName
}