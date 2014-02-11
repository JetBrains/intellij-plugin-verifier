package com.jetbrains.pluginverifier.pool;

import com.jetbrains.pluginverifier.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class ResolverUtil {
  private final static MethodNode ARRAY_METHOD_NODE = new MethodNode();
  private final static ClassNode ARRAY_CLASS_NODE = new ClassNode();

  private final static MethodLocation ARRAY_METHOD_LOCATION = new MethodLocation(ARRAY_CLASS_NODE, ARRAY_METHOD_NODE);

  private final static MethodNode UNKNOWN_CLASS_METHOD_NODE = new MethodNode();
  private final static ClassNode UNKNOWN_CLASS_NODE = new ClassNode();

  private final static MethodLocation UNKNOWN_METHOD_LOCATION = new MethodLocation(UNKNOWN_CLASS_NODE, UNKNOWN_CLASS_METHOD_NODE);

  @Nullable
  public static MethodLocation findMethod(final Resolver resolver, final String className, final String methodName, final String methodDesc) {
    if (className.startsWith("[")) {
      // so a receiver is an array, just assume it does exist =)
      return ARRAY_METHOD_LOCATION;
    }

    final ClassNode clazz = resolver.findClass(className);
    if (clazz == null) {
      return UNKNOWN_METHOD_LOCATION;
    }

    return findMethod(resolver, clazz, methodName, methodDesc);
  }

  @Nullable
  public static MethodLocation findMethod(@NotNull Resolver resolver, @NotNull ClassNode clazz, @NotNull String methodName, @NotNull String methodDesc) {
    for (Object o : clazz.methods) {
      final MethodNode method = (MethodNode)o;
      if (methodName.equals(method.name) && methodDesc.equals(method.desc)) {
        return new MethodLocation(clazz, method);
      }
    }

    if (clazz.superName != null) {
      MethodLocation res = findMethod(resolver, clazz.superName, methodName, methodDesc);
      if (res != null) return res;
    }

    for (Object anInterface : clazz.interfaces) {
      final MethodLocation res = findMethod(resolver, (String)anInterface, methodName, methodDesc);
      if (res != null) return res;
    }

    return null;
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

    public String getMethodDescr() {
      return classNode.name + '#' + methodNode.name + methodNode.desc;
    }
  }
}
