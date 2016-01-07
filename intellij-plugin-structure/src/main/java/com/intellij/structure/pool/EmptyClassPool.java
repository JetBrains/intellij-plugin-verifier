package com.intellij.structure.pool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey Patrikeev
 */
public class EmptyClassPool implements ClassPool {

  public static final ClassPool INSTANCE = new EmptyClassPool();

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) {
    return null;
  }

  @Nullable
  @Override
  public String getClassLocationMoniker(@NotNull String className) {
    return null;
  }

  @Override
  @NotNull
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
}
