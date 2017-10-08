package com.jetbrains.pluginverifier.parameters.filtering.documented

import com.jetbrains.pluginverifier.results.problems.*

/**
 * @author Sergey Patrikeev
 */
sealed class DocumentedProblem {

  abstract fun isDocumenting(problem: Problem): Boolean

  data class ClassRemoved(val className: String) : DocumentedProblem() {
    override fun isDocumenting(problem: Problem): Boolean =
        problem is ClassNotFoundProblem && problem.unresolved.className == className
  }

  data class MethodRemoved(val hostClass: String, val methodName: String) : DocumentedProblem() {
    override fun isDocumenting(problem: Problem): Boolean =
        problem is MethodNotFoundProblem && problem.method.hostClass.className == hostClass && problem.method.methodName == methodName
  }

  data class FieldRemoved(val hostClass: String, val fieldName: String) : DocumentedProblem() {
    override fun isDocumenting(problem: Problem): Boolean =
        problem is FieldNotFoundProblem && problem.field.hostClass.className == hostClass && problem.field.fieldName == fieldName
  }

  data class PackageRemoved(val packageName: String) : DocumentedProblem() {
    override fun isDocumenting(problem: Problem): Boolean =
        problem is ClassNotFoundProblem && problem.unresolved.className.startsWith(packageName + "/")
  }

  data class AbstractMethodAdded(val hostClass: String, val methodName: String) : DocumentedProblem() {
    override fun isDocumenting(problem: Problem): Boolean =
        problem is MethodNotImplementedProblem && problem.method.hostClass.className == hostClass && problem.method.methodName == methodName
  }

}