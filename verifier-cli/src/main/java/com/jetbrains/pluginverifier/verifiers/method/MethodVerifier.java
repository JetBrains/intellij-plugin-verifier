package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public interface MethodVerifier {
  void verify(ClassNode clazz, MethodNode method, Resolver resolver, VerificationContext ctx);
}
