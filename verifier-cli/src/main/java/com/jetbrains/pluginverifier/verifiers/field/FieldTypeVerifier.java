package com.jetbrains.pluginverifier.verifiers.field;

import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public class FieldTypeVerifier implements FieldVerifier {
  public void verify(final ClassNode clazz, final FieldNode field, final Resolver resolver, final Consumer<Problem> register) {
    final String className = field.desc;
    if(className == null || VerifierUtil.isNativeType(className) ||
        VerifierUtil.classExists(resolver, className)) return;

    ClassNotFoundProblem problem = new ClassNotFoundProblem();
    ProblemLocation location = new ProblemLocation(clazz.name);
    location.setFieldName(field.name);
    problem.setLocation(location);
    problem.setUnknownClass(className);

    register.consume(problem);
  }
}
