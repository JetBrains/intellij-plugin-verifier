package com.intellij.structure.impl.resolvers;

import com.intellij.structure.impl.utils.AsmUtil;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dennis.Ushakov
 */
public class JarFileResolver extends Resolver {

  private static final String CLASS_SUFFIX = ".class";

  private final ZipFile myJarFile;

  private final Map<String, SoftReference<ClassNode>> myClassesCache = new HashMap<String, SoftReference<ClassNode>>();

  public JarFileResolver(@NotNull File jarFile) throws IOException {
    myJarFile = new ZipFile(jarFile);
    preloadClassMap();
  }

  private void preloadClassMap() throws IOException {
    Enumeration<? extends ZipEntry> entries = myJarFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.endsWith(CLASS_SUFFIX)) {
        myClassesCache.put(StringUtil.trimEnd(name, CLASS_SUFFIX), null);
      }
    }
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myClassesCache.keySet());
  }

  @Override
  public String toString() {
    return myJarFile.getName();
  }

  @Override
  public boolean isEmpty() {
    return myClassesCache.isEmpty();
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
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
  private ClassNode evaluateNode(@NotNull String className) throws IOException {
    final ZipEntry entry = myJarFile.getEntry(className + CLASS_SUFFIX);
    InputStream inputStream = null;
    try {
      inputStream = myJarFile.getInputStream(entry);
      return AsmUtil.readClassNode(className, inputStream);
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

  @Override
  public void close() {
    try {
      myJarFile.close();
    } catch (IOException ignored) {
    }
  }
}
