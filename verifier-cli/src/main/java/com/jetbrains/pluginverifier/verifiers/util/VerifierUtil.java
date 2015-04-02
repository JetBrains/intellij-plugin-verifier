package com.jetbrains.pluginverifier.verifiers.util;

import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.resolvers.Resolver;
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

  private static boolean isValidClassOrInterface(PluginVerifierOptions opt, final Resolver resolver, final @NotNull String name, final Boolean isInterface) {
    assert !name.startsWith("[");
    assert !name.endsWith(";");

    if (opt.isExternalClass(name)) {
      return true;
    }

    final ClassNode clazz = resolver.findClass(name);
    return clazz != null && (isInterface == null || isInterface == ((clazz.access & Opcodes.ACC_INTERFACE) != 0));
  }

  public static String prepareArrayName(final String className) {
    if (className.startsWith("[")) {
      int i = 1;
      while (i < className.length() && className.charAt(i) == '[') {
        i++;
      }

      return className.substring(i);
    }

    return className;
  }

  @Nullable // return null for primitive types
  public static String extractClassNameFromDescr(String descr) {
    descr = prepareArrayName(descr);

    if (isPrimitiveType(descr)) return null;

    if (descr.startsWith("L") && descr.endsWith(";")) {
      return descr.substring(1, descr.length() - 1);
    }

    return descr;
  }

  public static boolean isPrimitiveType(final String type) {
    if (type.length() != 1)
      return false;

    return "Z".equals(type) || "I".equals(type) || "J".equals(type) || "B".equals(type) ||
           "F".equals(type) || "S".equals(type) || "D".equals(type) || "C".equals(type);
  }

  public static boolean isFinal(final MethodNode superMethod) {
    return (superMethod.access & Opcodes.ACC_FINAL) != 0;
  }

  public static boolean isAbstract(final MethodNode superMethod) {
    return (superMethod.access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public static boolean isAbstract(@NotNull ClassNode clazz) {
    return (clazz.access & Opcodes.ACC_ABSTRACT) != 0;
  }
}
