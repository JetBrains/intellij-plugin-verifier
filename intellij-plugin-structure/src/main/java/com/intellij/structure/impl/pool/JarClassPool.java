package com.intellij.structure.impl.pool;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.pool.ClassPool;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private final Map<String, SoftReference<ClassFile>> myClassesCache = new HashMap<String, SoftReference<ClassFile>>();

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
  public ClassFile findClass(@NotNull String className) {
    if (!myClassesCache.containsKey(className)) {
      return null;
    }
    SoftReference<ClassFile> reference = myClassesCache.get(className);
    ClassFile classFile = reference == null ? null : reference.get();
    if (classFile == null) {
      classFile = evaluateNode(className);
      myClassesCache.put(className, new SoftReference<ClassFile>(classFile));
    }
    return classFile;
  }

  @Nullable
  private ClassFile evaluateNode(@NotNull String className) {
    final ZipEntry entry = myJarFile.getEntry(className + CLASS_SUFFIX);
    InputStream inputStream = null;
    try {
      inputStream = myJarFile.getInputStream(entry);
      return new ClassFile(inputStream);
    } catch (IOException e) {
      //TODO: proceed exception
      return null;
    } finally {
      IOUtils.closeQuietly(inputStream);
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
