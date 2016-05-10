package com.jetbrains.pluginverifier.verifiers.util;

import com.google.common.base.Predicates;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.verifiers.VerificationContext;
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
  public static MethodLocation findMethod(@NotNull Resolver resolver, @NotNull String className, @NotNull String methodName, @NotNull String methodDesc, VerificationContext ctx) {
    if (className.startsWith("[")) {
      // so a receiver is an array, just assume it does exist =)
      return null;
    }

    final ClassNode classFile = VerifierUtil.findClass(resolver, className, ctx);
    if (classFile == null) {
      return null;
    }

    return findMethod(resolver, classFile, methodName, methodDesc, ctx);
  }

  @Nullable
  public static MethodLocation findMethod(@NotNull Resolver resolver, @NotNull ClassNode clazz, @NotNull String methodName, @NotNull String methodDesc, VerificationContext ctx) {
    for (Object o : clazz.methods) {
      final MethodNode method = (MethodNode) o;
      if (methodName.equals(method.name) && methodDesc.equals(method.desc)) {
        return new MethodLocation(clazz, method);
      }
    }

    if (clazz.superName != null) {
      MethodLocation res = findMethod(resolver, clazz.superName, methodName, methodDesc, ctx);
      if (res != null) {
        return res;
      }
    }

    for (Object anInterface : clazz.interfaces) {
      final MethodLocation res = findMethod(resolver, (String) anInterface, methodName, methodDesc, ctx);
      if (res != null) {
        return res;
      }
    }

    return null;
  }

  @NotNull
  public static Set<String> collectUnresolvedClasses(@NotNull Resolver resolver, @NotNull String className, VerificationContext ctx) {
    ClassNode node = VerifierUtil.findClass(resolver, className, ctx);
    if (node == null) {
      return Collections.singleton(className);
    }
    return new ParentsVisitor(resolver, ctx).collectUnresolvedParents(className, Predicates.alwaysFalse());
  }


  public static class MethodLocation {

    private final ClassNode classNode;
    private final MethodNode methodNode;

    MethodLocation(@NotNull ClassNode classNode, @NotNull MethodNode methodNode) {
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
