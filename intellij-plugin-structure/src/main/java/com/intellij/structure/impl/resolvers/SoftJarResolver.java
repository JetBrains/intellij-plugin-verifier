package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dennis.Ushakov
 */
public class SoftJarResolver extends Resolver {

  private static final String CLASS_SUFFIX = ".class";

  private final ZipFile myJarFile;
  private final String myMoniker;

  private final Map<String, SoftReference<ClassNode>> myClassesCache = new HashMap<String, SoftReference<ClassNode>>();

  public SoftJarResolver(@NotNull ZipFile jarFile) throws IOException {
    myMoniker = jarFile.getName();
    myJarFile = jarFile;
    preloadClassMap();
  }

  private void preloadClassMap() throws IOException {
    Enumeration<? extends ZipEntry> entries = myJarFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
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

  @Override
  public String toString() {
    return myMoniker;
  }

  @Override
  public boolean isEmpty() {
    return myClassesCache.isEmpty();
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) {
    if (!myClassesCache.containsKey(className)) {
      return null;
    }
    SoftReference<ClassNode> reference = myClassesCache.get(className);
    ClassNode classFile = reference == null ? null : reference.get();
    if (classFile == null) {
      classFile = evaluateNode(className);
      myClassesCache.put(className, new SoftReference<ClassNode>(classFile));
    }
    return classFile;
  }

  @Nullable
  private ClassNode evaluateNode(@NotNull String className) {
    final ZipEntry entry = myJarFile.getEntry(className + CLASS_SUFFIX);
    InputStream inputStream = null;
    try {
      inputStream = myJarFile.getInputStream(entry);

      ClassNode node = new ClassNode();
      new ClassReader(inputStream).accept(node, 0);

      return node;
    } catch (IOException e) {
      return null;
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  @Override
  public Resolver getClassLocation(@NotNull String className) {
    if (myClassesCache.containsKey(className)) {
      return this;
    }
    return null;
  }
}
