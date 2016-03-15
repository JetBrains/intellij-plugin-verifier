package com.jetbrains.pluginverifier.verifiers.field;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public class FieldTypeVerifier implements FieldVerifier {
  public void verify(final ClassNode clazz, final FieldNode field, final Resolver resolver, final VerificationContext ctx) throws VerificationError {
    final String className = VerifierUtil.extractClassNameFromDescr(field.desc);

    if (className == null || VerifierUtil.classExists(ctx.getVerifierOptions(), resolver, className)) {
      return;
    }

    ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromField(clazz.name, field.name));
  }
}
