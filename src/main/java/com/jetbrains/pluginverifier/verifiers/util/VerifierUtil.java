package com.jetbrains.pluginverifier.verifiers.util;

import com.jetbrains.pluginverifier.pool.Resolver;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis.Ushakov
 */
public class VerifierUtil {
  // TODO: do normal caching with multi-threaded support
  private static Map<String, Boolean> classExistsCache = new HashMap<String, Boolean>();
  private static Map<String, Boolean> methodExistsCache = new HashMap<String, Boolean>();

  public static boolean classExists(final Resolver resolver, final String className) {
    return classExists(resolver, className, null);
  }

  public static boolean classExists(final Resolver resolver, final String className, final Boolean isInterface) {
    final Boolean cached = classExistsCache.get(resolver.getName() + ";" + className + ";" + isInterface);
    if (cached != null) return cached;
    final boolean result;
    if (className.startsWith("[") || className.endsWith(";")) {
      final String name = prepareArrayName(className);
      // "" means it's a primitive type, validation can be skipped
      result = "".equals(name) || isValidClassOrInterface(resolver, name, isInterface);
    } else {
      result = isValidClassOrInterface(resolver, className, isInterface);
    }
    classExistsCache.put(resolver.getName() + ";" + className, result);
    return result;
  }

  private static boolean isValidClassOrInterface(final Resolver resolver, final String name, final Boolean isInterface) {
    final ClassNode clazz = resolver.findClass(name);
    return clazz != null && (isInterface == null || isInterface == ((clazz.access & Opcodes.ACC_INTERFACE) != 0));
  }

  private static String prepareArrayName(final String className) {
    final String prefix = className.replaceAll("\\[", "");
    return prefix.substring(1, prefix.length() - (prefix.endsWith(";") ? 1 : 0));
  }

  public static boolean isNativeType(final String type) {
    return "Z".equals(type) || "I".equals(type) || "J".equals(type) || "B".equals(type) ||
           "F".equals(type) || "S".equals(type) || "D".equals(type) || "C".equals(type); 
  }

  public static boolean methodExists(final Resolver resolver, final String className, final String methodName, final String methodDesc) {
    final Boolean cached = methodExistsCache.get(resolver.getName() + ";" + className + ";" + methodName + ";" + methodDesc);
    if (cached != null) return cached;
    final boolean result =  resolver.findMethod(className, methodName, methodDesc) != null;
    methodExistsCache.put(resolver.getName() + ";" + className + ";" + methodName, result);
    return result;
  }

  public static boolean isFinal(final MethodNode superMethod) {
    return (superMethod.access & Opcodes.ACC_FINAL) != 0;
  }

  public static boolean isAbstract(final MethodNode superMethod) {
    return (superMethod.access & Opcodes.ACC_ABSTRACT) == 0;
  }
}
