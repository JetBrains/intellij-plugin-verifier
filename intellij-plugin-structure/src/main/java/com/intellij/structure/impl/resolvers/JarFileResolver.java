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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dennis.Ushakov
 */
public class JarFileResolver extends Resolver {

  private static final String CLASS_SUFFIX = ".class";

  private final ZipFile myJarFile;

  private final Set<String> myClassesNames = new HashSet<String>();

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
        myClassesNames.add(StringUtil.trimEnd(name, CLASS_SUFFIX));
      }
    }
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myClassesNames);
  }

  @Override
  public String toString() {
    return myJarFile.getName();
  }

  @Override
  public boolean isEmpty() {
    return myClassesNames.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myClassesNames.contains(className);
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    if (!myClassesNames.contains(className)) {
      return null;
    }
    return evaluateNode(className);
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
    if (myClassesNames.contains(className)) {
      return this;
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    myJarFile.close();
  }
}
