package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.error.VerificationError;
import org.objectweb.asm.tree.ClassNode;

public interface ClassVerifier {
  void verify(ClassNode clazz, Resolver resolver, VerificationContext ctx) throws VerificationError;
}
