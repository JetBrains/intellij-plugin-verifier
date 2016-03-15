package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Evdokimov
 */
public class InMemoryJarResolver extends Resolver {

  private final Map<String, ClassNode> myInMemoryClasses = new HashMap<String, ClassNode>();

  private final String myPresentableName;

  public InMemoryJarResolver(@NotNull String presentableName) {
    myPresentableName = presentableName;
  }

  public void addClass(@NotNull ClassNode classNode) {
    myInMemoryClasses.put(classNode.name, classNode);
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myInMemoryClasses.keySet());
  }


  public boolean isEmpty() {
    return myInMemoryClasses.isEmpty();
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) {
    return myInMemoryClasses.get(className);
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
