package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VContext;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.OverridingFinalMethodProblem;
import com.jetbrains.pluginverifier.utils.LocationUtils;
import com.jetbrains.pluginverifier.utils.ResolverUtil;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;


/**
 * @author Dennis.Ushakov
 */
public class OverrideNonFinalVerifier implements MethodVerifier {

  //TODO: add the following use-case:
  // static or private method overrides instance method (overriding abstract method is already processed in AbstractVerifier)

  public void verify(final ClassNode clazz, final MethodNode method, final Resolver resolver, final VContext ctx) {
    if (VerifierUtil.isPrivate(method)) return;

    /*
    According to JVM 8 specification the static methods cannot <i>override</i> the parent methods.
    They can only <i>hide</i> them. Java compiler prohibits <i>hiding</i> the final static methods of the parent,
    but Java Virtual Machine (at least the 8-th version) allows to invoke such methods and doesn't complain
    during the class-file verification
     */
    if (VerifierUtil.isStatic(method)) return;

    final String superClass = clazz.superName;

    if (superClass == null || superClass.startsWith("[") || ctx.getVerifierOptions().isExternalClass(superClass)) {
      return;
    }

    ClassNode superNode = VerifierUtil.findClass(resolver, superClass, ctx);
    if (superNode == null) {
      ctx.registerProblem(new ClassNotFoundProblem(superClass), ProblemLocation.fromMethod(clazz.name, method));
      return;
    }

    ResolverUtil.MethodLocation superMethod = ResolverUtil.findMethod(resolver, superNode, method.name, method.desc, ctx);
    if (superMethod == null) {
      return;
    }

    ClassNode classNode = superMethod.getClassNode();
    MethodNode methodNode = superMethod.getMethodNode();

    if (VerifierUtil.isFinal(methodNode) && !VerifierUtil.isAbstract(methodNode)) {
      ctx.registerProblem(new OverridingFinalMethodProblem(LocationUtils.INSTANCE.getMethodLocation(classNode, methodNode)), ProblemLocation.fromMethod(clazz.name, method));
    }
  }
}
