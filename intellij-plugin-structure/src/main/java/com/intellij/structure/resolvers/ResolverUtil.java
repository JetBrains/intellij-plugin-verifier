package com.intellij.structure.resolvers;

import com.google.common.base.Predicates;
import com.intellij.structure.impl.resolvers.ParentsVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.Set;

/**
 * @author Dennis.Ushakov
 */
public class ResolverUtil {

  @Nullable
  public static MethodLocation findMethod(final Resolver resolver, final String className, final String methodName, final String methodDesc) {
    if (className.startsWith("[")) {
      // so a receiver is an array, just assume it does exist =)
      return null;
    }

    final ClassNode clazz = resolver.findClass(className);
    if (clazz == null) {
      return null;
    }

    return findMethod(resolver, clazz, methodName, methodDesc);
  }

  @Nullable
  public static MethodLocation findMethod(@NotNull Resolver resolver, @NotNull ClassNode clazz, @NotNull String methodName, @NotNull String methodDesc) {
    for (Object o : clazz.methods) {
      final MethodNode method = (MethodNode) o;
      if (methodName.equals(method.name) && methodDesc.equals(method.desc)) {
        return new MethodLocation(clazz, method);
      }
    }

    if (clazz.superName != null) {
      MethodLocation res = findMethod(resolver, clazz.superName, methodName, methodDesc);
      if (res != null) return res;
    }

    for (Object anInterface : clazz.interfaces) {
      final MethodLocation res = findMethod(resolver, (String) anInterface, methodName, methodDesc);
      if (res != null) return res;
    }

    return null;
  }

  @NotNull
  public static Set<String> collectUnresolvedClasses(@NotNull Resolver resolver, @NotNull String className) {
    ClassNode node = resolver.findClass(className);
    if (node == null) {
      return Collections.singleton(className);
    }
    return new ParentsVisitor(resolver).collectUnresolvedParents(className, Predicates.<String>alwaysFalse());
  }


  public static class MethodLocation {

    private final ClassNode classNode;
    private final MethodNode methodNode;

    public MethodLocation(@NotNull ClassNode classNode, @NotNull MethodNode methodNode) {
      this.classNode = classNode;
      this.methodNode = methodNode;
    }

    @NotNull
    public ClassNode getClassNode() {
      return classNode;
    }

    @NotNull
    public MethodNode getMethodNode() {
      return methodNode;
    }

  }
}
