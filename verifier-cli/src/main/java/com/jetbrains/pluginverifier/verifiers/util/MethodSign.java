package com.jetbrains.pluginverifier.verifiers.util;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Evdokimov
 */
public class MethodSign {

  private final String name;
  private final String descr;

  private int hashCode;

  public MethodSign(@NotNull String name, @NotNull String descr) {
    this.name = name;
    this.descr = descr;
  }

  public MethodSign(@NotNull MethodNode methodNode) {
    this(methodNode.name, methodNode.desc);
  }

  public String getName() {
    return name;
  }

  public String getDescr() {
    return descr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodSign)) return false;

    MethodSign sign = (MethodSign)o;

    if (!descr.equals(sign.descr)) return false;
    if (!name.equals(sign.name)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = 31 * name.hashCode() + descr.hashCode();
    }
    return hashCode;
  }
}
