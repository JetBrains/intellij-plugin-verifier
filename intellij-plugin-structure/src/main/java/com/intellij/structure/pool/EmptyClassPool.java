package com.intellij.structure.pool;

import com.intellij.structure.bytecode.ClassFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey Patrikeev
 */
public class EmptyClassPool implements ClassPool {

  public static final ClassPool INSTANCE = new EmptyClassPool();

  @Nullable
  @Override
  public ClassFile findClass(@NotNull String className) {
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
