package com.intellij.structure.impl.pool;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.utils.ClassFileUtil;
import org.jetbrains.annotations.NotNull;
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

  public void addClass(@NotNull ClassFile classFile) {
    String className = ClassFileUtil.extractClassName(classFile);
    myInMemoryClasses.put(className, classFile);
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
    ClassFile classFile = new ClassFile(classContent);

    myInMemoryClasses.put(className, classFile);

    return classFile;
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
