package com.intellij.structure.impl.resolvers;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class InMemoryJarResolver extends Resolver {

  private final Map<String, Object> myInMemoryClasses = new HashMap<String, Object>();

  private final String myMoniker;

  public InMemoryJarResolver(@NotNull String moniker) {
    myMoniker = moniker;
  }

  public void addClass(@NotNull String name, @NotNull byte[] code) {
    myInMemoryClasses.put(name, code);
  }

  public void addClass(@NotNull ClassFile classFile) {
    myInMemoryClasses.put(classFile.getClassName(), classFile);
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
  public ClassFile findClass(@NotNull String className) {
    Object obj = myInMemoryClasses.get(className);

    if (obj == null) return null;

    if (obj instanceof ClassFile) {
      return (ClassFile) obj;
    }

    byte[] classContent = (byte[]) obj;
    ClassFile classFile = new ClassFile(className, classContent);

    myInMemoryClasses.put(className, classFile);

    return classFile;
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
    return getMoniker();
  }

}
