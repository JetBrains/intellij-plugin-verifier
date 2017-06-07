package com.intellij.structure.impl.resolvers;

import com.google.common.collect.Iterators;
import com.intellij.structure.impl.utils.AsmUtil;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
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
class CompileOutputResolver extends Resolver {

  private final Map<String, File> allClasses = new HashMap<String, File>();
  private final Set<File> classPath = new HashSet<File>();
  private final File myDir;

  CompileOutputResolver(@NotNull File dir) throws IOException {
    myDir = dir;
    Collection<File> classFiles = FileUtils.listFiles(dir, new String[]{"class"}, true);
    for (File classFile : classFiles) {
      ClassNode classNode = AsmUtil.readClassFromFile(classFile);
      allClasses.put(classNode.name, classFile);
      classPath.add(getClassRoot(classFile.getCanonicalFile(), classNode));
    }
  }

  private File getClassRoot(File classFile, ClassNode classNode) throws IOException {
    int levelsUp = StringUtil.countChars(classNode.name, '/');
    String back = StringUtil.repeat("../", levelsUp + 1);
    return new File(classFile, back).getCanonicalFile();
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
  public void close() throws IOException {
    //nothing to do
  }

  @Override
  public String toString() {
    return myDir.getPath();
  }
}
