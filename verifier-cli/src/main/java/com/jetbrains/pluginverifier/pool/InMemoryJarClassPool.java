package com.jetbrains.pluginverifier.pool;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Sergey Evdokimov
 */
public class InMemoryJarClassPool implements ClassPool {

  private final Map<String, Object> myClassMap = new HashMap<String, Object>();

  private final String myMoniker;

  public InMemoryJarClassPool(String moniker) throws IOException {
    myMoniker = moniker;
  }

  public void addClass(String name, byte[] code) {
    myClassMap.put(name, code);
  }

  public void addClass(@NotNull ClassNode node) {
    myClassMap.put(node.name, node);
  }

  @Override
  public ClassNode getClassNode(String className) {
    Object obj = myClassMap.get(className);

    if (obj == null) return null;

    if (obj instanceof ClassNode) {
      return (ClassNode)obj;
    }

    byte[] classContent = (byte[])obj;

    ClassNode node = new ClassNode();
    new ClassReader(classContent).accept(node, 0);

    myClassMap.put(className, node);

    return node;
  }

  @Override
  public String getClassLocationMoniker(String className) {
    if (myClassMap.containsKey(className))
      return myMoniker;

    return null;
  }

  @Override
  public Collection<String> getAllClasses() {
    return myClassMap.keySet();
  }

  @NotNull
  @Override
  public String getMoniker() {
    return myMoniker;
  }

  @Override
  public boolean isEmpty() {
    return myClassMap.isEmpty();
  }

  @Override
  public String toString() {
    return getMoniker();
  }

}
