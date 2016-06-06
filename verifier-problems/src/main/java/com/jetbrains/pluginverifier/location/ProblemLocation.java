package com.jetbrains.pluginverifier.location;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.persistence.Jsonable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Patrikeev
 */
public abstract class ProblemLocation implements Jsonable<ProblemLocation> {

  //TODO: add more detailed location, e.g. superclass, field of a class, interface, throws list and so on

  @NotNull
  public static ProblemLocation fromPlugin(@NotNull String pluginId) {
    return new PluginLocation(pluginId);
  }

  @NotNull
  public static ProblemLocation fromClass(@NotNull String className) {
    return new CodeLocation(className, null, null);
  }

  @NotNull
  public static ProblemLocation fromField(@NotNull String className, @NotNull String fieldName) {
    return new CodeLocation(className, null, fieldName);
  }

  @NotNull
  @TestOnly
  public static ProblemLocation fromMethod(@NotNull String className, @NotNull String methodDescr) {
    return new CodeLocation(className, methodDescr, null);
  }

  @NotNull
  public static ProblemLocation fromMethod(@NotNull String className, @NotNull MethodNode methodNode) {
    return new CodeLocation(className, getMethodDescr(methodNode), null);
  }

  @NotNull
  private static String getMethodDescr(@NotNull MethodNode methodNode) {
    Preconditions.checkArgument(methodNode.name != null);
    Preconditions.checkArgument(methodNode.desc != null);
    return methodNode.name + methodNode.desc;
  }

  public abstract String asString();

  @Override
  public final String toString() {
    return asString();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || o.getClass() != getClass()) return false;
    return asString().equals(((ProblemLocation) o).asString());
  }

  @Override
  public int hashCode() {
    return asString().hashCode();
  }
}
