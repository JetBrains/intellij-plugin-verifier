package com.jetbrains.plugin.structure.classes.resolvers;

import com.google.common.collect.Iterators;
import com.jetbrains.plugin.structure.classes.utils.AsmUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Sergey Patrikeev
 */
public final class FilesResolver extends Resolver {

  private final Map<String, File> myClass2File = new HashMap<String, File>();
  private final String myPresentableName;
  private final File myRoot;

  public FilesResolver(@NotNull String presentableName, @NotNull File root) throws IOException {
    myPresentableName = presentableName;
    myRoot = root;
    Collection<File> classFiles = FileUtils.listFiles(root, new String[]{"class"}, true);
    for (File classFile : classFiles) {
      if (classFile.getName().endsWith(".class")) {
        ClassNode node = AsmUtil.readClassFromFile(classFile);
        myClass2File.put(node.name, classFile);
      }
    }
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) throws IOException {
    if (!myClass2File.containsKey(className)) {
      return null;
    }
    return AsmUtil.readClassFromFile(myClass2File.get(className));
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return myClass2File.containsKey(className) ? this : null;
  }

  @NotNull
  @Override
  public Iterator<String> getAllClasses() {
    return Iterators.unmodifiableIterator(myClass2File.keySet().iterator());
  }

  @Override
  public boolean isEmpty() {
    return myClass2File.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myClass2File.containsKey(className);
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return Collections.singletonList(myRoot);
  }

  @Override
  public String toString() {
    return myPresentableName;
  }


  @Override
  public void close() {
  }
}
