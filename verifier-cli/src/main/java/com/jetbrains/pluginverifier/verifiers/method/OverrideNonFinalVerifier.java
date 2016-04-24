package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.OverridingFinalMethodProblem;
import com.jetbrains.pluginverifier.verifiers.util.ResolverUtil;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static com.jetbrains.pluginverifier.utils.LocationUtils.getMethodLocation;

/**
 * @author Dennis.Ushakov
 */
public class OverrideNonFinalVerifier implements MethodVerifier {

  //TODO: add the following use-case:
  // static or private method overrides instance method (overriding abstract method is already processed in AbstractVerifier)

  public void verify(final ClassNode clazz, final MethodNode method, final Resolver resolver, final VerificationContext ctx) {
    if ((method.access & Opcodes.ACC_PRIVATE) != 0) return;
    final String superClass = clazz.superName;
    final ResolverUtil.MethodLocation superMethod = ResolverUtil.findMethod(resolver, superClass, method.name, method.desc, ctx);
    if (superMethod == null) return;

    ClassNode classNode = superMethod.getClassNode();
    MethodNode methodNode = superMethod.getMethodNode();

    if (VerifierUtil.isFinal(methodNode) && !VerifierUtil.isAbstract(methodNode)) {
      ctx.registerProblem(new OverridingFinalMethodProblem(getMethodLocation(classNode, methodNode)), ProblemLocation.fromMethod(clazz.name, method));
    }
  }
}
