package com.jetbrains.pluginverifier.resolvers;

import org.objectweb.asm.tree.ClassNode;

public interface Resolver {
  ClassNode findClass(String className);
  String getClassLocationMoniker(String className);
}

