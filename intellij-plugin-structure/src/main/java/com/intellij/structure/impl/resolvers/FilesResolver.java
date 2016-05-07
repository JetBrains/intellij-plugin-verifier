package com.intellij.structure.impl.resolvers;

import com.google.common.io.Files;
import com.intellij.structure.impl.utils.AsmUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by Sergey Patrikeev
 */
public class FilesResolver extends Resolver {

  private final Map<String, File> myClass2File = new HashMap<String, File>();
  private final String myPresentableName;

  public FilesResolver(@NotNull String presentableName, @NotNull Collection<File> classFiles) throws IOException {
    myPresentableName = presentableName;
    for (File classFile : classFiles) {
      if (classFile.getName().endsWith(".class")) {
        ClassNode node = evaluateNode(classFile);
        myClass2File.put(node.name, classFile);
      }
    }
  }

  @NotNull
  private ClassNode evaluateNode(@NotNull File classFile) throws IOException {
    InputStream is = null;
    try {
      is = FileUtils.openInputStream(classFile);
      String className = Files.getNameWithoutExtension(classFile.getName());
      return AsmUtil.readClassNode(className, is);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) throws IOException {
    if (!myClass2File.containsKey(className)) {
      return null;
    }
    return evaluateNode(myClass2File.get(className));
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return myClass2File.containsKey(className) ? this : null;
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myClass2File.keySet());
  }

  @Override
  public boolean isEmpty() {
    return myClass2File.isEmpty();
  }

  @Override
  public String toString() {
    return myPresentableName;
  }


}
