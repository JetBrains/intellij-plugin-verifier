package com.intellij.structure.pool;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Dennis.Ushakov
 */
public interface ClassPool extends Resolver {
  ClassPool EMPTY_POOL = new ClassPool() {
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
  };

  @Nullable
  ClassNode findClass(@NotNull String className);

  @Nullable
  String getClassLocationMoniker(@NotNull String className);

  /**
   * @return list of names of all containing classes. Names are present in binary form.
   */
  @NotNull
  Collection<String> getAllClasses();

  /**
   * @return moniker of this class-pool. It may be for example name of containing .jar-file
   */
  @Nullable
  String getMoniker();

  boolean isEmpty();
}
