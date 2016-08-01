package com.jetbrains.pluginverifier.verifiers.field;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VContext;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public class FieldTypeVerifier implements FieldVerifier {
  public void verify(final ClassNode clazz, final FieldNode field, final Resolver resolver, final VContext ctx) {
    final String className = VerifierUtil.extractClassNameFromDescr(field.desc);

    if (className == null || VerifierUtil.classExistsOrExternal(ctx, resolver, className)) {
      return;
    }

    ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromField(clazz.name, field.name));
  }
}
