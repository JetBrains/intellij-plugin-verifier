package com.jetbrains.plugin.structure.classes.resolvers;

import com.google.common.collect.Iterators;
import com.jetbrains.plugin.structure.classes.utils.AsmUtil;
import com.jetbrains.plugin.structure.classes.utils.StringUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dennis.Ushakov
 */
public final class JarFileResolver extends Resolver {

  private static final String CLASS_SUFFIX = ".class";

  private final ZipFile myJarFile;

  private final Set<String> myClasses;

  private final File myOriginalFile;

  public JarFileResolver(@NotNull File jarFile) throws IOException {
    myOriginalFile = jarFile;
    myJarFile = new ZipFile(jarFile);
    myClasses = readClasses();
  }

  private Set<String> readClasses() {
    Enumeration<? extends ZipEntry> entries = myJarFile.entries();
    Set<String> classes = new HashSet<String>();
    ZipEntry entry;
    while (entries.hasMoreElements()) {
      entry = entries.nextElement();
      String entryName = entry.getName();
      if (entryName.endsWith(CLASS_SUFFIX)) {
        classes.add(StringUtil.trimEnd(entryName, CLASS_SUFFIX));
      }
    }
    return classes;
  }

  @NotNull
  @Override
  public Iterator<String> getAllClasses() {
    return Iterators.unmodifiableIterator(myClasses.iterator());
  }

  @Override
  public String toString() {
    return myJarFile.getName();
  }

  @Override
  public boolean isEmpty() {
    return myClasses.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myClasses.contains(className);
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return Collections.singletonList(myOriginalFile);
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    return myClasses.contains(className) ? evaluateNode(className) : null;
  }

  @Nullable
  private ClassNode evaluateNode(@NotNull String className) throws IOException {
    final ZipEntry entry = myJarFile.getEntry(className + CLASS_SUFFIX);
    if (entry != null) {
      InputStream inputStream = null;
      try {
        inputStream = myJarFile.getInputStream(entry);
        return AsmUtil.readClassNode(className, inputStream);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }
    return null;
  }

  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return containsClass(className) ? this : null;
  }

  @Override
  public void close() throws IOException {
    myJarFile.close();
  }
}
