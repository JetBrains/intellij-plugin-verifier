package com.jetbrains.pluginverifier.verifiers.util;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Evdokimov
 */
public class MethodSign {

  private final String myName;
  private final String myDescriptor;

  private int hashCode;

  public MethodSign(@NotNull String name, @NotNull String myDescriptor) {
    this.myName = name;
    this.myDescriptor = myDescriptor;
  }

  public MethodSign(@NotNull MethodNode methodNode) {
    this(methodNode.name, methodNode.desc);
  }

  public String getName() {
    return myName;
  }

  public String getDescriptor() {
    return myDescriptor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodSign)) return false;

    MethodSign sign = (MethodSign) o;

    return myDescriptor.equals(sign.myDescriptor) && myName.equals(sign.myName);

  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = 31 * myName.hashCode() + myDescriptor.hashCode();
    }
    return hashCode;
  }
}
