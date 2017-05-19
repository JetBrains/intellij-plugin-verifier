package com.intellij.structure.impl.resolvers;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
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
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.google.common.collect.Iterators.*;

/**
 * @author Dennis.Ushakov
 */
public final class JarFileResolver extends Resolver {

  private static final String CLASS_SUFFIX = ".class";

  private final ZipFile myJarFile;

  private final File myOriginalFile;

  public JarFileResolver(@NotNull File jarFile) throws IOException {
    myOriginalFile = jarFile;
    myJarFile = new ZipFile(jarFile);
  }

  private Iterator<String> getClasses() {
    Enumeration<? extends ZipEntry> entries = myJarFile.entries();
    Function<ZipEntry, String> mapper = new Function<ZipEntry, String>() {
      @Override
      public String apply(ZipEntry input) {
        String entryName = input.getName();
        return entryName.endsWith(CLASS_SUFFIX) ? StringUtil.trimEnd(entryName, CLASS_SUFFIX) : null;
      }
    };
    return filter(transform(forEnumeration(entries), mapper), Predicates.notNull());
  }

  @NotNull
  @Override
  public Iterator<String> getAllClasses() {
    return Iterators.unmodifiableIterator(getClasses());
  }

  @Override
  public String toString() {
    return myJarFile.getName();
  }

  @Override
  public boolean isEmpty() throws IOException {
    return !Iterators.any(getAllClasses(), Predicates.<String>alwaysTrue());
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myJarFile.getEntry(className + CLASS_SUFFIX) != null;
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return Collections.singletonList(myOriginalFile);
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    return evaluateNode(className);
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
