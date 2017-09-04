package com.jetbrains.plugin.structure.classes.resolvers;

import com.google.common.collect.Iterators;
import com.jetbrains.plugin.structure.classes.utils.AsmUtil;
import com.jetbrains.plugin.structure.intellij.utils.StringUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Sergey Patrikeev
 */
public class CompileOutputResolver extends Resolver {

  private final Map<String, File> allClasses = new HashMap<String, File>();
  private final Set<File> classPath = new HashSet<File>();
  private final File myDir;

  public CompileOutputResolver(@NotNull File dir) throws IOException {
    myDir = dir.getCanonicalFile();
    Collection<File> classFiles = FileUtils.listFiles(myDir, new String[]{"class"}, true);
    for (File classFile : classFiles) {
      String className = AsmUtil.readClassName(classFile);
      File classRoot = getClassRoot(classFile, className);
      if (classRoot != null) {
        allClasses.put(className, classFile);
        classPath.add(classRoot);
      }
    }
  }

  @Nullable
  private File getClassRoot(@NotNull File classFile, @NotNull String className) {
    int levelsUp = StringUtil.countChars(className, '/');
    File root = classFile;
    for (int i = 0; i < levelsUp + 1; i++) {
      root = root != null ? root.getParentFile() : null;
    }
    return root;
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) throws IOException {
    if (!containsClass(className)) {
      return null;
    }
    return AsmUtil.readClassFromFile(allClasses.get(className));
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    if (!containsClass(className)) {
      return null;
    }
    return this;
  }

  @NotNull
  @Override
  public Iterator<String> getAllClasses() {
    return Iterators.unmodifiableIterator(allClasses.keySet().iterator());
  }

  @Override
  public boolean isEmpty() {
    return allClasses.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return allClasses.containsKey(className);
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return new ArrayList<File>(classPath);
  }

  @Override
  public void close() {
    //nothing to do
  }

  @Override
  public String toString() {
    return myDir.getPath();
  }
}
