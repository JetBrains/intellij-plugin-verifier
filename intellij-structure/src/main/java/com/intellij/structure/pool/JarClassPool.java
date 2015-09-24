package com.intellij.structure.pool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Dennis.Ushakov
 */
public class JarClassPool implements ClassPool {

  private static final String CLASS_SUFFIX = ".class";

  private final JarFile myJarFile;
  private final String myMoniker;

  private final Map<String, SoftReference<ClassNode>> myClassesCache = new HashMap<String, SoftReference<ClassNode>>();

  public JarClassPool(@NotNull JarFile jarFile) throws IOException {
    myMoniker = jarFile.getName();
    myJarFile = jarFile;
    preloadClassMap();
  }

  private void preloadClassMap() throws IOException {
    Enumeration<JarEntry> entries = myJarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.endsWith(CLASS_SUFFIX)) {
        myClassesCache.put(name.substring(0, name.indexOf(CLASS_SUFFIX)), null);
      }
    }
  }

  @NotNull
  @Override
  public Collection<String> getAllClasses() {
    return Collections.unmodifiableSet(myClassesCache.keySet());
  }

  @NotNull
  @Override
  public String getMoniker() {
    return myMoniker;
  }

  @Override
  public boolean isEmpty() {
    return myClassesCache.isEmpty();
  }

  @Override
  public ClassNode findClass(@NotNull String className) {
    if (!myClassesCache.containsKey(className)) {
      return null;
    }
    SoftReference<ClassNode> reference = myClassesCache.get(className);
    ClassNode classNode = reference == null ? null : reference.get();
    if (classNode == null) {
      classNode = evaluateNode(className);
      myClassesCache.put(className, new SoftReference<ClassNode>(classNode));
    }
    return classNode;
  }

  @Nullable
  private ClassNode evaluateNode(@NotNull String className) {
    try {
      final ZipEntry entry = myJarFile.getEntry(className + CLASS_SUFFIX);
      final InputStream inputStream = myJarFile.getInputStream(entry);
      final ClassNode classNode = new ClassNode();
      new ClassReader(inputStream).accept(classNode, 0);
      inputStream.close();
      return classNode;
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public String getClassLocationMoniker(@NotNull String className) {
    if (myClassesCache.containsKey(className)) {
      return myMoniker;
    }
    return null;
  }
}
