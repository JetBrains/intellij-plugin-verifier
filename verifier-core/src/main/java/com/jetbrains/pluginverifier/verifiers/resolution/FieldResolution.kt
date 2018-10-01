package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.tree.ClassNode

/**
 * Utility class that implements fields resolution strategy,
 * as described in the JVM specification (§5.4.3.2).
 */
class FieldResolution(
    private val fieldName: String,
    private val fieldDescriptor: String,
    private val clsResolver: ClsResolver,
    private val problemRegistrar: ProblemRegistrar
) {

  fun resolveField(currentClass: ClassNode): FieldResolutionResult {
    /**
     * 1) If C declares a field with the name and descriptor specified by the field reference,
     * field lookup succeeds. The declared field is the result of the field lookup.
     */
    val fields = currentClass.getFields().orEmpty()
    val matching = fields.firstOrNull { it.name == fieldName && it.desc == fieldDescriptor }
    if (matching != null) {
      return FieldResolutionResult.Found(currentClass, matching)
    }

    /**
     * 2) Otherwise, field lookup is applied recursively to the direct superinterfaces
     * of the specified class or interface C.
     */
    for (anInterface in currentClass.getInterfaces().orEmpty()) {
      val resolvedInterface = clsResolver.resolveClassOrProblem(anInterface, currentClass, problemRegistrar) { currentClass.createClassLocation() }
          ?: return FieldResolutionResult.Abort

      val lookupResult = resolveField(resolvedInterface)
      when (lookupResult) {
        FieldResolutionResult.NotFound -> Unit
        FieldResolutionResult.Abort -> return lookupResult
        is FieldResolutionResult.Found -> return lookupResult
      }
    }

    /**
     * 3) Otherwise, if C has a superclass S, field lookup is applied recursively to S.
     */
    val superName = currentClass.superName
    if (superName != null) {
      val resolvedSuper = clsResolver.resolveClassOrProblem(superName, currentClass, problemRegistrar) { currentClass.createClassLocation() }
          ?: return FieldResolutionResult.Abort
      val lookupResult = resolveField(resolvedSuper)
      when (lookupResult) {
        FieldResolutionResult.NotFound -> Unit
        FieldResolutionResult.Abort -> return lookupResult
        is FieldResolutionResult.Found -> return lookupResult
      }
    }

    /**
     * 4) Otherwise, field lookup fails.
     */
    return FieldResolutionResult.NotFound
  }

}