package com.intellij.structure.impl.resolvers;

import com.intellij.structure.impl.utils.AsmUtil;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

  private static final String CLASS_SUFFIX = ".class";
  private final Map<String, SoftReference<ClassNode>> myClassesCache = new HashMap<String, SoftReference<ClassNode>>();
  private final String myPresentableName;
  private final String myZipUrl;
  private final String myRootDirectoryPrefix;

  public ZipResolver(@NotNull String presentableName, @NotNull String zipUrl, @NotNull String rootDirectory) throws IOException {
    myPresentableName = presentableName;
    myZipUrl = zipUrl;
    if (rootDirectory.isEmpty() || rootDirectory.equals(".")) {
      myRootDirectoryPrefix = "";
    } else {
      myRootDirectoryPrefix = StringUtil.trimEnd(rootDirectory, "/") + "/";
    }
    updateCacheAndFindClass(null, false);
  }

  @Nullable
  private ClassNode updateCacheAndFindClass(@Nullable String findClass, boolean loadClasses) throws IOException {
    ClassNode result = null; //strong reference on result

    InputStream inputStream = null;

    try {
      inputStream = URLUtil.openRecursiveJarStream(new URL(myZipUrl));
      if (!(inputStream instanceof ZipInputStream)) {
        throw new IOException("Supplied input stream is not a stream for .zip of .jar archive");
      }
      ZipInputStream zipInputStream = null;

      try {
        zipInputStream = (ZipInputStream) inputStream;

        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
          if (entry.isDirectory()) continue;

          String entryName = entry.getName();
          if (entryName.startsWith(myRootDirectoryPrefix) && entryName.endsWith(CLASS_SUFFIX)) {
            String className = StringUtil.trimStart(StringUtil.trimEnd(entryName, CLASS_SUFFIX), myRootDirectoryPrefix);

            ClassNode classNode = null;
            if (loadClasses) {
              try {
                classNode = AsmUtil.readClassNode(className, zipInputStream);

                if (StringUtil.equal(className, findClass)) {
                  result = classNode;
                }

              } catch (IOException e) {
                if (StringUtil.equal(className, findClass)) {
                  //we are searching this class => throw exception
                  throw e;
                }
                //it might be some unrelated broken classfile => ignore it
              }
            }

            myClassesCache.put(className, classNode == null ? null : new SoftReference<ClassNode>(classNode));
          }
        }
      } finally {
        IOUtils.closeQuietly(zipInputStream);
      }

    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return result;
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
      node = updateCacheAndFindClass(className, true);
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
