package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Patrikeev
 */
public class LocationUtils {

  @NotNull
  public static String getMethodLocation(@NotNull ClassNode classNode, @NotNull MethodNode methodNode) {
    return getMethodLocation(classNode.name, methodNode);
  }

  @NotNull
  public static String getMethodLocation(@NotNull String ownerClassName, @NotNull String methodName, @NotNull String methodDesc) {
    //NotNull checks are performed automatically
    return ownerClassName + '#' + methodName + methodDesc;
  }

  @NotNull
  public static String getMethodLocation(@NotNull String ownerClassName, @NotNull MethodNode methodNode) {
    return getMethodLocation(ownerClassName, methodNode.name, methodNode.desc);
  }

  @NotNull
  public static String getFieldLocation(@NotNull String ownerClassName, @NotNull String fieldName, @NotNull String fieldDescriptor) {
    //NotNull checks are performed automatically
    return ownerClassName + '#' + fieldName + "#" + fieldDescriptor;
  }

  @NotNull
  public static String getFieldLocation(@NotNull String ownerClassName, @NotNull FieldNode fieldNode) {
    return getFieldLocation(ownerClassName, fieldNode.name, fieldNode.desc);
  }
}
