package com.jetbrains.pluginverifier.pool;

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
  private final String myName;

  public JarClassPool(final String name, final JarFile jarFile) throws IOException {
    myJarFile = jarFile;
    myName = name;
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
    final ClassNode node;
    if (ref == null || ref.get() == null) {
      node = evaluateNode(className);
    } else {
      node = ref.get();
    }
    return node;
  }

  public Collection<String> getAllClasses() {
    return myClassMap.keySet();
  }

  public String getName() {
    return myName;
  }

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
}
