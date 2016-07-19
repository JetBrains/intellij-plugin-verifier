package com.jetbrains.pluginverifier.utils;

import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VContext;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Dennis.Ushakov
 */
public class ResolverUtil {

  @Nullable
  private static MethodLocation findMethod(@NotNull Resolver resolver, @NotNull String className, @NotNull String methodName, @NotNull String methodDesc, VContext ctx, String childName) {
    if (className.startsWith("[")) {
      throw new RuntimeException("Method owner class must not be an array class");
    }

    final ClassNode classFile = VerifierUtil.findClass(resolver, className, ctx);
    if (classFile == null) {
      if (!ctx.getVerifierOptions().isExternalClass(className)) {
        ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromClass(childName)); //TODO: add a 'super' location
      }
      return null;
    }

    return findMethod(resolver, classFile, methodName, methodDesc, ctx);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static MethodLocation findMethod(@NotNull Resolver resolver, @NotNull ClassNode clazz, @NotNull String methodName, @NotNull String methodDesc, VContext ctx) {
    for (MethodNode method : (List<MethodNode>) clazz.methods) {
      if (methodName.equals(method.name) && methodDesc.equals(method.desc)) {
        return new MethodLocation(clazz, method);
      }
    }

    if (clazz.superName != null) {
      MethodLocation res = findMethod(resolver, clazz.superName, methodName, methodDesc, ctx, clazz.name);
      if (res != null) {
        return res;
      }
    }

    for (Object anInterface : clazz.interfaces) {
      final MethodLocation res = findMethod(resolver, (String) anInterface, methodName, methodDesc, ctx, clazz.name);
      if (res != null) {
        return res;
      }
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static FieldLocation findField(@NotNull Resolver resolver, @NotNull String className, @NotNull String fieldName, @NotNull String fieldDescriptor, VContext ctx, String childName) {
    if (className.startsWith("[")) {
      throw new RuntimeException("Method owner class must not be an array class");
    }

    ClassNode classFile = VerifierUtil.findClass(resolver, className, ctx);
    if (classFile == null) {
      if (!ctx.getVerifierOptions().isExternalClass(className)) {
        ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromClass(childName));
      }
      return null;
    }

    return findField(resolver, classFile, fieldName, fieldDescriptor, ctx);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static FieldLocation findField(@NotNull Resolver resolver, @NotNull ClassNode clazz, @NotNull String fieldName, @NotNull String fieldDescriptor, VContext ctx) {
    for (FieldNode field : (List<FieldNode>) clazz.fields) {
      if (StringUtil.equal(field.name, fieldName) && StringUtil.equal(field.desc, fieldDescriptor)) {
        return new FieldLocation(clazz, field);
      }
    }

    //superinterfaces first
    for (String anInterface : (List<String>) clazz.interfaces) {
      FieldLocation res = findField(resolver, anInterface, fieldName, fieldDescriptor, ctx, clazz.name);
      if (res != null) {
        return res;
      }
    }

    //superclass second
    if (clazz.superName != null) {
      FieldLocation res = findField(resolver, clazz.superName, fieldName, fieldDescriptor, ctx, clazz.name);
      if (res != null) {
        return res;
      }
    }

    return null;
  }

  @NotNull
  static Set<String> collectUnresolvedClasses(@NotNull Resolver resolver, @NotNull String className, VContext ctx) {
    ClassNode node = VerifierUtil.findClass(resolver, className, ctx);
    if (node == null) {
      return Collections.singleton(className);
    }
    return new ParentsVisitor(resolver, ctx).collectUnresolvedParents(className, s -> false);
  }

  public static class FieldLocation {
    private final ClassNode myClassNode;
    private final FieldNode myFieldNode;


    FieldLocation(ClassNode classNode, FieldNode fieldNode) {
      myClassNode = classNode;
      myFieldNode = fieldNode;
    }

    public ClassNode getClassNode() {
      return myClassNode;
    }

    public FieldNode getFieldNode() {
      return myFieldNode;
    }
  }

  public static class MethodLocation {

    private final ClassNode myClassNode;
    private final MethodNode myMethodNode;

    MethodLocation(@NotNull ClassNode classNode, @NotNull MethodNode methodNode) {
      this.myClassNode = classNode;
      this.myMethodNode = methodNode;
    }

    @NotNull
    public ClassNode getClassNode() {
      return myClassNode;
    }

    @NotNull
    public MethodNode getMethodNode() {
      return myMethodNode;
    }

  }
}
