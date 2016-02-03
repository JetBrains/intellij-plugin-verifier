package com.intellij.structure.impl.resolvers;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey Patrikeev
 */
public class EmptyResolver extends Resolver {

  public static final Resolver INSTANCE = new EmptyResolver();

  @Nullable
  @Override
  public ClassFile findClass(@NotNull String className) {
    return null;
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
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
    return "EmptyResolver";
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
