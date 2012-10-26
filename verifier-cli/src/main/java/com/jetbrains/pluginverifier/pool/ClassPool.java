package com.jetbrains.pluginverifier.pool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Dennis.Ushakov
 */
public interface ClassPool {
  @Nullable
  ClassNode getClassNode(String className);

  @Nullable
  String getClassLocationMoniker(String className);

  Collection<String> getAllClasses();

  @NotNull
  String getMoniker();

  boolean isEmpty();

  public static final ClassPool EMPTY = new ClassPool() {
    @Nullable
    @Override
    public ClassNode getClassNode(String className) {
      return null;
    }

    @Nullable
    @Override
    public String getClassLocationMoniker(String className) {
      return null;
    }

    @Override
    public Collection<String> getAllClasses() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public String getMoniker() {
      return "EmptyClassPool";
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public String toString() {
      return getMoniker();
    }
  };
}
