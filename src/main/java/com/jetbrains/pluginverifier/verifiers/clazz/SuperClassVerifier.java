package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.pool.ResolverUtil;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class SuperClassVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final ErrorRegister register) {
    final String className = clazz.superName;
    if(!VerifierUtil.classExists(resolver, className, false))  {
      register.registerError(clazz.name, "super class " + className + " not found");
      return;
    }

    if ((clazz.access & Opcodes.ACC_ABSTRACT) != 0)
      return;

    final ClassNode superClass = resolver.findClass(className);
    for (Object o : superClass.methods) {
      final MethodNode method = (MethodNode)o;
      if (VerifierUtil.isAbstract(method)) {
        final MethodNode impl = ResolverUtil.findMethod(resolver, clazz.name, method.name, method.desc);
        if (impl == null) {
          register.registerError(clazz.name, "abstract method " + method.name + "not implemented");
        }
      }
    }
  }
}
