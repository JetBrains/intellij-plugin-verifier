package com.jetbrains.pluginverifier.pool;

import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;

/**
 * @author Dennis.Ushakov
 */
public interface ClassPool {
  ClassNode getClassNode(String className);
  String getClassLocationMoniker(String className);
  Collection<String> getAllClasses();
  String getMoniker();

  boolean isEmpty();
}
