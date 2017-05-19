package com.intellij.structure.impl.resolvers;

import com.google.common.collect.ImmutableSet;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class EmptyResolver extends Resolver {

  public static final Resolver INSTANCE = new EmptyResolver();

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) {
    return null;
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return null;
  }

  @Override
  @NotNull
  public Iterator<String> getAllClasses() {
    return ImmutableSet.<String>of().iterator();
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return false;
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return Collections.emptyList();
  }

  @Override
  public String toString() {
    return "EmptyResolver";
  }

  @Override
  public void close() {
    //do nothing
  }
}
