package com.intellij.structure.domain;

import com.intellij.structure.impl.domain.JdkImpl;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Set of JDK classes
 *
 * @author Sergey Patrikeev
 */
public abstract class Jdk {

  @NotNull
  public static Jdk createJdk(@NotNull File jdkPath) throws IOException {
    return new JdkImpl(jdkPath);
  }

  @NotNull
  public abstract Resolver getResolver();


}
