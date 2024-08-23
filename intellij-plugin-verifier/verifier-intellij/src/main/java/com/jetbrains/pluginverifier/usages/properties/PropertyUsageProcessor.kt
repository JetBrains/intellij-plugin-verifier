/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.problems.MissingPropertyReferenceProblem
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.CodeAnalysis
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.bytecode.InterpreterAdapter
import com.jetbrains.pluginverifier.verifiers.bytecode.InvokeSpecialInterpreterListener
import com.jetbrains.pluginverifier.verifiers.bytecode.StringValue
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.getAnnotationValue
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicValue
import java.util.*

class PropertyUsageProcessor : ApiUsageProcessor {

  private val enumClassPropertyUsage = EnumClassPropertyUsageAdapter()

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    println(callerMethod.location.toString() + " " + callerMethod.name)
    if (enumClassPropertyUsage.supports(resolvedMethod)) {
      if (callerMethod is MethodAsm) {
        val invokeSpecialDetector = InvokeSpecialInterpreterListener()
        val a: Analyzer<BasicValue> = Analyzer(InterpreterAdapter(invokeSpecialDetector))
        val f = a.analyze("PropertyUsageProcessor", callerMethod.asmNode)
        val z= callerMethod.asmNode.instructions.zip(f)
        z
        enumClassPropertyUsage.resolve(resolvedMethod)?.run {
          invokeSpecialDetector.invocations.filter {
            it.methodName == "<init>" && it.desc == "(Ljava/lang/String;ILjava/lang/String;)V"
          }.forEach { constructorInvocation ->
            val invocationParameteres = constructorInvocation.values.drop(3) // 1) invocation target 2) enum member name 3) enum ordinal value
            // TODO support more parameters
            invocationParameteres.firstStringOrNull()?.let { propertyKey ->
              checkProperty(bundleName, propertyKey, context, callerMethod.location)
            }
          }
        }
      }
      return
    }

    val methodParameters = resolvedMethod.methodParameters
    if (methodParameters.any { it.name.contains("default", true) }) {
      //Some resource bundle methods provide default value parameter, which is used if such property is not available in the bundle.
      return
    }
    for ((parameterIndex, methodParameter) in methodParameters.withIndex()) {
      val propertyKeyAnnotation = methodParameter.annotations.findAnnotation("org/jetbrains/annotations/PropertyKey")
        ?: continue

      val resourceBundleName = propertyKeyAnnotation.getAnnotationValue("resourceBundle") as? String ?: continue

      val instructions = callerMethod.instructions
      val instructionIndex = instructions.indexOf(instructionNode)

      val onStackIndex = methodParameters.size - 1 - parameterIndex
      val propertyKey = CodeAnalysis().evaluateConstantString(callerMethod, instructionIndex, onStackIndex)

      if (propertyKey != null) {
        checkProperty(resourceBundleName, propertyKey, context, callerMethod.location)
      }
    }
  }

  private fun checkProperty(
    resourceBundleName: String,
    propertyKey: String,
    context: VerificationContext,
    usageLocation: Location
  ) {
    if (resourceBundleName != getBundleBaseName(resourceBundleName)) {
      //In general, we can't resolve non-base bundles, like "some.Bundle_en" because we don't know the locale to use.
      return
    }

    val resolutionResult = context.classResolver.resolveExactPropertyResourceBundle(resourceBundleName, Locale.ROOT)
    if (resolutionResult !is ResolutionResult.Found) {
      return
    }

    val resourceBundle = resolutionResult.value
    if (!resourceBundle.containsKey(propertyKey)) {
      // MP-3201: Don't report warnings about properties which were moved to *DeprecatedMessagesBundle files
      val deprecatedBundleNames = context.classResolver.allBundleNameSet.baseBundleNames
        .filter { it.endsWith("DeprecatedMessagesBundle") }
      for (deprecatedBundleName in deprecatedBundleNames) {
        val resolution = context.classResolver.resolveExactPropertyResourceBundle(deprecatedBundleName, Locale.ROOT)
        if (resolution is ResolutionResult.Found) {
          val deprecatedBundle = resolution.value
          if (deprecatedBundle.containsKey(propertyKey)) {
            context.warningRegistrar.registerCompatibilityWarning(
              DeprecatedPropertyUsageWarning(propertyKey, resourceBundleName, deprecatedBundleName, usageLocation)
            )
            return
          }
        }
      }

      context.problemRegistrar.registerProblem(MissingPropertyReferenceProblem(propertyKey, resourceBundleName, usageLocation))
    }
  }

  private fun List<BasicValue>.firstStringOrNull(): String? =
    filterIsInstance<StringValue>().firstOrNull()?.value

}

