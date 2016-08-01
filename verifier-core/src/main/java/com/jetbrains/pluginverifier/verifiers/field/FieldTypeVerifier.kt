package com.jetbrains.pluginverifier.verifiers.field

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, resolver: Resolver, ctx: VContext) {
    val className = VerifierUtil.extractClassNameFromDescr(field.desc)

    if (className == null || VerifierUtil.classExistsOrExternal(ctx, resolver, className)) {
      return
    }

    ctx.registerProblem(ClassNotFoundProblem(className), ProblemLocation.fromField(clazz.name, field.name))
  }
}
