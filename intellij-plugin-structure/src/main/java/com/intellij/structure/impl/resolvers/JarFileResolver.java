package com.intellij.structure.impl.resolvers;

import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Dennis.Ushakov
 */
public class JarFileResolver extends Resolver {

  private static final String CLASS_SUFFIX = ".class";

  private final File myFile;

  private final Map<String, SoftReference<ClassNode>> myClassesCache = new HashMap<String, SoftReference<ClassNode>>();

  public JarFileResolver(@NotNull File jarFile) throws IOException {
    if (!jarFile.getName().endsWith(".jar")) {
      throw new IllegalArgumentException("File should be a .jar archive");
    }
    myFile = jarFile;
    updateCacheAndFindClass(false, null);
  }

  @Nullable
  private ClassNode updateCacheAndFindClass(boolean loadClasses, @Nullable String findClass) throws IOException {
    ClassNode result = null;

    JarFile jarFile = null;
    try {
      jarFile = new JarFile(myFile);

      Enumeration<? extends ZipEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();
        if (entryName.endsWith(CLASS_SUFFIX)) {
          String className = StringUtil.trimEnd(entryName, CLASS_SUFFIX);
          ClassNode classNode = null;
          if (loadClasses) {
            classNode = getClassNodeFromInputStream(jarFile.getInputStream(entry));

            if (StringUtil.equal(className, findClass)) {
              result = classNode;
            }
          }
          myClassesCache.put(className, classNode == null ? null : new SoftReference<ClassNode>(classNode));
        }
      }
    } finally {
      IOUtils.closeQuietly(jarFile);
    }

    return result;
  }

  @NotNull
  private ClassNode getClassNodeFromInputStream(@NotNull InputStream is) throws IOException {
    ClassNode node = new ClassNode();
    new ClassReader(is).accept(node, 0);
    return node;
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myClassesCache.keySet());
  }

  @Override
  public String toString() {
    return myFile.getName();
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
      classFile = updateCacheAndFindClass(true, className);
    }
    return classFile;
  }

  @Override
  public Resolver getClassLocation(@NotNull String className) {
    if (myClassesCache.containsKey(className)) {
      return this;
    }
    return null;
  }
}
