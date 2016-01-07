package com.intellij.structure.impl.pool;

import com.intellij.structure.pool.ClassPool;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class InMemoryJarClassPool implements ClassPool {


  private final Map<String, Object> myInMemoryClasses = new HashMap<String, Object>();

  private final String myMoniker;

  public InMemoryJarClassPool(@NotNull String moniker) {
    myMoniker = moniker;
  }

  public void addClass(String name, byte[] code) {
    myInMemoryClasses.put(name, code);
  }

  public void addClass(@NotNull String className, @NotNull ClassNode classNode) {
    myInMemoryClasses.put(className, classNode);
  }

  public void addClass(@NotNull ClassNode node) {
    myInMemoryClasses.put(node.name, node);
  }

  @NotNull
  @Override
  public Collection<String> getAllClasses() {
    return Collections.unmodifiableCollection(myInMemoryClasses.keySet());
  }

  @NotNull
  @Override
  public String getMoniker() {
    return myMoniker;
  }

  public boolean isEmpty() {
    return myInMemoryClasses.isEmpty();
  }

  @Override
  public ClassNode findClass(@NotNull String className) {
    Object obj = myInMemoryClasses.get(className);

    if (obj == null) return null;

    if (obj instanceof ClassNode) {
      return (ClassNode) obj;
    }

    byte[] classContent = (byte[]) obj;

    ClassNode node = new ClassNode();
    new ClassReader(classContent).accept(node, 0);

    myInMemoryClasses.put(className, node);

    return node;
  }

  @Override
  public String getClassLocationMoniker(@NotNull String className) {
    if (myInMemoryClasses.containsKey(className)) {
      return myMoniker;
    }
    return null;
  }

  @Override
  public String toString() {
    return getMoniker();
  }

}
