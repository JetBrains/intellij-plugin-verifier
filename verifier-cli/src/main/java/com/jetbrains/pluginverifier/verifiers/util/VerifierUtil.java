package com.jetbrains.pluginverifier.verifiers.util;

import com.google.common.base.Preconditions;
import com.intellij.structure.bytecode.AsmBytecode;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.utils.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class VerifierUtil {
  public static boolean classExists(PluginVerifierOptions opt, final Resolver resolver, final @NotNull String className) {
    return classExists(opt, resolver, className, null);
  }

  public static boolean classExists(PluginVerifierOptions opt, final Resolver resolver, final @NotNull String className, final Boolean isInterface) {
    return isValidClassOrInterface(opt, resolver, className, isInterface);
  }

  public static boolean isInterface(@NotNull ClassNode classNode) {
    return (classNode.access & Opcodes.ACC_INTERFACE) != 0;
  }

  private static boolean isValidClassOrInterface(PluginVerifierOptions opt, final Resolver resolver, final @NotNull String name, final @Nullable Boolean isInterface) {
    Assert.assertTrue(!name.startsWith("["), name);
    Assert.assertTrue(!name.endsWith(";"), name);

    if (opt.isExternalClass(name)) {
      return true;
    }

    final ClassNode clazz = AsmBytecode.convertToAsmNode(resolver.findClass(name));
    return clazz != null && (isInterface == null || isInterface == isInterface(clazz));
  }

  public static String prepareArrayName(@NotNull final String className) {
    if (className.startsWith("[")) {
      int i = 1;
      while (i < className.length() && className.charAt(i) == '[') {
        i++;
      }

      return className.substring(i);
    }

    return className;
  }

  @NotNull
  private static String withoutReturnType(@NotNull String descriptor) {
    int bracket = descriptor.lastIndexOf(')');
    Preconditions.checkArgument(bracket != -1);
    return descriptor.substring(0, bracket + 1);
  }


  @Nullable // return null for primitive types
  public static String extractClassNameFromDescr(@NotNull String descr) {
    descr = prepareArrayName(descr);

    if (isPrimitiveType(descr)) return null;

    if (descr.startsWith("L") && descr.endsWith(";")) {
      return descr.substring(1, descr.length() - 1);
    }

    return descr;
  }

  public static boolean isPrimitiveType(@NotNull final String type) {
    return "Z".equals(type) || "I".equals(type) || "J".equals(type) || "B".equals(type) ||
        "F".equals(type) || "S".equals(type) || "D".equals(type) || "C".equals(type);
  }

  public static boolean isFinal(final MethodNode superMethod) {
    return (superMethod.access & Opcodes.ACC_FINAL) != 0;
  }

  public static boolean isAbstract(@NotNull ClassNode clazz) {
    return (clazz.access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public static boolean isPrivate(@NotNull MethodNode method) {
    return (method.access & Opcodes.ACC_PRIVATE) != 0;
  }

  public static boolean isPublic(@NotNull MethodNode method) {
    return (method.access & Opcodes.ACC_PUBLIC) != 0;
  }

  public static boolean isDefaultAccess(@NotNull MethodNode method) {
    return !isPublic(method) && !isProtected(method) && !isPrivate(method);
  }

  public static boolean isAbstract(@NotNull MethodNode method) {
    return (method.access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public static boolean isProtected(@NotNull MethodNode method) {
    return (method.access & Opcodes.ACC_PROTECTED) != 0;
  }

  public static boolean isStatic(@NotNull MethodNode method) {
    return (method.access & Opcodes.ACC_STATIC) != 0;
  }
}
