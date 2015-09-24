package com.jetbrains.pluginverifier.verifiers.field;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public interface FieldVerifier {
  void verify(ClassNode clazz, FieldNode field, Resolver resolver, VerificationContext ctx);
}
