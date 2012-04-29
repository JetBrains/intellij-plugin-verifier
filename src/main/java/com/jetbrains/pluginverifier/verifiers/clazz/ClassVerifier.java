package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import org.objectweb.asm.tree.ClassNode;

public interface ClassVerifier {
  void verify(ClassNode clazz, Resolver resolver, ErrorRegister register);
}
