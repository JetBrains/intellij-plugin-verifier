package com.intellij.structure.impl.resolvers;

import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Sergey Patrikeev
 */
public class ZipResolver extends Resolver {

  private final Map<String, SoftReference<ClassNode>> myClassesCache = new HashMap<String, SoftReference<ClassNode>>();
  private final String myPresentableName;
  private final String myZipUrl;

  public ZipResolver(@NotNull String presentableName, @NotNull String zipUrl) throws IOException {
    myPresentableName = presentableName;
    myZipUrl = zipUrl;
    updateCacheAndFindClass(null, true);
  }

  @Nullable
  private ClassNode updateCacheAndFindClass(@Nullable String findClass, boolean firstUpdate) throws IOException {
    ClassNode result = null;

    ZipInputStream zipInputStream = null;
    try {
      zipInputStream = URLUtil.openRecursiveJarStream(new URL(myZipUrl));

      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().endsWith(".class")) {
          ClassNode node = getClassNodeFromInputStream(zipInputStream);
          String className = node.name;
          if (StringUtil.equal(className, findClass)) {
            result = node;
          }
          myClassesCache.put(className, firstUpdate ? null : new SoftReference<ClassNode>(node));
        }
      }
    } finally {
      IOUtils.closeQuietly(zipInputStream);
    }

    //this is a strong reference to a ClassNode, so it's more reliable than myClassesCache.get(className).get()
    return result;
  }

  @NotNull
  private ClassNode getClassNodeFromInputStream(@NotNull InputStream is) throws IOException {
    ClassNode node = new ClassNode();
    new ClassReader(is).accept(node, 0);
    return node;
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) throws IOException {
    if (!myClassesCache.containsKey(className)) {
      return null;
    }
    SoftReference<ClassNode> reference = myClassesCache.get(className);
    ClassNode node = reference == null ? null : reference.get();
    if (node == null) {
      node = updateCacheAndFindClass(className, false);
    }
    return node;
  }

  @Override
  public String toString() {
    return myPresentableName;
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return myClassesCache.containsKey(className) ? this : null;
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myClassesCache.keySet());
  }

  @Override
  public boolean isEmpty() {
    return myClassesCache.isEmpty();
  }
}
