package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

/**
 * Set of class files necessary for running IDE (JDK classes for example)
 *
 * @author Sergey Patrikeev
 */
public interface Jdk {

  @NotNull
  Resolver getResolver();

}
