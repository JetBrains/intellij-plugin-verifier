package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
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
public class InMemoryJarResolver extends Resolver {

  private final Map<String, Object> myInMemoryClasses = new HashMap<String, Object>();

  private final String myPresentableName;

  public InMemoryJarResolver(@NotNull String presentableName) {
    myPresentableName = presentableName;
  }

  public void addClass(@NotNull String name, @NotNull byte[] code) {
    myInMemoryClasses.put(name, code);
  }

  public void addClass(@NotNull ClassNode classNode) {
    myInMemoryClasses.put(classNode.name, classNode);
  }

  @NotNull
  @Override
  public Collection<String> getAllClasses() {
    return Collections.unmodifiableCollection(myInMemoryClasses.keySet());
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
  public Resolver getClassLocation(@NotNull String className) {
    if (myInMemoryClasses.containsKey(className)) {
      return this;
    }
    return null;
  }

  @Override
  public String toString() {
    return myPresentableName;
  }

}
