package com.jetbrains.pluginverifier.verifiers.field;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public class FieldTypeVerifier implements FieldVerifier {
  public void verify(final ClassNode clazz, final FieldNode field, final Resolver resolver, final VerificationContext ctx) {
    final String className = VerifierUtil.extractClassNameFromDescr(field.desc);

    if(className == null || VerifierUtil.classExists(ctx.getOptions(), resolver, className)) {
      return;
    }

    ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromField(clazz.name, field.name));
  }
}
