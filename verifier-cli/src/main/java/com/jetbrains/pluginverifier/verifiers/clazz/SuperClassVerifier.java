package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.MethodNotImplementedProblem;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class SuperClassVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final VerificationContext ctx) {
    final String superClassName = clazz.superName;
    if(!VerifierUtil.classExists(resolver, superClassName, false))  {
      ctx.registerProblem(new ClassNotFoundProblem(clazz.name, superClassName));
      return;
    }

    if ((clazz.access & Opcodes.ACC_ABSTRACT) != 0)
      return;

    final ClassNode superClass = resolver.findClass(superClassName);
    if (superClass == null) return;

    mmm:
    for (Object o : superClass.methods) {
      final MethodNode abstractMethod = (MethodNode)o;
      if (VerifierUtil.isAbstract(abstractMethod)) {
        for (Object o2 : clazz.methods) {
          final MethodNode implMethod = (MethodNode)o2;
          if (abstractMethod.name.equals(implMethod.name) && abstractMethod.desc.equals(abstractMethod.desc)) {
            continue mmm;
          }
        }

        ctx.registerProblem(new MethodNotImplementedProblem(clazz.name, superClassName + '#' + abstractMethod.name + abstractMethod.desc));
      }
    }
  }
}
