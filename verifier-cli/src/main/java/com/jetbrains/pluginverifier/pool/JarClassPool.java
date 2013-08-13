package com.jetbrains.pluginverifier.pool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Dennis.Ushakov
 */
public class JarClassPool implements ClassPool {
  private final JarFile myJarFile;
  private final Map<String, SoftReference<ClassNode>> myClassMap = new HashMap<String, SoftReference<ClassNode>>();
  private static final String CLASS_SUFFIX = ".class";
  private final String myMoniker;

  public JarClassPool(@NotNull JarFile jarFile) throws IOException {
    myJarFile = jarFile;
    myMoniker = jarFile.getName();
    preloadClassMap();
  }

  private void preloadClassMap() throws IOException {
    final Enumeration<JarEntry> entries = myJarFile.entries();
    while (entries.hasMoreElements()) {
      final JarEntry entry = entries.nextElement();
      final String name = entry.getName();
      if (!name.endsWith(CLASS_SUFFIX)) continue;
      myClassMap.put(name.substring(0, name.indexOf(CLASS_SUFFIX)), null);
    }
  }

  public ClassNode getClassNode(String className) {
    if (!myClassMap.containsKey(className)) return null;
    final SoftReference<ClassNode> ref = myClassMap.get(className);
    ClassNode node;
    if (ref == null || (node = ref.get()) == null) {
      node = evaluateNode(className);
      myClassMap.put(className, new SoftReference<ClassNode>(node));
    }
    return node;
  }

  @Override
  public String getClassLocationMoniker(final String className) {
    if (myClassMap.containsKey(className))
      return myMoniker;

    return null;
  }

  public Collection<String> getAllClasses() {
    return myClassMap.keySet();
  }

  @NotNull
  public String getMoniker() {
    return myMoniker;
  }

  @Override
  public boolean isEmpty() {
    return myClassMap.isEmpty();
  }

  @Nullable
  private ClassNode evaluateNode(final String className) {
    try {
      final ZipEntry entry = myJarFile.getEntry(className + CLASS_SUFFIX);
      final InputStream is = myJarFile.getInputStream(entry);
      final ClassNode node = new ClassNode();
      new ClassReader(is).accept(node, 0);
      is.close();
      return node;
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String toString() {
    return getMoniker();
  }
}
